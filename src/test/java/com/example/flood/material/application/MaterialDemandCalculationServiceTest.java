package com.example.flood.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PublicIdGenerator;
import com.example.flood.material.domain.InventoryItem;
import com.example.flood.material.domain.MaterialDemandCalculator;
import com.example.flood.material.domain.MaterialStandardDefinition;
import com.example.flood.material.domain.PopulationBasis;
import com.example.flood.material.domain.PopulationSnapshot;
import com.example.flood.material.infrastructure.MaterialCalculationMapper;
import com.example.flood.material.infrastructure.MaterialCalculationRow;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.region.domain.RegionStatus;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.security.application.ApiPrincipal;
import com.example.flood.situation.application.SavedSituationSnapshot;
import com.example.flood.situation.application.SituationSnapshotLookup;
import com.example.flood.situation.domain.SituationLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MaterialDemandCalculationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-21T04:00:00Z");
    private static final ResolvedRegion REGION =
        new ResolvedRegion(11, "320111", "浦口区", 10L, "DISTRICT", RegionStatus.ACTIVE);
    private static final ApiPrincipal PRINCIPAL = new ApiPrincipal(7, 8, "client", Set.of());
    private RegionResolver regionResolver;
    private MaterialStandardService standards;
    private SituationSnapshotLookup snapshots;
    private MaterialCalculationMapper mapper;
    private MaterialDemandCalculationService service;

    @BeforeEach
    void setUp() {
        regionResolver = mock(RegionResolver.class);
        standards = mock(MaterialStandardService.class);
        snapshots = mock(SituationSnapshotLookup.class);
        mapper = mock(MaterialCalculationMapper.class);
        PublicIdGenerator ids = mock(PublicIdGenerator.class);
        when(regionResolver.resolve(any())).thenReturn(REGION);
        when(ids.next("MDC_")).thenReturn("MDC_1");
        when(standards.select(any(), any(), any())).thenReturn(new SelectedMaterialStandards(
            5, "material-v1", List.of(standard())));
        doAnswer(invocation -> {
            ((MaterialCalculationRow) invocation.getArgument(0)).setId(90);
            return 1;
        }).when(mapper).insert(any());
        service = new MaterialDemandCalculationService(regionResolver, standards,
            new MaterialDemandCalculator(), snapshots, mapper, ids,
            new ObjectMapper().findAndRegisterModules(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void directCalculationPersistsHeaderDetailsAndStandardSnapshot() {
        var response = service.calculateDirect(new DirectMaterialCalculationCommand(
            new RegionSelector("320111", null), SituationLevel.HIGH, new BigDecimal("25"),
            new BigDecimal("0.1"), new PopulationSnapshot(100, 10, 20, 5),
            List.of(new InventoryItem("WATER", "箱", new BigDecimal("5")))), PRINCIPAL);

        assertThat(response.sourceType()).isEqualTo("DIRECT");
        assertThat(response.assessmentId()).isNull();
        assertThat(response.supplyDays()).isEqualTo(2);
        assertThat(response.standardVersion()).isEqualTo("material-v1");
        assertThat(response.items()).hasSize(1);
        ArgumentCaptor<MaterialCalculationRow> header =
            ArgumentCaptor.forClass(MaterialCalculationRow.class);
        verify(mapper).insert(header.capture());
        assertThat(header.getValue().getSourceType()).isEqualTo("DIRECT");
        assertThat(header.getValue().getAssessmentId()).isNull();
        assertThat(header.getValue().getInputSnapshot()).contains("material-v1", "WATER");
        verify(mapper).insertItem(any());
    }

    @Test
    void databaseCalculationUsesLatestSavedAssessmentAndReportsMissingData() {
        Instant start = NOW.minusSeconds(3600);
        Instant end = NOW.plusSeconds(1);
        when(snapshots.findLatest(11, start, end)).thenReturn(Optional.of(
            new SavedSituationSnapshot(44, "RSA_01", SituationLevel.HIGH,
                100, 10, 20, 5, NOW.minusSeconds(10))));

        var response = service.calculateFromRegionData(new RegionDataMaterialCalculationCommand(
            new RegionSelector("320111", null), start, end, new BigDecimal("24"), List.of()), PRINCIPAL);

        assertThat(response.sourceType()).isEqualTo("DATABASE");
        assertThat(response.assessmentId()).isEqualTo("RSA_01");
        ArgumentCaptor<MaterialCalculationRow> header =
            ArgumentCaptor.forClass(MaterialCalculationRow.class);
        verify(mapper).insert(header.capture());
        assertThat(header.getValue().getAssessmentId()).isEqualTo(44);

        when(snapshots.findLatest(11, start, end)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.calculateFromRegionData(
            new RegionDataMaterialCalculationCommand(new RegionSelector("320111", null),
                start, end, BigDecimal.ONE, List.of()), PRINCIPAL))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.NO_SITUATION_DATA));
    }

    private static MaterialStandardDefinition standard() {
        return new MaterialStandardDefinition(1, 0, "WATER", "饮用水", "箱",
            PopulationBasis.AFFECTED, new BigDecimal("1"), null, BigDecimal.ONE,
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO);
    }
}
