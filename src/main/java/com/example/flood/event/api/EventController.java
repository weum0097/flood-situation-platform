package com.example.flood.event.api;

import com.example.flood.common.api.PageResponse;
import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentOperation;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.event.application.CreateEventCommand;
import com.example.flood.event.application.EventApplicationService;
import com.example.flood.event.application.AppendObservationCommand;
import com.example.flood.event.application.EventQuery;
import com.example.flood.event.application.EventQueryService;
import com.example.flood.event.application.UpdateEventCommand;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventObservation;
import com.example.flood.security.application.ApiPrincipal;
import jakarta.validation.Valid;
import java.util.Map;
import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/openapi/v1/disaster-events")
public class EventController {
    private final EventApplicationService service;
    private final EventQueryService queryService;
    private final IdempotencyExecutor idempotency;

    public EventController(EventApplicationService service, EventQueryService queryService,
        IdempotencyExecutor idempotency) {
        this.service = service; this.queryService = queryService; this.idempotency = idempotency;
    }

    @PostMapping("/{eventId}/observations")
    ResponseEntity<ObservationResponse> appendObservation(@PathVariable String eventId,
        @Valid @RequestBody EventObservationRequest request,
        @RequestHeader("Idempotency-Key") String key,
        @AuthenticationPrincipal ApiPrincipal principal) {
        IdempotentResult<ObservationResponse> result = idempotency.execute(principal,
            new IdempotentOperation(
                "POST:/openapi/v1/disaster-events/{eventId}/observations",
                Map.of("eventId", eventId), Map.of()),
            key, request, ObservationResponse.class, () -> {
                ObservationResponse body = service.appendObservation(
                    eventId, new AppendObservationCommand(observation(request)), principal);
                return new OperationResult<>(201, body, "OBSERVATION", body.observationId());
            });
        return ResponseEntity.status(result.httpStatus()).body(result.body());
    }

    @GetMapping
    ResponseEntity<PageResponse<EventSummaryResponse>> search(
        @RequestParam(required = false) String regionId,
        @RequestParam(required = false) String regionName,
        @RequestParam OffsetDateTime startTime,
        @RequestParam OffsetDateTime endTime,
        @RequestParam(required = false) EventStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(queryService.search(new EventQuery(
            new com.example.flood.region.application.RegionSelector(regionId, regionName),
            startTime.toInstant(), endTime.toInstant(), status, page, size)));
    }

    @PostMapping
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
        @RequestHeader("Idempotency-Key") String key, @AuthenticationPrincipal ApiPrincipal principal) {
        IdempotentResult<EventResponse> result = idempotency.execute(principal,
            new IdempotentOperation("POST:/openapi/v1/disaster-events", Map.of(), Map.of()),
            key, request, EventResponse.class, () -> {
                EventResponse body = service.create(toCommand(request), principal);
                return new OperationResult<>(201, body, "EVENT", body.eventId());
            });
        return ResponseEntity.status(result.httpStatus()).body(result.body());
    }

    @PutMapping("/{eventId}")
    ResponseEntity<EventResponse> update(@PathVariable String eventId,
        @Valid @RequestBody UpdateEventRequest request, @RequestHeader("Idempotency-Key") String key,
        @AuthenticationPrincipal ApiPrincipal principal) {
        IdempotentResult<EventResponse> result = idempotency.execute(principal,
            new IdempotentOperation("PUT:/openapi/v1/disaster-events/{eventId}",
                Map.of("eventId", eventId), Map.of()), key, request, EventResponse.class, () -> {
                    EventResponse body = service.update(eventId, new UpdateEventCommand(
                        request.eventType(), request.eventName(), request.startTime().toInstant(),
                        request.endTime() == null ? null : request.endTime().toInstant(), request.status()), principal);
                    return new OperationResult<>(200, body, "EVENT", body.eventId());
                });
        return ResponseEntity.status(result.httpStatus()).body(result.body());
    }

    private static CreateEventCommand toCommand(CreateEventRequest r) {
        return new CreateEventCommand(r.externalEventId(), r.sourceSystem(), r.region(), r.eventType(),
            r.eventName(), r.startTime().toInstant(), r.endTime() == null ? null : r.endTime().toInstant(),
            r.status(), observation(r.initialObservation()));
    }

    static EventObservation observation(EventObservationRequest r) {
        if (r == null) return null;
        HazardRequest h = r.hazard() == null ? new HazardRequest(null, null, null, null) : r.hazard();
        ImpactRequest i = r.impact();
        return new EventObservation(r.externalObservationId(), r.observedAt().toInstant(),
            h.rainfall24hMm(), h.waterLevelOverWarningM(), h.maxWaterDepthM(), h.affectedAreaKm2(),
            i.affectedPopulation(), i.trappedPopulation(), i.evacuatedPopulation(),
            i.vulnerablePopulation(), i.injuredPopulation(), i.missingPopulation(),
            i.deathPopulation(), i.damagedHouseholds(), i.collapsedHouses(),
            i.roadInterruptions(), i.criticalFacilitiesAffected(), i.powerOutageHouseholds());
    }
}
