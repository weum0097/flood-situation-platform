package com.example.flood.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class ExpiredIdempotencyCleanupTest {

    @Test
    void deletesOneBoundedBatchUsingCurrentTime() {
        IdempotencyRecordMapper mapper = mock(IdempotencyRecordMapper.class);
        Instant now = Instant.parse("2026-08-21T05:00:00Z");
        when(mapper.deleteExpired(now, 500)).thenReturn(12);

        int deleted = new ExpiredIdempotencyCleanup(
            mapper, Clock.fixed(now, ZoneOffset.UTC)).deleteBatch();

        assertThat(deleted).isEqualTo(12);
        verify(mapper).deleteExpired(now, 500);
    }

    @Test
    void cleanupIsScheduledAtConfigurableInterval() throws Exception {
        Scheduled scheduled = ExpiredIdempotencyCleanup.class
            .getDeclaredMethod("deleteBatch")
            .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString())
            .isEqualTo("${flood.idempotency.cleanup-interval:PT10M}");
    }
}
