package com.example.flood.common.idempotency;

import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExpiredIdempotencyCleanup {
    private final IdempotencyRecordMapper mapper;
    private final Clock clock;

    public ExpiredIdempotencyCleanup(IdempotencyRecordMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public int deleteBatch() {
        return mapper.deleteExpired(clock.instant(), 500);
    }
}
