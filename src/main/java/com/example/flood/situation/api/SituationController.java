package com.example.flood.situation.api;

import com.example.flood.common.api.PageResponse;
import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentOperation;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.event.api.EventObservationRequest;
import com.example.flood.event.api.HazardRequest;
import com.example.flood.event.api.ImpactRequest;
import com.example.flood.event.application.AssessmentEventImportCommand;
import com.example.flood.event.domain.EventObservation;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.security.application.ApiPrincipal;
import com.example.flood.situation.application.RegionSituationAssessmentService;
import com.example.flood.situation.application.SituationAssessmentCommand;
import com.example.flood.situation.application.SituationAssessmentQuery;
import com.example.flood.situation.domain.SituationLevel;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/openapi/v1/region-situation-assessments")
public class SituationController {
    private final RegionSituationAssessmentService service;
    private final IdempotencyExecutor idempotency;

    public SituationController(RegionSituationAssessmentService service,
        IdempotencyExecutor idempotency) {
        this.service = service;
        this.idempotency = idempotency;
    }

    @PostMapping
    ResponseEntity<SituationAssessmentResponse> assess(
        @Valid @RequestBody SituationAssessmentRequest request,
        @RequestHeader("Idempotency-Key") String key,
        @AuthenticationPrincipal ApiPrincipal principal) {
        IdempotentResult<SituationAssessmentResponse> result = idempotency.execute(principal,
            new IdempotentOperation("POST:/openapi/v1/region-situation-assessments",
                Map.of(), Map.of()), key, request, SituationAssessmentResponse.class, () -> {
                    SituationAssessmentResponse body = service.assess(toCommand(request), principal);
                    return new OperationResult<>(201, body, "SITUATION_ASSESSMENT",
                        body.assessmentId());
                });
        return ResponseEntity.status(result.httpStatus()).body(result.body());
    }

    @GetMapping
    ResponseEntity<PageResponse<SituationAssessmentSummaryResponse>> search(
        @RequestParam(required = false) String regionId,
        @RequestParam(required = false) String regionName,
        @RequestParam OffsetDateTime startTime,
        @RequestParam OffsetDateTime endTime,
        @RequestParam(required = false) SituationLevel situationLevel,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(new SituationAssessmentQuery(
            new RegionSelector(regionId, regionName), startTime.toInstant(), endTime.toInstant(),
            situationLevel, page, size)));
    }

    private static SituationAssessmentCommand toCommand(SituationAssessmentRequest request) {
        List<AssessmentEventImportCommand> events = request.events().stream()
            .map(event -> new AssessmentEventImportCommand(event.externalEventId(),
                event.sourceSystem(), event.eventType(), event.eventName(),
                event.startTime().toInstant(),
                event.endTime() == null ? null : event.endTime().toInstant(), event.status(),
                observation(event.observation())))
            .toList();
        return new SituationAssessmentCommand(request.region(),
            request.assessmentTime().toInstant(), events);
    }

    private static EventObservation observation(EventObservationRequest request) {
        HazardRequest hazard = request.hazard() == null
            ? new HazardRequest(null, null, null, null) : request.hazard();
        ImpactRequest impact = request.impact();
        return new EventObservation(request.externalObservationId(), request.observedAt().toInstant(),
            hazard.rainfall24hMm(), hazard.waterLevelOverWarningM(), hazard.maxWaterDepthM(),
            hazard.affectedAreaKm2(), impact.affectedPopulation(), impact.trappedPopulation(),
            impact.evacuatedPopulation(), impact.vulnerablePopulation(), impact.injuredPopulation(),
            impact.missingPopulation(), impact.deathPopulation(), impact.damagedHouseholds(),
            impact.collapsedHouses(), impact.roadInterruptions(),
            impact.criticalFacilitiesAffected(), impact.powerOutageHouseholds());
    }
}
