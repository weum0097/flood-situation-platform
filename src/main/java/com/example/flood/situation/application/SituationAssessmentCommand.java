package com.example.flood.situation.application;

import com.example.flood.event.application.AssessmentEventImportCommand;
import com.example.flood.region.application.RegionSelector;
import java.time.Instant;
import java.util.List;

public record SituationAssessmentCommand(
    RegionSelector region,
    Instant assessmentTime,
    List<AssessmentEventImportCommand> events
) {
    public SituationAssessmentCommand { events = List.copyOf(events); }
}
