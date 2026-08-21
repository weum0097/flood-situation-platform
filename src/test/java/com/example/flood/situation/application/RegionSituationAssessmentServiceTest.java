package com.example.flood.situation.application;

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
import com.example.flood.event.application.EventAssessmentImportPort;
import com.example.flood.event.application.ImportedAssessmentEvent;
import com.example.flood.event.domain.EventObservation;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.region.domain.RegionStatus;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.security.application.ApiPrincipal;
import com.example.flood.situation.domain.ComparisonDirection;
import com.example.flood.situation.domain.MetricCode;
import com.example.flood.situation.domain.SituationLevel;
import com.example.flood.situation.domain.SituationRuleDefinition;
import com.example.flood.situation.domain.SituationRuleEngine;
import com.example.flood.situation.infrastructure.SituationAssessmentMapper;
import com.example.flood.situation.infrastructure.SituationAssessmentRow;
import com.example.flood.situation.infrastructure.SituationAssessmentSummaryRow;
import com.example.flood.situation.infrastructure.SituationRuleMapper;
import com.example.flood.situation.infrastructure.SituationRuleRow;
import com.example.flood.situation.infrastructure.SituationRuleSetRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegionSituationAssessmentServiceTest {
    private static final Instant ASSESSMENT_TIME = Instant.parse("2026-08-21T04:00:00Z");
    private static final ApiPrincipal PRINCIPAL =
        new ApiPrincipal(7, 8, "client", Set.of("situation:calculate"));
    private static final ResolvedRegion REGION =
        new ResolvedRegion(11, "320111", "浦口区", 10L, "DISTRICT", RegionStatus.ACTIVE);

    private RegionResolver regionResolver;
    private EventAssessmentImportPort importer;
    private ActiveSituationRuleProvider ruleProvider;
    private SituationAssessmentMapper assessmentMapper;
    private PublicIdGenerator ids;
    private RegionSituationAssessmentService service;

    @BeforeEach
    void setUp() {
        regionResolver = mock(RegionResolver.class);
        importer = mock(EventAssessmentImportPort.class);
        ruleProvider = mock(ActiveSituationRuleProvider.class);
        assessmentMapper = mock(SituationAssessmentMapper.class);
        ids = mock(PublicIdGenerator.class);
        when(regionResolver.resolve(any())).thenReturn(REGION);
        when(ids.next("RSA_")).thenReturn("RSA_1");
        doAnswer(invocation -> {
            SituationAssessmentRow row = invocation.getArgument(0);
            row.setId(99L);
            return 1;
        }).when(assessmentMapper).insert(any());
        service = new RegionSituationAssessmentService(regionResolver, importer, ruleProvider,
            new SituationRuleEngine(), assessmentMapper, ids, new ObjectMapper().findAndRegisterModules(),
            Clock.fixed(ASSESSMENT_TIME.plusSeconds(1), ZoneOffset.UTC));
    }

    @Test
    void importsAggregatesEscalatesSnapshotsAndPersistsEvidence() {
        EventObservation first = observation("OBS_EXT_1", ASSESSMENT_TIME, 120, 10, 3, 2);
        EventObservation second = observation("OBS_EXT_2", ASSESSMENT_TIME, 130, 20, 4, 5);
        when(importer.importForAssessment(any(), any(), any(), any())).thenReturn(List.of(
            imported(1, 101, "EVT_1", "EXT_1", "OBS_1", first, true, false, true),
            imported(2, 102, "EVT_2", "EXT_2", "OBS_2", second, false, true, false)));
        when(ruleProvider.findActive(ASSESSMENT_TIME)).thenReturn(new ActiveSituationRuleSet(
            5, "flood-situation-v1.0", 2, List.of(new SituationRuleDefinition(
                "AFFECTED_MEDIUM", null, MetricCode.AFFECTED_POPULATION,
                ComparisonDirection.GTE, new BigDecimal("100"), new BigDecimal("1000"), 10, true))));

        var response = service.assess(new SituationAssessmentCommand(
            new RegionSelector("320111", null), ASSESSMENT_TIME, List.of()), PRINCIPAL);

        assertThat(response.situationLevel()).isEqualTo(SituationLevel.HIGH);
        assertThat(response.activeEventCount()).isEqualTo(2);
        assertThat(response.aggregateImpact().affectedPopulation()).isEqualTo(250);
        assertThat(response.aggregateImpact().trappedPopulation()).isEqualTo(30);
        assertThat(response.importSummary().createdEvents()).isEqualTo(1);
        assertThat(response.importSummary().updatedEvents()).isEqualTo(1);
        assertThat(response.importSummary().createdObservations()).isEqualTo(1);
        assertThat(response.importSummary().duplicatedObservations()).isEqualTo(1);
        assertThat(response.warnings()).extracting(warning -> warning.code())
            .containsExactly("POSSIBLE_POPULATION_OVERLAP");
        assertThat(response.eventResults()).hasSize(2)
            .allSatisfy(result -> assertThat(result.durationHours()).isEqualByComparingTo("4.000"));

        ArgumentCaptor<SituationAssessmentRow> header =
            ArgumentCaptor.forClass(SituationAssessmentRow.class);
        verify(assessmentMapper).insert(header.capture());
        assertThat(header.getValue().getSituationLevel()).isEqualTo("HIGH");
        assertThat(header.getValue().getSourceType()).isEqualTo("DIRECT_IMPORT");
        assertThat(header.getValue().getRuleVersion()).isEqualTo("flood-situation-v1.0");
        assertThat(header.getValue().getInputSnapshot()).contains("EXT_1", "AFFECTED_MEDIUM");
        verify(assessmentMapper, org.mockito.Mockito.times(2)).insertEvent(any());
    }

    @Test
    void excludesEndedEventsAndObservationsAfterAssessmentTime() {
        EventObservation now = observation("ONE", ASSESSMENT_TIME, 100, 0, 0, 0);
        EventObservation future = observation("TWO", ASSESSMENT_TIME.plusSeconds(1), 1000, 0, 0, 0);
        when(importer.importForAssessment(any(), any(), any(), any())).thenReturn(List.of(
            imported(1, 101, "EVT_1", "EXT_1", "OBS_1", now, false, true, true)
                .withStatus(EventStatus.ENDED),
            imported(2, 102, "EVT_2", "EXT_2", "OBS_2", future, true, false, true)));
        when(ruleProvider.findActive(any())).thenReturn(new ActiveSituationRuleSet(
            5, "v1", null, List.of()));

        var response = service.assess(new SituationAssessmentCommand(
            new RegionSelector("320111", null), ASSESSMENT_TIME, List.of()), PRINCIPAL);

        assertThat(response.activeEventCount()).isZero();
        assertThat(response.eventResults()).isEmpty();
        assertThat(response.situationLevel()).isEqualTo(SituationLevel.LOW);
    }

    @Test
    void activeRuleProviderUsesHalfOpenEffectiveTimeAndRejectsMissingOrMultipleSets() {
        SituationRuleMapper mapper = mock(SituationRuleMapper.class);
        ActiveSituationRuleProvider provider = new ActiveSituationRuleProvider(mapper);
        SituationRuleSetRow set = new SituationRuleSetRow(4, "v4", "ACTIVE",
            "HIGHEST_EVENT_LEVEL", null, ASSESSMENT_TIME, null);
        when(mapper.findActiveRuleSets(ASSESSMENT_TIME)).thenReturn(List.of(set));
        when(mapper.findRules(4)).thenReturn(List.of(new SituationRuleRow(1, 4, "R1", null,
            "AFFECTED_POPULATION", "GTE", BigDecimal.ONE, BigDecimal.TEN, 1, true)));

        assertThat(provider.findActive(ASSESSMENT_TIME).version()).isEqualTo("v4");
        verify(mapper).findActiveRuleSets(ASSESSMENT_TIME);

        when(mapper.findActiveRuleSets(ASSESSMENT_TIME)).thenReturn(List.of());
        assertThatThrownBy(() -> provider.findActive(ASSESSMENT_TIME))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.NO_ACTIVE_RULE_SET));

        when(mapper.findActiveRuleSets(ASSESSMENT_TIME)).thenReturn(List.of(set, set));
        assertThatThrownBy(() -> provider.findActive(ASSESSMENT_TIME))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void historicalSearchReturnsSavedRowsInMapperOrder() {
        SituationAssessmentQuery query = new SituationAssessmentQuery(
            new RegionSelector("320111", null), ASSESSMENT_TIME.minusSeconds(3600),
            ASSESSMENT_TIME.plusSeconds(3600), SituationLevel.HIGH, 0, 20);
        when(assessmentMapper.countForQuery(11, query)).thenReturn(2L);
        when(assessmentMapper.findPageForQuery(11, query)).thenReturn(List.of(
            summary(2, "RSA_2", ASSESSMENT_TIME),
            summary(1, "RSA_1", ASSESSMENT_TIME.minusSeconds(1))));

        var page = service.search(query);

        assertThat(page.items()).extracting(item -> item.assessmentId())
            .containsExactly("RSA_2", "RSA_1");
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    private static ImportedAssessmentEvent imported(long eventPk, long observationPk,
        String eventId, String externalId, String observationId, EventObservation observation,
        boolean created, boolean updated, boolean observationCreated) {
        return new ImportedAssessmentEvent(eventPk, observationPk, eventId, externalId,
            observationId, EventType.RIVER_FLOOD, EventStatus.ONGOING,
            ASSESSMENT_TIME.minusSeconds(4 * 3600), null, observation,
            created, updated, observationCreated);
    }

    private static EventObservation observation(String externalId, Instant observedAt,
        long affected, long trapped, long evacuated, long vulnerable) {
        return new EventObservation(externalId, observedAt, null, null, null, null,
            affected, trapped, evacuated, vulnerable, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static SituationAssessmentSummaryRow summary(long id, String publicId, Instant time) {
        return new SituationAssessmentSummaryRow(id, publicId, "320111", "浦口区", time,
            "HIGH", 1, 10, 2, 3, 1, "[\"EVT_1\"]", "v1", "[]");
    }
}
