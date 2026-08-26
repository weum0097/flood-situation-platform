package com.example.flood.situation.domain;

import com.example.flood.event.domain.EventType;
import java.math.BigDecimal;

public record SituationRuleDefinition(
    String ruleCode, EventType eventType, MetricCode metricCode,
    ComparisonDirection comparisonDirection, BigDecimal mediumThreshold,
    BigDecimal highThreshold, int priority, boolean enabled
) {}
