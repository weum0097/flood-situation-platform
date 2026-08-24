package com.example.flood.common.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TimeConfigurationTest {

    @Test
    void applicationClockAndApiConversionUseBeijingTime() {
        var clock = new TimeConfiguration().clock();

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
        assertThat(BeijingTime.from(Instant.parse("2026-08-21T00:00:00Z")).getOffset())
            .isEqualTo(ZoneOffset.ofHours(8));
        assertThat(BeijingTime.from(Instant.parse("2026-08-21T00:00:00Z")).toString())
            .isEqualTo("2026-08-21T08:00+08:00");
    }

    @Test
    void requestTimeRequiresBeijingOffsetAndConvertsToInstant() {
        assertThat(BeijingTime.requestInstant(
            OffsetDateTime.parse("2026-08-21T08:00:00+08:00"), "startTime"))
            .isEqualTo(Instant.parse("2026-08-21T00:00:00Z"));

        assertThatThrownBy(() -> BeijingTime.requestInstant(
            OffsetDateTime.parse("2026-08-21T00:00:00Z"), "startTime"))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                assertThat(error.getMessage()).contains("startTime", "+08:00");
            });
    }
}
