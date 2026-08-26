package com.example.flood.security.application;

import com.example.flood.common.api.ClientIdentity;

import java.util.Set;

public record ApiPrincipal(long clientId, long apiKeyId, String clientCode, Set<String> scopes)
    implements ClientIdentity {
    public ApiPrincipal {
        scopes = Set.copyOf(scopes);
    }
}
