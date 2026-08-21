package com.example.flood.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PublicIdGenerator;
import com.example.flood.event.domain.EventObservation;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.event.domain.EventValidator;
import com.example.flood.event.infrastructure.DisasterEventMapper;
import com.example.flood.event.infrastructure.DisasterEventRow;
import com.example.flood.event.infrastructure.EventObservationMapper;
import com.example.flood.event.infrastructure.EventObservationRow;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.domain.RegionStatus;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.security.application.ApiPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssessmentEventImportTest {
    private static final Instant START = Instant.parse("2026-08-21T00:00:00Z");
    private static final ResolvedRegion REGION =
        new ResolvedRegion(3, "320111", "浦口区", 2L, "DISTRICT", RegionStatus.ACTIVE);
    private static final ApiPrincipal PRINCIPAL = new ApiPrincipal(1, 2, "client", Set.of());
    private DisasterEventMapper eventMapper;
    private EventObservationMapper observationMapper;
    private DefaultEventApplicationService service;

    @BeforeEach
    void setUp() {
        eventMapper = mock(DisasterEventMapper.class);
        observationMapper = mock(EventObservationMapper.class);
        Clock clock = Clock.fixed(START.plusSeconds(3600), ZoneOffset.UTC);
        service = new DefaultEventApplicationService(eventMapper, observationMapper,
            mock(RegionResolver.class), new EventValidator(), new PublicIdGenerator(clock), clock);
    }

    @Test
    void rejectsExistingEventWithDifferentStableRegion() {
        when(eventMapper.findByExternalForUpdate("source", "external"))
            .thenReturn(Optional.of(row(9, "Flood", null, "ONGOING")));

        assertThatThrownBy(() -> service.importForAssessment(
            new ResolvedRegion(4, "OTHER", "Other", null, "DISTRICT", RegionStatus.ACTIVE),
            START.plusSeconds(3600), List.of(command("Flood", null, EventStatus.ONGOING)), PRINCIPAL))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.EVENT_CONFLICT));
    }

    @Test
    void updatesOnlyMutableFieldsAndCountsDuplicateObservation() {
        DisasterEventRow existing = row(3, "Old", null, "ONGOING");
        EventObservation observation = observation();
        when(eventMapper.findByExternalForUpdate("source", "external"))
            .thenReturn(Optional.of(existing));
        when(observationMapper.findByNaturalKey(10, "observation", observation.observedAt()))
            .thenReturn(Optional.of(new EventObservationRow(20, 10, "OBS_1", observation)));

        ImportedAssessmentEvent result = service.importForAssessment(REGION,
            START.plusSeconds(3600), List.of(command("Renamed", START.plusSeconds(3600),
                EventStatus.ENDED)), PRINCIPAL).getFirst();

        assertThat(result.eventCreated()).isFalse();
        assertThat(result.eventUpdated()).isTrue();
        assertThat(result.observationCreated()).isFalse();
        verify(eventMapper).updateAssessmentMutable(10, "Renamed",
            START.plusSeconds(3600), "ENDED");
    }

    private static AssessmentEventImportCommand command(String name, Instant end,
        EventStatus status) {
        return new AssessmentEventImportCommand("external", "source", EventType.RIVER_FLOOD,
            name, START, end, status, observation());
    }

    private static DisasterEventRow row(long regionId, String name, Instant end, String status) {
        return new DisasterEventRow(10, "EVT_1", "external", "source", regionId,
            "320111", "浦口区", "RIVER_FLOOD", name, START, end, status, 1, START, START);
    }

    private static EventObservation observation() {
        return new EventObservation("observation", START.plusSeconds(1800), null, null, null,
            null, 10, 1, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
