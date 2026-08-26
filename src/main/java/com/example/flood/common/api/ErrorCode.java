package com.example.flood.common.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    MALFORMED_REQUEST(400),
    INVALID_API_KEY(401),
    INSUFFICIENT_SCOPE(403),
    REGION_NOT_FOUND(404),
    EVENT_NOT_FOUND(404),
    REGION_NAME_AMBIGUOUS(409),
    REGION_SELECTOR_MISMATCH(409),
    EVENT_CONFLICT(409),
    OBSERVATION_CONFLICT(409),
    IDEMPOTENCY_CONFLICT(409),
    VALIDATION_ERROR(422),
    INVALID_TIME_RANGE(422),
    REGION_EVENT_MISMATCH(422),
    NO_ACTIVE_RULE_SET(422),
    NO_SITUATION_DATA(422),
    NO_ACTIVE_MATERIAL_STANDARD(422),
    NO_MATERIAL_STANDARD_ITEM(422),
    UNIT_MISMATCH(422),
    RATE_LIMIT_EXCEEDED(429),
    INTERNAL_ERROR(500);

    private final HttpStatus status;

    ErrorCode(int status) {
        this.status = HttpStatus.valueOf(status);
    }

    public HttpStatus status() {
        return status;
    }
}
