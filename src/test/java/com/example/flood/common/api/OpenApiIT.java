package com.example.flood.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OpenApiIT extends MySqlIntegrationTestBase {
    @Autowired ObjectMapper objectMapper;

    @Test
    void documentsAllOperationsApiKeyTracingIdempotencyAndErrors() throws Exception {
        var response = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode document = objectMapper.readTree(response.getBody());

        assertThat(document.at("/components/securitySchemes/ApiKeyAuth/name").asText())
            .isEqualTo("X-API-Key");
        assertThat(document.at("/components/schemas/ApiErrorResponse").isObject()).isTrue();
        assertThat(operationCount(document.get("paths"))).isEqualTo(8);
        assertThat(document.at("/paths/~1openapi~1v1~1disaster-events/post/parameters")
            .toString()).contains("Idempotency-Key", "XRequestId");
        assertThat(document.at("/paths/~1openapi~1v1~1disaster-events/post/responses/409/content"
            + "/application~1problem+json/schema/$ref").asText())
            .isEqualTo("#/components/schemas/ApiErrorResponse");
    }

    private static long operationCount(JsonNode paths) {
        return StreamSupport.stream(paths.spliterator(), false)
            .flatMap(path -> StreamSupport.stream(path.spliterator(), false))
            .count();
    }
}
