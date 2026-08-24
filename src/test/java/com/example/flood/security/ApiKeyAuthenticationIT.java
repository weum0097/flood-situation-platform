package com.example.flood.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import com.example.flood.security.application.ApiKeyHasher;
import com.example.flood.security.infrastructure.ApiClientMapper;
import com.example.flood.security.infrastructure.ApiKeyMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

class ApiKeyAuthenticationIT extends MySqlIntegrationTestBase {
    private static final String NO_SCOPE_KEY =
        "flood_live_noscope1.0123456789abcdefghijklmnopqrstuvwxyzXYZ";
    @Autowired ApiClientMapper clientMapper;
    @Autowired ApiKeyMapper keyMapper;
    @Autowired ApiKeyHasher hasher;

    @Test
    void rejectsMissingKeyAndAcceptsBootstrapKey() {
        String path = "/openapi/v1/disaster-events?regionId=320111"
            + "&startTime={startTime}&endTime={endTime}";
        var missing = rest.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, String.class,
            "2026-08-21T08:00:00+08:00", "2026-08-22T08:00:00+08:00");
        var accepted = rest.exchange(path, HttpMethod.GET, json(null, null), String.class,
            "2026-08-21T08:00:00+08:00", "2026-08-22T08:00:00+08:00");
        assertThat(missing.getStatusCode().value()).isEqualTo(401);
        assertThat(accepted.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void allEightOperationsRejectAKeyWithoutRequiredScopes() {
        clientMapper.upsert("integration-no-scope");
        long clientId = clientMapper.findIdByCode("integration-no-scope").orElseThrow();
        keyMapper.upsert(clientId, "noscope1", hasher.hash(NO_SCOPE_KEY), "[]",
            Instant.now().plus(1, ChronoUnit.DAYS));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", NO_SCOPE_KEY);
        headers.set("Idempotency-Key", "scope-check");
        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        List<Operation> operations = List.of(
            new Operation(HttpMethod.POST, "/openapi/v1/disaster-events"),
            new Operation(HttpMethod.PUT, "/openapi/v1/disaster-events/EVT_scope"),
            new Operation(HttpMethod.POST,
                "/openapi/v1/disaster-events/EVT_scope/observations"),
            new Operation(HttpMethod.GET, "/openapi/v1/disaster-events"),
            new Operation(HttpMethod.POST, "/openapi/v1/region-situation-assessments"),
            new Operation(HttpMethod.GET, "/openapi/v1/region-situation-assessments"),
            new Operation(HttpMethod.POST, "/openapi/v1/material-demand-calculations"),
            new Operation(HttpMethod.POST,
                "/openapi/v1/material-demand-calculations/from-region-data"));

        operations.forEach(operation -> {
            var response = rest.exchange(operation.path(), operation.method(), request, String.class);
            assertThat(response.getStatusCode().value())
                .as("%s %s", operation.method(), operation.path()).isEqualTo(403);
            assertThat(response.getBody()).contains("INSUFFICIENT_SCOPE");
        });
    }

    private record Operation(HttpMethod method, String path) {}
}
