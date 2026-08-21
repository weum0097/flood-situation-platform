package com.example.flood.event.application;

import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.security.application.ApiPrincipal;
import java.time.Instant;
import java.util.List;

public interface EventAssessmentImportPort {
    List<ImportedAssessmentEvent> importForAssessment(
        ResolvedRegion rootRegion,
        Instant assessmentTime,
        List<AssessmentEventImportCommand> events,
        ApiPrincipal principal);
}
