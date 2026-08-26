package com.example.flood.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PublicIdGenerator;
import com.example.flood.event.api.ObservationResponse;
import com.example.flood.event.domain.EventObservation;
import com.example.flood.event.domain.EventValidator;
import com.example.flood.event.infrastructure.DisasterEventMapper;
import com.example.flood.event.infrastructure.DisasterEventRow;
import com.example.flood.event.infrastructure.EventObservationMapper;
import com.example.flood.event.infrastructure.EventObservationRow;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.security.application.ApiPrincipal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObservationConflictTest {
    private DisasterEventMapper eventMapper;
    private EventObservationMapper observationMapper;
    private DefaultEventApplicationService service;
    private final ApiPrincipal principal = new ApiPrincipal(1, 2, "client", Set.of());
    private final Instant start = Instant.parse("2026-08-21T00:00:00Z");

    @BeforeEach void setUp() {
        eventMapper = mock(DisasterEventMapper.class);
        observationMapper = mock(EventObservationMapper.class);
        Clock clock = Clock.fixed(start.plusSeconds(100), ZoneOffset.UTC);
        service = new DefaultEventApplicationService(eventMapper, observationMapper,
            mock(RegionResolver.class), new EventValidator(), new PublicIdGenerator(clock), clock);
    }

    @Test void missingEventIsReported() {
        when(eventMapper.findByPublicIdForUpdate("EVT_missing")).thenReturn(Optional.empty());
        assertCode(() -> service.appendObservation("EVT_missing",
            new AppendObservationCommand(observation(start.plusSeconds(1), "external", 10)), principal),
            ErrorCode.EVENT_NOT_FOUND);
    }

    @Test void identicalNaturalKeyReturnsExistingObservation() {
        EventObservation observation = observation(start.plusSeconds(1), "external", 10);
        stubEvent();
        when(observationMapper.findByNaturalKey(10, "external", observation.observedAt()))
            .thenReturn(Optional.of(new EventObservationRow(30, 10, "OBS_existing", observation)));

        ObservationResponse result = service.appendObservation("EVT_1",
            new AppendObservationCommand(observation), principal);

        assertThat(result.observationId()).isEqualTo("OBS_existing");
    }

    @Test void changedBodyWithSameNaturalKeyIsConflict() {
        EventObservation persisted = observation(start.plusSeconds(1), "external", 10);
        EventObservation changed = observation(start.plusSeconds(1), "external", 11);
        stubEvent();
        when(observationMapper.findByNaturalKey(10, "external", changed.observedAt()))
            .thenReturn(Optional.of(new EventObservationRow(30, 10, "OBS_existing", persisted)));

        assertCode(() -> service.appendObservation("EVT_1",
            new AppendObservationCommand(changed), principal), ErrorCode.OBSERVATION_CONFLICT);
    }

    @Test void observationBeforeEventStartIsRejected() {
        stubEvent();
        assertCode(() -> service.appendObservation("EVT_1",
            new AppendObservationCommand(observation(start.minusSeconds(1), "external", 10)), principal),
            ErrorCode.VALIDATION_ERROR);
    }

    private void stubEvent() {
        when(eventMapper.findByPublicIdForUpdate("EVT_1")).thenReturn(Optional.of(
            new DisasterEventRow(10, "EVT_1", "ext", "source", 3, "320111", "浦口区",
                "RIVER_FLOOD", "Flood", start, null, "ONGOING", 1, start, start)));
    }
    private EventObservation observation(Instant time, String externalId, long affected) {
        return new EventObservation(externalId, time, BigDecimal.ONE, null, BigDecimal.ONE, null,
            affected, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    private static void assertCode(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(code));
    }
}
