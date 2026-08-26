package com.example.flood.common.api;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public PageResponse {
        items = List.copyOf(items);
    }
}
