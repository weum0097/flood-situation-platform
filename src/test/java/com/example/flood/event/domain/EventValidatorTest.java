package com.example.flood.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventValidatorTest {
    private final EventValidator validator = new EventValidator();
    private final Instant start = Instant.parse("2026-08-21T00:00:00Z");

    @Test void ongoingMustNotHaveEnd() {
        assertInvalid(() -> validator.validateTimes(EventStatus.ONGOING, start, start.plusSeconds(1)));
    }

    @Test void endedMustHaveEndAndEndCannotPrecedeStart() {
        assertInvalid(() -> validator.validateTimes(EventStatus.ENDED, start, null));
        assertInvalid(() -> validator.validateTimes(EventStatus.ENDED, start, start.minusSeconds(1)));
    }

    @Test void observationMustNotPrecedeEventOrContainInvalidImpact() {
        EventObservation observation = new EventObservation(
            null, start.minusSeconds(1), null, null, new BigDecimal("-1"), null,
            10, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertInvalid(() -> validator.validateObservation(start, observation));
    }

    private static void assertInvalid(Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
