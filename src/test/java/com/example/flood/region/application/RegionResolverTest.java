package com.example.flood.region.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.region.domain.RegionStatus;
import com.example.flood.region.domain.ResolvedRegion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegionResolverTest {

    private FakeLookup lookup;
    private RegionResolver resolver;

    @BeforeEach
    void setUp() {
        lookup = new FakeLookup();
        lookup.add(region(1, "320000", "江苏省", null, "PROVINCE", RegionStatus.ACTIVE));
        lookup.add(region(2, "320100", "南京市", 1L, "CITY", RegionStatus.ACTIVE));
        lookup.add(region(3, "320111", "浦口区", 2L, "DISTRICT", RegionStatus.ACTIVE));
        resolver = new RegionResolver(lookup);
    }

    @Test
    void resolvesByExternalRegionId() {
        assertThat(resolver.resolve(new RegionSelector("320111", null)).id()).isEqualTo(3);
    }

    @Test
    void resolvesByNormalizedName() {
        assertThat(resolver.resolve(new RegionSelector(null, "  浦口区  ")).regionCode())
            .isEqualTo("320111");
    }

    @Test
    void acceptsMatchingIdAndName() {
        assertThat(resolver.resolve(new RegionSelector("320111", "浦口区")).id()).isEqualTo(3);
    }

    @Test
    void rejectsMismatchedIdAndName() {
        assertError(
            () -> resolver.resolve(new RegionSelector("320111", "南京市")),
            ErrorCode.REGION_SELECTOR_MISMATCH);
    }

    @Test
    void rejectsAmbiguousName() {
        lookup.add(region(4, "999999", "浦口区", null, "OTHER", RegionStatus.ACTIVE));

        assertError(
            () -> resolver.resolve(new RegionSelector(null, "浦口区")),
            ErrorCode.REGION_NAME_AMBIGUOUS);
    }

    @Test
    void treatsInactiveRegionAsNotFound() {
        lookup.add(region(5, "inactive", "停用区域", null, "OTHER", RegionStatus.INACTIVE));

        assertError(
            () -> resolver.resolve(new RegionSelector("inactive", null)),
            ErrorCode.REGION_NOT_FOUND);
    }

    @Test
    void requiresAtLeastOneSelector() {
        assertError(
            () -> resolver.resolve(new RegionSelector(" ", null)),
            ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void returnsSpecificParentChainEndingWithGlobalScope() {
        ResolvedRegion district = resolver.resolve(new RegionSelector("320111", null));

        assertThat(resolver.specificToGlobalScopeIds(district)).containsExactly(3L, 2L, 1L, 0L);
    }

    @Test
    void detectsParentCycle() {
        lookup.add(region(10, "cycle-a", "循环甲", 11L, "OTHER", RegionStatus.ACTIVE));
        lookup.add(region(11, "cycle-b", "循环乙", 10L, "OTHER", RegionStatus.ACTIVE));

        assertError(
            () -> resolver.specificToGlobalScopeIds(
                resolver.resolve(new RegionSelector("cycle-a", null))),
            ErrorCode.INTERNAL_ERROR);
    }

    private static ResolvedRegion region(
        long id,
        String code,
        String name,
        Long parentId,
        String level,
        RegionStatus status
    ) {
        return new ResolvedRegion(id, code, name, parentId, level, status);
    }

    private static void assertError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(errorCode));
    }

    private static class FakeLookup implements RegionLookup {
        private final Map<Long, ResolvedRegion> byId = new HashMap<>();

        void add(ResolvedRegion region) {
            byId.put(region.id(), region);
        }

        @Override
        public Optional<ResolvedRegion> findActiveByCode(String regionCode) {
            return byId.values().stream()
                .filter(region -> region.status() == RegionStatus.ACTIVE)
                .filter(region -> region.regionCode().equals(regionCode))
                .findFirst();
        }

        @Override
        public List<ResolvedRegion> findActiveByNormalizedName(String normalizedName) {
            return byId.values().stream()
                .filter(region -> region.status() == RegionStatus.ACTIVE)
                .filter(region -> RegionResolver.normalizeName(region.regionName())
                    .equals(normalizedName))
                .toList();
        }

        @Override
        public Optional<ResolvedRegion> findActiveByDatabaseId(long id) {
            return Optional.ofNullable(byId.get(id))
                .filter(region -> region.status() == RegionStatus.ACTIVE);
        }
    }
}
