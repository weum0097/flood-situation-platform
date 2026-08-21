package com.example.flood.situation.infrastructure;

import java.time.Instant;

public record SituationRuleSetRow(
    long id,
    String version,
    String status,
    String aggregationStrategy,
    Integer mediumToHighCount,
    Instant effectiveFrom,
    Instant effectiveTo
) {}
