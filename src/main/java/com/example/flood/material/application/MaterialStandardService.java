package com.example.flood.material.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.material.domain.MaterialStandardDefinition;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.situation.domain.SituationLevel;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MaterialStandardService {
    private final MaterialStandardLookup lookup;
    private final RegionResolver regionResolver;
    public MaterialStandardService(MaterialStandardLookup lookup, RegionResolver regionResolver) {
        this.lookup = lookup; this.regionResolver = regionResolver;
    }
    public SelectedMaterialStandards select(ResolvedRegion region, SituationLevel level, Instant instant) {
        var set = lookup.findActiveSet(instant).orElseThrow(() -> new ApiException(
            ErrorCode.NO_ACTIVE_MATERIAL_STANDARD, "No active material standard"));
        List<Long> scopes = regionResolver.specificToGlobalScopeIds(region);
        List<MaterialStandardDefinition> candidates = lookup.findDefinitions(set.id(), level, scopes);
        Map<String, MaterialStandardDefinition> selected = new LinkedHashMap<>();
        for (long scope : scopes) candidates.stream().filter(item -> item.regionScopeId() == scope)
            .forEach(item -> selected.putIfAbsent(item.materialCode(), item));
        if (selected.isEmpty()) throw new ApiException(
            ErrorCode.NO_MATERIAL_STANDARD_ITEM, "No material items for situation level");
        return new SelectedMaterialStandards(set.id(), set.version(), List.copyOf(selected.values()));
    }
}
