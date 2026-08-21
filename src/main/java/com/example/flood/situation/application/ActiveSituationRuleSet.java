package com.example.flood.situation.application;

import com.example.flood.situation.domain.SituationRuleDefinition;
import java.util.List;

public record ActiveSituationRuleSet(
    long id,
    String version,
    Integer mediumToHighCount,
    List<SituationRuleDefinition> rules
) {
    public ActiveSituationRuleSet {
        rules = List.copyOf(rules);
    }
}
