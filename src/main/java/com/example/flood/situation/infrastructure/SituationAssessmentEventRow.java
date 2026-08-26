package com.example.flood.situation.infrastructure;

import java.math.BigDecimal;

public record SituationAssessmentEventRow(
    long assessmentId,
    long eventId,
    long observationId,
    String eventLevel,
    BigDecimal durationHours,
    String matchedRuleCodes
) {}
