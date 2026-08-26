package com.example.flood.situation.infrastructure;

import java.math.BigDecimal;

public record SituationRuleRow(
    long id,
    long ruleSetId,
    String ruleCode,
    String eventType,
    String metricCode,
    String comparisonDirection,
    BigDecimal mediumThreshold,
    BigDecimal highThreshold,
    int priority,
    boolean enabled
) {}
