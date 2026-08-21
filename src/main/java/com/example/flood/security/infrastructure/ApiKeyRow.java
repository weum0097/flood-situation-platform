package com.example.flood.security.infrastructure;

import java.time.Instant;

public record ApiKeyRow(
    long id, long clientId, String keyPrefix, byte[] secretHash,
    String scopes, String status, Instant expiresAt, Instant lastUsedAt
) {}
