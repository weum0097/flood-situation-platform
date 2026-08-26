package com.example.flood.common.api;

import java.util.Set;

public record RequestContext(
    String requestId,
    Long clientId,
    Long apiKeyId,
    String clientCode,
    Set<String> scopes,
    String remoteIp
) {
    public RequestContext {
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }
}
