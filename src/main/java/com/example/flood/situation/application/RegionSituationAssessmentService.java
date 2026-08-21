package com.example.flood.situation.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PageResponse;
import com.example.flood.common.api.PublicIdGenerator;
import com.example.flood.event.application.EventAssessmentImportPort;
import com.example.flood.event.application.ImportedAssessmentEvent;
import com.example.flood.event.domain.EventObservation;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.security.application.ApiPrincipal;
import com.example.flood.situation.api.SituationAssessmentResponse;
import com.example.flood.situation.api.SituationAssessmentSummaryResponse;
import com.example.flood.situation.domain.EventSituationInput;
import com.example.flood.situation.domain.EventSituationResult;
import com.example.flood.situation.domain.MetricCode;
import com.example.flood.situation.domain.RegionSituationResult;
import com.example.flood.situation.domain.SituationLevel;
import com.example.flood.situation.domain.SituationRuleEngine;
import com.example.flood.situation.infrastructure.SituationAssessmentEventRow;
import com.example.flood.situation.infrastructure.SituationAssessmentMapper;
import com.example.flood.situation.infrastructure.SituationAssessmentRow;
import com.example.flood.situation.infrastructure.SituationAssessmentSummaryRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class RegionSituationAssessmentService {
    private static final SituationAssessmentResponse.Warning POPULATION_WARNING =
        new SituationAssessmentResponse.Warning("POSSIBLE_POPULATION_OVERLAP",
            "MVP 未使用 GIS，跨事件人口可能存在重复统计");

    private final RegionResolver regionResolver;
    private final EventAssessmentImportPort importer;
    private final ActiveSituationRuleProvider ruleProvider;
    private final SituationRuleEngine engine;
    private final SituationAssessmentMapper mapper;
    private final PublicIdGenerator ids;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RegionSituationAssessmentService(RegionResolver regionResolver,
        EventAssessmentImportPort importer, ActiveSituationRuleProvider ruleProvider,
        SituationRuleEngine engine, SituationAssessmentMapper mapper, PublicIdGenerator ids,
        ObjectMapper objectMapper, Clock clock) {
        this.regionResolver = regionResolver;
        this.importer = importer;
        this.ruleProvider = ruleProvider;
        this.engine = engine;
        this.mapper = mapper;
        this.ids = ids;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public SituationAssessmentResponse assess(SituationAssessmentCommand command,
        ApiPrincipal principal) {
        if (command.assessmentTime() == null || command.events() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "assessmentTime and events are required");
        }
        ResolvedRegion region = regionResolver.resolve(command.region());
        List<ImportedAssessmentEvent> imported = importer.importForAssessment(region,
            command.assessmentTime(), command.events(), principal);
        ActiveSituationRuleSet ruleSet = ruleProvider.findActive(command.assessmentTime());
        List<EvaluatedEvent> evaluated = imported.stream()
            .filter(event -> event.status() == EventStatus.ONGOING)
            .filter(event -> !event.observation().observedAt().isAfter(command.assessmentTime()))
            .filter(event -> !event.startTime().isAfter(command.assessmentTime()))
            .map(event -> new EvaluatedEvent(event,
                engine.evaluateEvent(toInput(event), ruleSet.rules()),
                durationHours(event.startTime(), command.assessmentTime())))
            .toList();
        RegionSituationResult regionResult = engine.aggregate(
            evaluated.stream().map(EvaluatedEvent::result).toList(),
            ruleSet.mediumToHighCount());
        SituationAssessmentResponse.AggregateImpact impact = aggregate(evaluated);
        List<SituationAssessmentResponse.Warning> warnings = evaluated.size() > 1
            && hasPopulation(impact) ? List.of(POPULATION_WARNING) : List.of();
        String assessmentId = ids.next("RSA_");
        OffsetDateTime createdAt = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        SituationAssessmentRow row = new SituationAssessmentRow(assessmentId, region.id(),
            command.assessmentTime(), regionResult.level().name(), evaluated.size(),
            impact.affectedPopulation(), impact.trappedPopulation(), impact.evacuatedPopulation(),
            impact.vulnerablePopulation(), ruleSet.id(), ruleSet.version(), "DIRECT_IMPORT",
            json(new AssessmentSnapshot(region, command.assessmentTime(), imported, ruleSet)),
            json(warnings), principal.clientId());
        mapper.insert(row);
        List<SituationAssessmentResponse.EventResult> eventResults = new ArrayList<>();
        for (EvaluatedEvent item : evaluated) {
            mapper.insertEvent(new SituationAssessmentEventRow(row.getId(),
                item.event().eventDatabaseId(), item.event().observationDatabaseId(),
                item.result().level().name(), item.durationHours(),
                json(item.result().matchedRuleCodes())));
            eventResults.add(new SituationAssessmentResponse.EventResult(item.event().eventId(),
                item.event().externalEventId(), item.event().observationId(), item.result().level(),
                item.durationHours(), item.result().matchedRuleCodes()));
        }
        return new SituationAssessmentResponse(assessmentId,
            new RegionSelector(region.regionCode(), region.regionName()),
            utc(command.assessmentTime()), regionResult.level(), evaluated.size(), impact,
            eventResults, importSummary(imported), ruleSet.version(), warnings, createdAt);
    }

    @Transactional(readOnly = true)
    public PageResponse<SituationAssessmentSummaryResponse> search(SituationAssessmentQuery query) {
        ResolvedRegion region = regionResolver.resolve(query.region());
        long total = mapper.countForQuery(region.id(), query);
        List<SituationAssessmentSummaryResponse> items = mapper.findPageForQuery(region.id(), query)
            .stream().map(this::toSummary).toList();
        int totalPages = total == 0 ? 0 : (int) ((total + query.size() - 1) / query.size());
        return new PageResponse<>(items, query.page(), query.size(), total, totalPages);
    }

    private SituationAssessmentSummaryResponse toSummary(SituationAssessmentSummaryRow row) {
        return new SituationAssessmentSummaryResponse(row.publicId(),
            new RegionSelector(row.regionCode(), row.regionName()), utc(row.assessmentTime()),
            SituationLevel.valueOf(row.situationLevel()), row.activeEventCount(),
            new SituationAssessmentResponse.AggregateImpact(row.affectedPopulation(),
                row.trappedPopulation(), row.evacuatedPopulation(), row.vulnerablePopulation()),
            readList(row.eventIds(), new TypeReference<List<String>>() {}), row.ruleVersion(),
            row.warnings() == null ? List.of() : readList(row.warnings(),
                new TypeReference<List<SituationAssessmentResponse.Warning>>() {}));
    }

    private EventSituationInput toInput(ImportedAssessmentEvent event) {
        EventObservation observation = event.observation();
        Map<MetricCode, BigDecimal> metrics = new EnumMap<>(MetricCode.class);
        put(metrics, MetricCode.RAINFALL_24H_MM, observation.rainfall24hMm());
        put(metrics, MetricCode.WATER_LEVEL_OVER_WARNING_M, observation.waterLevelOverWarningM());
        put(metrics, MetricCode.MAX_WATER_DEPTH_M, observation.maxWaterDepthM());
        put(metrics, MetricCode.AFFECTED_AREA_KM2, observation.affectedAreaKm2());
        metrics.put(MetricCode.AFFECTED_POPULATION, BigDecimal.valueOf(observation.affectedPopulation()));
        metrics.put(MetricCode.TRAPPED_POPULATION, BigDecimal.valueOf(observation.trappedPopulation()));
        metrics.put(MetricCode.EVACUATED_POPULATION, BigDecimal.valueOf(observation.evacuatedPopulation()));
        metrics.put(MetricCode.VULNERABLE_POPULATION, BigDecimal.valueOf(observation.vulnerablePopulation()));
        metrics.put(MetricCode.INJURED_POPULATION, BigDecimal.valueOf(observation.injuredPopulation()));
        metrics.put(MetricCode.MISSING_POPULATION, BigDecimal.valueOf(observation.missingPopulation()));
        metrics.put(MetricCode.DEATH_POPULATION, BigDecimal.valueOf(observation.deathPopulation()));
        metrics.put(MetricCode.DAMAGED_HOUSEHOLDS, BigDecimal.valueOf(observation.damagedHouseholds()));
        metrics.put(MetricCode.COLLAPSED_HOUSES, BigDecimal.valueOf(observation.collapsedHouses()));
        metrics.put(MetricCode.ROAD_INTERRUPTIONS, BigDecimal.valueOf(observation.roadInterruptions()));
        metrics.put(MetricCode.CRITICAL_FACILITIES_AFFECTED,
            BigDecimal.valueOf(observation.criticalFacilitiesAffected()));
        metrics.put(MetricCode.POWER_OUTAGE_HOUSEHOLDS,
            BigDecimal.valueOf(observation.powerOutageHouseholds()));
        return new EventSituationInput(event.eventType(), metrics);
    }

    private static SituationAssessmentResponse.AggregateImpact aggregate(
        List<EvaluatedEvent> events) {
        long affected = 0, trapped = 0, evacuated = 0, vulnerable = 0;
        for (EvaluatedEvent item : events) {
            EventObservation observation = item.event().observation();
            affected = Math.addExact(affected, observation.affectedPopulation());
            trapped = Math.addExact(trapped, observation.trappedPopulation());
            evacuated = Math.addExact(evacuated, observation.evacuatedPopulation());
            vulnerable = Math.addExact(vulnerable, observation.vulnerablePopulation());
        }
        return new SituationAssessmentResponse.AggregateImpact(
            affected, trapped, evacuated, vulnerable);
    }

    private static SituationAssessmentResponse.ImportSummary importSummary(
        List<ImportedAssessmentEvent> imported) {
        return new SituationAssessmentResponse.ImportSummary(
            (int) imported.stream().filter(ImportedAssessmentEvent::eventCreated).count(),
            (int) imported.stream().filter(ImportedAssessmentEvent::eventUpdated).count(),
            (int) imported.stream().filter(ImportedAssessmentEvent::observationCreated).count(),
            (int) imported.stream().filter(event -> !event.observationCreated()).count());
    }

    private static boolean hasPopulation(SituationAssessmentResponse.AggregateImpact impact) {
        return impact.affectedPopulation() > 0 || impact.trappedPopulation() > 0
            || impact.evacuatedPopulation() > 0 || impact.vulnerablePopulation() > 0;
    }

    private static BigDecimal durationHours(Instant start, Instant end) {
        return BigDecimal.valueOf(Duration.between(start, end).toMillis())
            .divide(BigDecimal.valueOf(3_600_000), 3, RoundingMode.HALF_UP);
    }

    private static void put(Map<MetricCode, BigDecimal> metrics, MetricCode code,
        BigDecimal value) {
        if (value != null) metrics.put(code, value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not snapshot assessment input");
        }
    }

    private <T> T readList(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Stored assessment JSON is unreadable");
        }
    }

    private static OffsetDateTime utc(Instant value) { return value.atOffset(ZoneOffset.UTC); }

    private record EvaluatedEvent(ImportedAssessmentEvent event, EventSituationResult result,
        BigDecimal durationHours) {}
    private record AssessmentSnapshot(ResolvedRegion region, Instant assessmentTime,
        List<ImportedAssessmentEvent> events, ActiveSituationRuleSet ruleSet) {}
}
