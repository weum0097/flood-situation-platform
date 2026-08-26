package com.example.flood.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalRequestHasherTest {

    private final CanonicalRequestHasher hasher = new CanonicalRequestHasher(new ObjectMapper());

    @Test
    void ignoresObjectOrderAndDecimalScale() {
        IdempotentOperation operation = new IdempotentOperation("POST:/test", Map.of(), Map.of());
        Map<String, Object> first = Map.of("b", new BigDecimal("1.00"), "a", "value");
        Map<String, Object> second = Map.of("a", "value", "b", new BigDecimal("1"));

        assertThat(hasher.hash(operation, first)).isEqualTo(hasher.hash(operation, second));
    }

    @Test
    void includesPathVariablesAndOperationCode() {
        Object body = Map.of("value", 1);
        IdempotentOperation create = new IdempotentOperation("POST:/test", Map.of(), Map.of());
        IdempotentOperation update = new IdempotentOperation(
            "PUT:/test/{id}", Map.of("id", "EVT_1"), Map.of("tag", List.of("a")));

        assertThat(hasher.hash(create, body)).isNotEqualTo(hasher.hash(update, body));
    }
}
