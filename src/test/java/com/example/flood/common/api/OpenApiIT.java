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

    @Test
    void publishesOnlyResolvableLocalReferences() throws Exception {
        var response = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode document = objectMapper.readTree(response.getBody());

        var unresolvedReferences = document.findValuesAsText("$ref").stream()
            .filter(reference -> reference.startsWith("#/"))
            .filter(reference -> document.at(reference.substring(1)).isMissingNode())
            .distinct()
            .toList();

        assertThat(unresolvedReferences).isEmpty();
    }

    @Test
    void documentsOperationsParametersAndBusinessFieldsInChinese() throws Exception {
        var response = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode document = objectMapper.readTree(response.getBody());

        JsonNode createEvent = document.at(
            "/paths/~1openapi~1v1~1disaster-events/post");
        assertThat(createEvent.path("summary").asText()).isEqualTo("新增灾害事件");
        assertThat(createEvent.path("tags").toString()).contains("灾害事件");
        assertThat(createEvent.path("parameters").toString())
            .contains("用于保证写请求幂等");

        JsonNode searchEvents = document.at(
            "/paths/~1openapi~1v1~1disaster-events/get");
        assertThat(searchEvents.path("summary").asText()).isEqualTo("查询灾害事件");
        assertThat(searchEvents.path("parameters").toString())
            .contains("行政区域编码", "查询窗口开始时间");

        JsonNode createSchema = document.at("/components/schemas/CreateEventRequest/properties");
        assertThat(createSchema.path("eventType").path("description").asText())
            .contains("RIVER_FLOOD", "URBAN_WATERLOGGING");
        assertThat(createSchema.path("status").path("description").asText())
            .contains("ONGOING", "endTime");
        assertThat(createSchema.path("endTime").path("description").asText())
            .contains("ONGOING", "ENDED");

        assertThat(document.at(
            "/paths/~1openapi~1v1~1region-situation-assessments/post/summary").asText())
            .isEqualTo("计算区域洪水情景态势");
        assertThat(document.at(
            "/paths/~1openapi~1v1~1material-demand-calculations/post/summary").asText())
            .isEqualTo("按输入数据计算物资需求");
    }

    private static long operationCount(JsonNode paths) {
        return StreamSupport.stream(paths.spliterator(), false)
            .flatMap(path -> StreamSupport.stream(path.spliterator(), false))
            .count();
    }
}
