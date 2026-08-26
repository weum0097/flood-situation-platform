package com.example.flood.common.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
    String requestId,
    String errorCode,
    String message,
    List<ApiFieldError> details,
    OffsetDateTime timestamp
) {
    public ApiErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
