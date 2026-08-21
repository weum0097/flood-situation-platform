package com.example.flood.common.idempotency;

public record IdempotentResult<T>(int httpStatus, T body, boolean replayed) {}
