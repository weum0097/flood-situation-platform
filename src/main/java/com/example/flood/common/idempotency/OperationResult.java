package com.example.flood.common.idempotency;

public record OperationResult<T>(
    int httpStatus,
    T body,
    String resourceType,
    String resourcePublicId
) {}
