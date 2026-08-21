package com.example.flood.security.application;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public record ApiKeyCredential(
    long clientId,
    long apiKeyId,
    String clientCode,
    String keyPrefix,
    byte[] secretHash,
    Set<String> scopes,
    String keyStatus,
    String clientStatus,
    Instant expiresAt,
    Instant lastUsedAt,
    int rateLimitPerMinute,
    List<String> allowedIps
) {
    public ApiKeyCredential {
        secretHash = Arrays.copyOf(secretHash, secretHash.length);
        scopes = Set.copyOf(scopes);
        allowedIps = allowedIps == null ? List.of() : List.copyOf(allowedIps);
    }

    @Override
    public byte[] secretHash() {
        return Arrays.copyOf(secretHash, secretHash.length);
    }
}
