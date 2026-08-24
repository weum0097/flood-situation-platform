package com.example.flood.situation.api;

import com.example.flood.event.api.EventObservationRequest;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Schema(description = "参与区域态势计算并导入数据库的灾害事件")
public record SituationEventImportRequest(
    @Schema(description = "来源系统中的事件唯一编号", example = "EXT-FLOOD-20260821-001")
    @NotBlank String externalEventId,
    @Schema(description = "事件来源系统标识", example = "EMERGENCY_PLATFORM")
    @NotBlank String sourceSystem,
    @Schema(description = "灾害事件类型", example = "RIVER_FLOOD") @NotNull EventType eventType,
    @Schema(description = "事件名称", example = "浦口区河流洪水") @NotBlank String eventName,
    @Schema(description = "事件开始时间，必须使用北京时间偏移 +08:00，最多毫秒精度",
        example = "2026-08-21T08:00:00.000+08:00") @NotNull OffsetDateTime startTime,
    @Schema(description = "事件结束时间，填写时必须使用北京时间偏移 +08:00。ONGOING 时必须为空；ENDED 时必填",
        nullable = true) OffsetDateTime endTime,
    @Schema(description = "事件状态。只有 ONGOING 且时间有效的事件参与本次态势聚合",
        example = "ONGOING") @NotNull EventStatus status,
    @Schema(description = "本次评估使用的灾情观测，observedAt 不得晚于 assessmentTime")
    @Valid @NotNull EventObservationRequest observation
) {}
