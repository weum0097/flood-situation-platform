package com.example.flood.common.time;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class BeijingTime {
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    public static final ZoneOffset OFFSET = ZoneOffset.ofHours(8);

    private BeijingTime() {}

    public static OffsetDateTime from(Instant instant) {
        return instant == null ? null : instant.atZone(ZONE_ID).toOffsetDateTime();
    }

    public static OffsetDateTime now(Clock clock) {
        return from(clock.instant());
    }

    public static Instant requestInstant(OffsetDateTime value, String field) {
        if (value == null) {
            return null;
        }
        if (!OFFSET.equals(value.getOffset())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                field + " must use Beijing time offset +08:00");
        }
        return value.toInstant();
    }
}
