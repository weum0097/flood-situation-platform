package com.example.flood.common.idempotency;

public record IdempotencyRecordRow(
    long id,
    long clientId,
    String operationCode,
    String idempotencyKey,
    byte[] requestHash,
    Integer responseStatus,
    String responseBody,
    String resourceType,
    String resourcePublicId
) {}
