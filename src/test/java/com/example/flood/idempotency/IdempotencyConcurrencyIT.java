package com.example.flood.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

class IdempotencyConcurrencyIT extends MySqlIntegrationTestBase {
    @Autowired ObjectMapper objectMapper;

    @Test
    void concurrentSameOperationAndKeyCreateOneResource() throws Exception {
        String body = """
            {"region":{"regionId":"320111"},"situationLevel":"MEDIUM",
             "supplyDurationHours":24,"population":{"affectedPopulation":80,
             "trappedPopulation":8,"evacuatedPopulation":16,"vulnerablePopulation":4},
             "currentInventory":[]}
            """;
        String key = "it-concurrent-material";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<ResponseEntity<String>>> futures = List.of(
                executor.submit(() -> postAfterBarrier(body, key, ready, start)),
                executor.submit(() -> postAfterBarrier(body, key, ready, start)));
            ready.await();
            start.countDown();

            ResponseEntity<String> first = futures.get(0).get();
            ResponseEntity<String> second = futures.get(1).get();
            assertThat(first.getStatusCode().value()).isEqualTo(201);
            assertThat(second.getStatusCode().value()).isEqualTo(201);
            JsonNode firstJson = objectMapper.readTree(first.getBody());
            JsonNode secondJson = objectMapper.readTree(second.getBody());
            String calculationId = firstJson.get("calculationId").asText();
            assertThat(secondJson.get("calculationId").asText()).isEqualTo(calculationId);
            assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM material_demand_calculation WHERE public_id = ?",
                Integer.class, calculationId)).isEqualTo(1);
            assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM idempotency_record
                WHERE operation_code = 'POST:/openapi/v1/material-demand-calculations'
                  AND idempotency_key = ?
                """, Integer.class, key)).isEqualTo(1);
        }
    }

    @Test
    void sameKeyCanBeUsedByAnotherOperation() {
        String key = "it-shared-across-operations";
        String event = """
            {"externalEventId":"IT-IDEMP-OTHER-OP","sourceSystem":"it-idempotency",
             "region":{"regionId":"320111"},"eventType":"URBAN_WATERLOGGING",
             "eventName":"Operation scoped key","startTime":"2026-08-21T00:00:00Z",
             "status":"ONGOING"}
            """;
        String material = """
            {"region":{"regionId":"320111"},"situationLevel":"LOW",
             "supplyDurationHours":12,"population":{"affectedPopulation":20,
             "trappedPopulation":1,"evacuatedPopulation":2,"vulnerablePopulation":1},
             "currentInventory":[]}
            """;

        var eventResponse = rest.postForEntity("/openapi/v1/disaster-events",
            json(event, key), String.class);
        var materialResponse = rest.postForEntity("/openapi/v1/material-demand-calculations",
            json(material, key), String.class);

        assertThat(eventResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(materialResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM idempotency_record WHERE idempotency_key = ?",
            Integer.class, key)).isEqualTo(2);
    }

    private ResponseEntity<String> postAfterBarrier(String body, String key,
        CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return rest.postForEntity("/openapi/v1/material-demand-calculations",
            json(body, key), String.class);
    }
}
