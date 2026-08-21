package com.example.flood.security.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ClientRateLimiterTest {

    @Test
    void tokenBucketRefillsAccordingToPerMinuteRate() {
        ClientRateLimiter limiter = new ClientRateLimiter();
        Instant start = Instant.parse("2026-08-21T00:00:00Z");

        assertThat(limiter.tryAcquire(10L, 2, start)).isTrue();
        assertThat(limiter.tryAcquire(10L, 2, start)).isTrue();
        assertThat(limiter.tryAcquire(10L, 2, start)).isFalse();
        assertThat(limiter.tryAcquire(10L, 2, start.plusSeconds(30))).isTrue();
    }
}
