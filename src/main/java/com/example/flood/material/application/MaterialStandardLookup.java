package com.example.flood.material.application;

import com.example.flood.material.domain.MaterialStandardDefinition;
import com.example.flood.situation.domain.SituationLevel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MaterialStandardLookup {
    Optional<StandardSet> findActiveSet(Instant instant);
    List<MaterialStandardDefinition> findDefinitions(long setId, SituationLevel level,
        List<Long> regionScopeIds);
    record StandardSet(long id, String version) {}
}
