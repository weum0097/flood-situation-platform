package com.example.flood.material.infrastructure;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.material.application.MaterialStandardLookup;
import com.example.flood.material.domain.MaterialStandardDefinition;
import com.example.flood.material.domain.PopulationBasis;
import com.example.flood.situation.domain.SituationLevel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class MybatisMaterialStandardLookup implements MaterialStandardLookup {
    private final MaterialStandardMapper mapper;
    public MybatisMaterialStandardLookup(MaterialStandardMapper mapper) { this.mapper = mapper; }

    @Override
    public Optional<StandardSet> findActiveSet(Instant instant) {
        var sets = mapper.findActiveSets(instant);
        if (sets.size() > 1) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                "Multiple active material standard sets overlap");
        }
        return sets.stream().findFirst().map(row -> new StandardSet(row.id(), row.version()));
    }

    @Override
    public List<MaterialStandardDefinition> findDefinitions(long setId, SituationLevel level,
        List<Long> regionScopeIds) {
        return mapper.findDefinitions(setId, level, regionScopeIds).stream()
            .map(row -> new MaterialStandardDefinition(row.id(), row.regionScopeId(),
                row.materialCode(), row.materialName(), row.unit(),
                PopulationBasis.valueOf(row.populationBasis()), row.perPersonPerDay(),
                row.fixedBaseQuantity(), row.levelFactor(), row.reserveRatio(),
                row.packageSize(), row.minimumQuantity()))
            .toList();
    }
}
