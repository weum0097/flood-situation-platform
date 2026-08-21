package com.example.flood.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

class ApiKeyAuthenticationIT extends MySqlIntegrationTestBase {
    @Test
    void rejectsMissingKeyAndAcceptsBootstrapKey() {
        var missing = rest.exchange("/openapi/v1/disaster-events?regionId=320111"
            + "&startTime=2026-08-21T00:00:00Z&endTime=2026-08-22T00:00:00Z",
            HttpMethod.GET, HttpEntity.EMPTY, String.class);
        var accepted = rest.exchange("/openapi/v1/disaster-events?regionId=320111"
            + "&startTime=2026-08-21T00:00:00Z&endTime=2026-08-22T00:00:00Z",
            HttpMethod.GET, json(null, null), String.class);
        assertThat(missing.getStatusCode().value()).isEqualTo(401);
        assertThat(accepted.getStatusCode().value()).isEqualTo(200);
    }
}
