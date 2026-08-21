package com.example.flood.common.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CanonicalRequestHasher {
    private final ObjectMapper objectMapper;

    public CanonicalRequestHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] hash(IdempotentOperation operation, Object request) {
        JsonNode root = objectMapper.valueToTree(Map.of(
            "operationCode", operation.operationCode(),
            "pathVariables", operation.pathVariables(),
            "queryParameters", operation.queryParameters(),
            "request", request));
        try {
            return MessageDigest.getInstance("SHA-256")
                .digest(canonical(root).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String canonical(JsonNode node) {
        if (node.isObject()) {
            List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            iterator.forEachRemaining(fields::add);
            fields.sort(Comparator.comparing(Map.Entry::getKey));
            return fields.stream()
                .map(field -> quote(field.getKey()) + ":" + canonical(field.getValue()))
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(value -> values.add(canonical(value)));
            return String.join(",", values).transform(value -> "[" + value + "]");
        }
        if (node.isNumber()) {
            return node.decimalValue().stripTrailingZeros().toPlainString();
        }
        if (node.isTextual()) return quote(node.textValue());
        if (node.isBoolean()) return Boolean.toString(node.booleanValue());
        return "null";
    }

    private String quote(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not canonicalize request", exception);
        }
    }
}
