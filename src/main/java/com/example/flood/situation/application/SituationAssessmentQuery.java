package com.example.flood.situation.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.situation.domain.SituationLevel;
import java.time.Instant;

public record SituationAssessmentQuery(
    RegionSelector region,
    Instant startTime,
    Instant endTime,
    SituationLevel situationLevel,
    int page,
    int size
) {
    public SituationAssessmentQuery {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new ApiException(ErrorCode.INVALID_TIME_RANGE,
                "startTime must be earlier than endTime");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "page must be non-negative and size must be between 1 and 100");
        }
    }

    public long offset() { return (long) page * size; }
}
