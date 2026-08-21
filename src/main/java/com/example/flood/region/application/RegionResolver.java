package com.example.flood.region.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ApiFieldError;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.region.domain.ResolvedRegion;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class RegionResolver {

    private static final int MAX_PARENT_DEPTH = 16;
    private final RegionLookup lookup;

    public RegionResolver(RegionLookup lookup) {
        this.lookup = lookup;
    }

    public ResolvedRegion resolve(RegionSelector selector) {
        if (selector == null || (selector.regionId() == null && selector.regionName() == null)) {
            throw new ApiException(
                ErrorCode.VALIDATION_ERROR,
                "At least one region selector is required",
                List.of(new ApiFieldError("regionId", "regionId or regionName is required")));
        }

        if (selector.regionId() != null) {
            ResolvedRegion byCode = lookup.findActiveByCode(selector.regionId())
                .orElseThrow(RegionResolver::notFound);
            if (selector.regionName() != null
                && !normalizeName(byCode.regionName()).equals(normalizeName(selector.regionName()))) {
                throw new ApiException(
                    ErrorCode.REGION_SELECTOR_MISMATCH,
                    "regionId and regionName refer to different regions");
            }
            return byCode;
        }

        List<ResolvedRegion> matches = lookup.findActiveByNormalizedName(
            normalizeName(selector.regionName()));
        if (matches.isEmpty()) {
            throw notFound();
        }
        if (matches.size() > 1) {
            throw new ApiException(
                ErrorCode.REGION_NAME_AMBIGUOUS,
                "regionName matches more than one active region");
        }
        return matches.getFirst();
    }

    public List<Long> specificToGlobalScopeIds(ResolvedRegion region) {
        List<Long> scopes = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ResolvedRegion current = region;
        for (int depth = 0; depth < MAX_PARENT_DEPTH; depth++) {
            if (!visited.add(current.id())) {
                throw hierarchyError("Region parent hierarchy contains a cycle");
            }
            scopes.add(current.id());
            if (current.parentId() == null) {
                scopes.add(0L);
                return List.copyOf(scopes);
            }
            long parentId = current.parentId();
            current = lookup.findActiveByDatabaseId(parentId)
                .orElseThrow(() -> hierarchyError("Region parent is missing or inactive"));
        }
        throw hierarchyError("Region parent hierarchy exceeds maximum depth");
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return Normalizer.normalize(name, Normalizer.Form.NFKC)
            .strip()
            .replaceAll("\\s+", " ");
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.REGION_NOT_FOUND, "Active region was not found");
    }

    private static ApiException hierarchyError(String message) {
        return new ApiException(ErrorCode.INTERNAL_ERROR, message);
    }
}
