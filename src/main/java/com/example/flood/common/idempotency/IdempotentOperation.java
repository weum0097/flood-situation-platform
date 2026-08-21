package com.example.flood.common.idempotency;

import java.util.List;
import java.util.Map;

public record IdempotentOperation(
    String operationCode,
    Map<String, String> pathVariables,
    Map<String, List<String>> queryParameters
) {
    public IdempotentOperation {
        pathVariables = pathVariables == null ? Map.of() : Map.copyOf(pathVariables);
        queryParameters = queryParameters == null ? Map.of() : Map.copyOf(queryParameters);
    }
}
