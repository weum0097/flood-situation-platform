package com.example.flood.region.domain;

public record ResolvedRegion(
    long id,
    String regionCode,
    String regionName,
    Long parentId,
    String regionLevel,
    RegionStatus status
) {}
