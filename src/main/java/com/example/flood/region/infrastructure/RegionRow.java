package com.example.flood.region.infrastructure;

public record RegionRow(
    long id,
    String regionCode,
    String regionName,
    Long parentId,
    String regionLevel,
    String status
) {}
