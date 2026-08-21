package com.example.flood.security.api;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.security.application.ApiPrincipal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ScopeAuthorizationManager {

    public Optional<String> requiredScope(String method, String path) {
        if ("POST".equals(method) && "/openapi/v1/disaster-events".equals(path)) return Optional.of("event:write");
        if ("PUT".equals(method) && path.matches("/openapi/v1/disaster-events/[^/]+")) return Optional.of("event:write");
        if ("POST".equals(method) && path.matches("/openapi/v1/disaster-events/[^/]+/observations")) return Optional.of("event:write");
        if ("GET".equals(method) && "/openapi/v1/disaster-events".equals(path)) return Optional.of("event:read");
        if ("POST".equals(method) && "/openapi/v1/region-situation-assessments".equals(path)) return Optional.of("situation:calculate");
        if ("GET".equals(method) && "/openapi/v1/region-situation-assessments".equals(path)) return Optional.of("situation:read");
        if ("POST".equals(method) && ("/openapi/v1/material-demand-calculations".equals(path)
            || "/openapi/v1/material-demand-calculations/from-region-data".equals(path))) return Optional.of("material:calculate");
        return Optional.empty();
    }

    public void authorize(ApiPrincipal principal, String method, String path) {
        String required = requiredScope(method, path)
            .orElseThrow(() -> new ApiException(ErrorCode.INSUFFICIENT_SCOPE, "Operation is not exposed"));
        if (!principal.scopes().contains(required)) {
            throw new ApiException(ErrorCode.INSUFFICIENT_SCOPE, "API key lacks required scope");
        }
    }
}
