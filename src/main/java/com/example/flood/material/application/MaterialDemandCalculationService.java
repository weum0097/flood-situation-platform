package com.example.flood.material.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PublicIdGenerator;
import com.example.flood.common.time.BeijingTime;
import com.example.flood.material.api.MaterialCalculationResponse;
import com.example.flood.material.api.MaterialDemandItemResponse;
import com.example.flood.material.domain.MaterialDemandCalculator;
import com.example.flood.material.domain.MaterialDemandInput;
import com.example.flood.material.domain.MaterialDemandLine;
import com.example.flood.material.domain.MaterialStandardDefinition;
import com.example.flood.material.domain.PopulationSnapshot;
import com.example.flood.material.infrastructure.MaterialCalculationMapper;
import com.example.flood.material.infrastructure.MaterialCalculationRow;
import com.example.flood.material.infrastructure.MaterialDemandItemRow;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.security.application.ApiPrincipal;
import com.example.flood.situation.application.SavedSituationSnapshot;
import com.example.flood.situation.application.SituationSnapshotLookup;
import com.example.flood.situation.domain.SituationLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class MaterialDemandCalculationService {
    private final RegionResolver regionResolver;
    private final MaterialStandardService standardService;
    private final MaterialDemandCalculator calculator;
    private final SituationSnapshotLookup snapshotLookup;
    private final MaterialCalculationMapper mapper;
    private final PublicIdGenerator ids;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MaterialDemandCalculationService(RegionResolver regionResolver,
        MaterialStandardService standardService, MaterialDemandCalculator calculator,
        SituationSnapshotLookup snapshotLookup, MaterialCalculationMapper mapper,
        PublicIdGenerator ids, ObjectMapper objectMapper, Clock clock) {
        this.regionResolver = regionResolver; this.standardService = standardService;
        this.calculator = calculator; this.snapshotLookup = snapshotLookup; this.mapper = mapper;
        this.ids = ids; this.objectMapper = objectMapper; this.clock = clock;
    }

    @Transactional
    public MaterialCalculationResponse calculateDirect(DirectMaterialCalculationCommand command,
        ApiPrincipal principal) {
        ResolvedRegion region = regionResolver.resolve(command.region());
        return calculate(region, "DIRECT", null, null, command.situationLevel(),
            command.supplyDurationHours(), command.reserveRatio(), command.population(),
            command.currentInventory(), command, principal);
    }

    @Transactional
    public MaterialCalculationResponse calculateFromRegionData(
        RegionDataMaterialCalculationCommand command, ApiPrincipal principal) {
        if (command.startTime() == null || command.endTime() == null
            || !command.startTime().isBefore(command.endTime())) {
            throw new ApiException(ErrorCode.INVALID_TIME_RANGE,
                "assessmentTimeRange.startTime must be earlier than endTime");
        }
        ResolvedRegion region = regionResolver.resolve(command.region());
        SavedSituationSnapshot snapshot = snapshotLookup.findLatest(region.id(),
            command.startTime(), command.endTime()).orElseThrow(() -> new ApiException(
                ErrorCode.NO_SITUATION_DATA, "No saved situation assessment exists in the range"));
        return calculate(region, "DATABASE", snapshot.databaseId(), snapshot.publicId(),
            snapshot.level(), command.supplyDurationHours(), null, new PopulationSnapshot(
                snapshot.affectedPopulation(), snapshot.trappedPopulation(),
                snapshot.evacuatedPopulation(), snapshot.vulnerablePopulation()),
            command.currentInventory(), new DatabaseInputSnapshot(command, snapshot), principal);
    }

    private MaterialCalculationResponse calculate(ResolvedRegion region, String sourceType,
        Long assessmentDatabaseId, String assessmentPublicId, SituationLevel level,
        BigDecimal duration, BigDecimal reserveRatio, PopulationSnapshot population,
        List<com.example.flood.material.domain.InventoryItem> inventory, Object requestSnapshot,
        ApiPrincipal principal) {
        Instant calculationTime = clock.instant();
        SelectedMaterialStandards selected = standardService.select(region, level, calculationTime);
        MaterialDemandInput input = new MaterialDemandInput(duration, level, population,
            reserveRatio, inventory);
        List<MaterialDemandLine> lines = calculator.calculate(input, selected.definitions());
        int supplyDays = calculator.supplyDays(duration);
        String calculationId = ids.next("MDC_");
        BigDecimal storedDuration = duration.setScale(3, RoundingMode.HALF_UP);
        List<String> warnings = List.of();
        MaterialCalculationRow header = new MaterialCalculationRow(calculationId, region.id(),
            assessmentDatabaseId, sourceType, level.name(), storedDuration, supplyDays,
            population.affected(), population.trapped(), population.evacuated(),
            population.vulnerable(), selected.standardSetId(), selected.version(),
            json(new CalculationInputSnapshot(requestSnapshot, selected)), json(warnings),
            principal.clientId());
        mapper.insert(header);
        for (MaterialDemandLine line : lines) {
            MaterialStandardDefinition definition = selected.definitions().stream()
                .filter(candidate -> candidate.materialCode().equals(line.materialCode()))
                .findFirst().orElseThrow();
            mapper.insertItem(new MaterialDemandItemRow(header.getId(), line.materialCode(),
                line.materialName(), line.unit(), line.populationBasis().name(),
                line.basisPopulation(), line.grossDemand(), line.currentInventory(),
                line.netDemand(), json(new FormulaSnapshot(definition, reserveRatio, supplyDays))));
        }
        OffsetDateTime createdAt = BeijingTime.from(calculationTime);
        return new MaterialCalculationResponse(calculationId, sourceType, assessmentPublicId,
            new RegionSelector(region.regionCode(), region.regionName()), level, storedDuration,
            supplyDays, lines.stream().map(MaterialDemandCalculationService::responseItem).toList(),
            selected.version(), warnings, createdAt);
    }

    private static MaterialDemandItemResponse responseItem(MaterialDemandLine line) {
        return new MaterialDemandItemResponse(line.materialCode(), line.materialName(),
            line.populationBasis(), line.basisPopulation(), line.grossDemand(),
            line.currentInventory(), line.netDemand(), line.unit());
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not snapshot material calculation");
        }
    }

    private record CalculationInputSnapshot(Object request, SelectedMaterialStandards standards) {}
    private record DatabaseInputSnapshot(RegionDataMaterialCalculationCommand request,
        SavedSituationSnapshot assessment) {}
    private record FormulaSnapshot(MaterialStandardDefinition standard,
        BigDecimal reserveRatioOverride, int supplyDays) {}
}
