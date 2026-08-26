package com.example.flood.situation.api;

import com.example.flood.common.api.PageResponse;
import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentOperation;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.common.time.BeijingTime;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "区域情景态势", description = "导入区域灾害事件并计算高中低三级态势，以及查询历史评估结果")
public class SituationController {
    private final RegionSituationAssessmentService service;
    private final IdempotencyExecutor idempotency;

    public SituationController(RegionSituationAssessmentService service,
        IdempotencyExecutor idempotency) {
        this.service = service;
        this.idempotency = idempotency;
    }

    @PostMapping
    @Operation(summary = "计算区域洪水情景态势",
        description = "导入指定区域在评估时刻的灾害事件与观测数据，保存事件记录，并依据当前生效规则聚合区域态势等级。")
    @ApiResponse(responseCode = "201", description = "态势计算并保存成功")
    ResponseEntity<SituationAssessmentResponse> assess(
        @Valid @RequestBody SituationAssessmentRequest request,
        @Parameter(description = "用于保证写请求幂等；不同请求内容必须使用不同值",
            example = "swagger-situation-assess-001")
        @RequestHeader("Idempotency-Key") String key,
        @AuthenticationPrincipal ApiPrincipal principal) {
        IdempotentResult<SituationAssessmentResponse> result = idempotency.execute(principal,
            new IdempotentOperation("POST:/openapi/v1/region-situation-assessments",
                Map.of(), Map.of()), key, request, SituationAssessmentResponse.class, () -> {
                    SituationAssessmentResponse body = service.assess(toCommand(request), principal);
                    return new OperationResult<>(201, body, "ASSESSMENT",
                        body.assessmentId());
                });
        return ResponseEntity.status(result.httpStatus()).body(result.body());
    }

    @GetMapping
    @Operation(summary = "查询区域历史态势",
        description = "按区域、评估时间区间和可选态势等级分页查询已经保存的区域态势结果。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    ResponseEntity<PageResponse<SituationAssessmentSummaryResponse>> search(
        @Parameter(description = "行政区域编码；与 regionName 至少填写一个", example = "320111")
        @RequestParam(required = false) String regionId,
        @Parameter(description = "行政区域名称；仅填写名称时必须能够唯一匹配", example = "浦口区")
        @RequestParam(required = false) String regionName,
        @Parameter(description = "评估时间窗口开始时间，必须使用北京时间偏移 +08:00",
            example = "2026-08-21T00:00:00+08:00")
        @RequestParam OffsetDateTime startTime,
        @Parameter(description = "评估时间窗口结束时间，必须使用北京时间偏移 +08:00，且晚于或等于 startTime",
            example = "2026-08-22T00:00:00+08:00")
        @RequestParam OffsetDateTime endTime,
        @Parameter(description = "可选态势等级：LOW、MEDIUM、HIGH", example = "HIGH")
        @RequestParam(required = false) SituationLevel situationLevel,
        @Parameter(description = "页码，从 0 开始", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "每页数量", example = "20")
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(new SituationAssessmentQuery(
            new RegionSelector(regionId, regionName),
            BeijingTime.requestInstant(startTime, "startTime"),
            BeijingTime.requestInstant(endTime, "endTime"),
            situationLevel, page, size)));
    }

    private static SituationAssessmentCommand toCommand(SituationAssessmentRequest request) {
        List<AssessmentEventImportCommand> events = request.events().stream()
            .map(event -> new AssessmentEventImportCommand(event.externalEventId(),
                event.sourceSystem(), event.eventType(), event.eventName(),
                BeijingTime.requestInstant(event.startTime(), "events[].startTime"),
                BeijingTime.requestInstant(event.endTime(), "events[].endTime"), event.status(),
                observation(event.observation())))
            .toList();
        return new SituationAssessmentCommand(request.region(),
            BeijingTime.requestInstant(request.assessmentTime(), "assessmentTime"), events);
    }

    private static EventObservation observation(EventObservationRequest request) {
        HazardRequest hazard = request.hazard() == null
            ? new HazardRequest(null, null, null, null) : request.hazard();
        ImpactRequest impact = request.impact();
        return new EventObservation(request.externalObservationId(),
            BeijingTime.requestInstant(request.observedAt(), "events[].observation.observedAt"),
            hazard.rainfall24hMm(), hazard.waterLevelOverWarningM(), hazard.maxWaterDepthM(),
            hazard.affectedAreaKm2(), impact.affectedPopulation(), impact.trappedPopulation(),
            impact.evacuatedPopulation(), impact.vulnerablePopulation(), impact.injuredPopulation(),
            impact.missingPopulation(), impact.deathPopulation(), impact.damagedHouseholds(),
            impact.collapsedHouses(), impact.roadInterruptions(),
            impact.criticalFacilitiesAffected(), impact.powerOutageHouseholds());
    }
}
