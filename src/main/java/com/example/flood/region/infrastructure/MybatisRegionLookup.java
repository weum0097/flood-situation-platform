package com.example.flood.region.infrastructure;

import com.example.flood.region.application.RegionLookup;
import com.example.flood.region.domain.RegionStatus;
import com.example.flood.region.domain.ResolvedRegion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Repository
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class MybatisRegionLookup implements RegionLookup {

    private final RegionMapper mapper;

    public MybatisRegionLookup(RegionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ResolvedRegion> findActiveByCode(String regionCode) {
        return mapper.findActiveByCode(regionCode).map(MybatisRegionLookup::toDomain);
    }

    @Override
    public List<ResolvedRegion> findActiveByNormalizedName(String normalizedName) {
        return mapper.findActiveByName(normalizedName).stream()
            .map(MybatisRegionLookup::toDomain)
            .toList();
    }

    @Override
    public Optional<ResolvedRegion> findActiveByDatabaseId(long id) {
        return mapper.findActiveById(id).map(MybatisRegionLookup::toDomain);
    }

    private static ResolvedRegion toDomain(RegionRow row) {
        return new ResolvedRegion(
            row.id(),
            row.regionCode(),
            row.regionName(),
            row.parentId(),
            row.regionLevel(),
            RegionStatus.valueOf(row.status()));
    }
}
