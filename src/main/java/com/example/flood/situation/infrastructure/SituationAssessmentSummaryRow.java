package com.example.flood.situation.infrastructure;

import java.time.Instant;

public record SituationAssessmentSummaryRow(
    long id,
    String publicId,
    String regionCode,
    String regionName,
    Instant assessmentTime,
    String situationLevel,
    int activeEventCount,
    long affectedPopulation,
    long trappedPopulation,
    long evacuatedPopulation,
    long vulnerablePopulation,
    String eventIds,
    String ruleVersion,
    String warnings
) {}
