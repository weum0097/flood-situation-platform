package com.example.flood.region.application;

import com.example.flood.region.domain.ResolvedRegion;
import java.util.List;
import java.util.Optional;

public interface RegionLookup {
    Optional<ResolvedRegion> findActiveByCode(String regionCode);

    List<ResolvedRegion> findActiveByNormalizedName(String normalizedName);

    Optional<ResolvedRegion> findActiveByDatabaseId(long id);
}
