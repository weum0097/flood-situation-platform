package com.example.flood.security.application;

import java.util.Set;

public record ApiPrincipal(long clientId, long apiKeyId, String clientCode, Set<String> scopes) {
    public ApiPrincipal {
        scopes = Set.copyOf(scopes);
    }
}
