package com.example.flood.situation.api;

import com.example.flood.region.application.RegionSelector;
import com.example.flood.situation.domain.SituationLevel;
import java.time.OffsetDateTime;
import java.util.List;

public record SituationAssessmentSummaryResponse(
    String assessmentId,
    RegionSelector region,
    OffsetDateTime assessmentTime,
    SituationLevel situationLevel,
    int activeEventCount,
    SituationAssessmentResponse.AggregateImpact aggregateImpact,
    List<String> eventIds,
    String ruleVersion,
    List<SituationAssessmentResponse.Warning> warnings
) {
    public SituationAssessmentSummaryResponse {
        eventIds = List.copyOf(eventIds);
        warnings = List.copyOf(warnings);
    }

    public static SituationAssessmentSummaryResponse from(SituationAssessmentResponse response) {
        return new SituationAssessmentSummaryResponse(response.assessmentId(), response.region(),
            response.assessmentTime(), response.situationLevel(), response.activeEventCount(),
            response.aggregateImpact(), response.eventResults().stream()
                .map(SituationAssessmentResponse.EventResult::eventId).toList(),
            response.ruleVersion(), response.warnings());
    }
}
