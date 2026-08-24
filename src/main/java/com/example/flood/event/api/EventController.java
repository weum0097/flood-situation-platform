package com.example.flood.event.api;

import com.example.flood.common.api.PageResponse;
import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentOperation;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.common.time.BeijingTime;
import com.example.flood.event.application.CreateEventCommand;
import com.example.flood.event.application.EventApplicationService;
import com.example.flood.event.application.AppendObservationCommand;
import com.example.flood.event.application.EventQuery;
import com.example.flood.event.application.EventQueryService;
import com.example.flood.event.application.UpdateEventCommand;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventObservation;
import com.example.flood.security.application.ApiPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "灾害事件", description = "新增、更新、查询洪水灾害事件及追加灾情观测")
public class EventController {
    private final EventApplicationService service;
    private final EventQueryService queryService;
    private final IdempotencyExecutor idempotency;

    public EventController(EventApplicationService service, EventQueryService queryService,
        IdempotencyExecutor idempotency) {
        this.service = service; this.queryService = queryService; this.idempotency = idempotency;
    }

    @PostMapping("/{eventId}/observations")
    @Operation(summary = "追加灾情观测",
        description = "为指定事件写入一个时点的灾害强度和受灾影响数据。观测时间不得早于事件开始时间。")
    @ApiResponse(responseCode = "201", description = "观测新增成功；重复提交相同自然键和内容时返回已有观测")
    ResponseEntity<ObservationResponse> appendObservation(
        @Parameter(description = "事件公开编号，必须使用 EVT_ 开头的接口返回值",
            example = "EVT_01m0hp1as75c71rfyb11py43ra") @PathVariable String eventId,
        @Valid @RequestBody EventObservationRequest request,
        @Parameter(description = "用于保证写请求幂等；不同请求内容必须使用不同值",
            example = "swagger-observation-create-001")
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
    @Operation(summary = "查询灾害事件",
        description = "按区域和时间区间分页查询灾害事件，可选按事件状态过滤。regionId 和 regionName 至少填写一个。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    ResponseEntity<PageResponse<EventSummaryResponse>> search(
        @Parameter(description = "行政区域编码；与 regionName 至少填写一个", example = "320111")
        @RequestParam(required = false) String regionId,
        @Parameter(description = "行政区域名称；仅填写名称时必须能够唯一匹配", example = "浦口区")
        @RequestParam(required = false) String regionName,
        @Parameter(description = "查询窗口开始时间，ISO 8601 格式，必须使用北京时间偏移 +08:00",
            example = "2026-08-21T00:00:00+08:00") @RequestParam OffsetDateTime startTime,
        @Parameter(description = "查询窗口结束时间，必须使用北京时间偏移 +08:00，且晚于或等于 startTime",
            example = "2026-08-22T00:00:00+08:00") @RequestParam OffsetDateTime endTime,
        @Parameter(description = "可选事件状态：ONGOING、ENDED、CANCELLED", example = "ONGOING")
        @RequestParam(required = false) EventStatus status,
        @Parameter(description = "页码，从 0 开始", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "每页数量", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(queryService.search(new EventQuery(
            new com.example.flood.region.application.RegionSelector(regionId, regionName),
            BeijingTime.requestInstant(startTime, "startTime"),
            BeijingTime.requestInstant(endTime, "endTime"), status, page, size)));
    }

    @PostMapping
    @Operation(summary = "新增灾害事件",
        description = "新增一条灾害事件，可同时写入首条观测。ONGOING 的 endTime 必须为空，ENDED 的 endTime 必填。")
    @ApiResponse(responseCode = "201", description = "事件新增成功")
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
        @Parameter(description = "用于保证写请求幂等；同一客户端、操作和请求内容可复用同一值，不同请求内容必须更换",
            example = "swagger-event-create-001")
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
    @Operation(summary = "更新灾害事件",
        description = "完整更新事件的可变字段。请求体必须包含 eventType、eventName、startTime 和 status。")
    @ApiResponse(responseCode = "200", description = "事件更新成功")
    ResponseEntity<EventResponse> update(
        @Parameter(description = "事件公开编号，必须使用 EVT_ 开头的接口返回值",
            example = "EVT_01m0hp1as75c71rfyb11py43ra") @PathVariable String eventId,
        @Valid @RequestBody UpdateEventRequest request,
        @Parameter(description = "用于保证写请求幂等；不同请求内容必须使用不同值",
            example = "swagger-event-update-001") @RequestHeader("Idempotency-Key") String key,
        @AuthenticationPrincipal ApiPrincipal principal) {
        IdempotentResult<EventResponse> result = idempotency.execute(principal,
            new IdempotentOperation("PUT:/openapi/v1/disaster-events/{eventId}",
                Map.of("eventId", eventId), Map.of()), key, request, EventResponse.class, () -> {
                    EventResponse body = service.update(eventId, new UpdateEventCommand(
                        request.eventType(), request.eventName(),
                        BeijingTime.requestInstant(request.startTime(), "startTime"),
                        BeijingTime.requestInstant(request.endTime(), "endTime"), request.status()), principal);
                    return new OperationResult<>(200, body, "EVENT", body.eventId());
                });
        return ResponseEntity.status(result.httpStatus()).body(result.body());
    }

    private static CreateEventCommand toCommand(CreateEventRequest r) {
        return new CreateEventCommand(r.externalEventId(), r.sourceSystem(), r.region(), r.eventType(),
            r.eventName(), BeijingTime.requestInstant(r.startTime(), "startTime"),
            BeijingTime.requestInstant(r.endTime(), "endTime"),
            r.status(), observation(r.initialObservation()));
    }

    static EventObservation observation(EventObservationRequest r) {
        if (r == null) return null;
        HazardRequest h = r.hazard() == null ? new HazardRequest(null, null, null, null) : r.hazard();
        ImpactRequest i = r.impact();
        return new EventObservation(r.externalObservationId(),
            BeijingTime.requestInstant(r.observedAt(), "observedAt"),
            h.rainfall24hMm(), h.waterLevelOverWarningM(), h.maxWaterDepthM(), h.affectedAreaKm2(),
            i.affectedPopulation(), i.trappedPopulation(), i.evacuatedPopulation(),
            i.vulnerablePopulation(), i.injuredPopulation(), i.missingPopulation(),
            i.deathPopulation(), i.damagedHouseholds(), i.collapsedHouses(),
            i.roadInterruptions(), i.criticalFacilitiesAffected(), i.powerOutageHouseholds());
    }
}
