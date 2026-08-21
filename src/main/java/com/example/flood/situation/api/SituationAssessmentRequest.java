package com.example.flood.situation.api;

import com.example.flood.region.application.RegionSelector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record SituationAssessmentRequest(
    @Valid @NotNull RegionSelector region,
    @NotNull OffsetDateTime assessmentTime,
    @Valid @NotEmpty List<SituationEventImportRequest> events
) {
    public SituationAssessmentRequest {
        events = events == null ? null : List.copyOf(events);
    }
}
