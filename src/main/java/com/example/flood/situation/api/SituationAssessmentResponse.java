package com.example.flood.situation.api;

import com.example.flood.region.application.RegionSelector;
import com.example.flood.situation.domain.SituationLevel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SituationAssessmentResponse(
    String assessmentId,
    RegionSelector region,
    OffsetDateTime assessmentTime,
    SituationLevel situationLevel,
    int activeEventCount,
    AggregateImpact aggregateImpact,
    List<EventResult> eventResults,
    ImportSummary importSummary,
    String ruleVersion,
    List<Warning> warnings,
    OffsetDateTime createdAt
) {
    public SituationAssessmentResponse {
        eventResults = List.copyOf(eventResults);
        warnings = List.copyOf(warnings);
    }

    public record AggregateImpact(long affectedPopulation, long trappedPopulation,
        long evacuatedPopulation, long vulnerablePopulation) {}

    public record EventResult(String eventId, String externalEventId, String observationId,
        SituationLevel eventLevel, BigDecimal durationHours, List<String> matchedRules) {
        public EventResult { matchedRules = List.copyOf(matchedRules); }
    }

    public record ImportSummary(int createdEvents, int updatedEvents,
        int createdObservations, int duplicatedObservations) {}

    public record Warning(String code, String message) {}
}
