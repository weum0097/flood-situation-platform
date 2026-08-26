package com.example.flood.event.api;

import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.region.application.RegionSelector;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Schema(description = "灾害事件新增请求")
public record CreateEventRequest(
    @Schema(description = "来源系统中的事件唯一编号；与 sourceSystem 共同保证唯一",
        example = "EXT-FLOOD-20260821-001") @NotBlank String externalEventId,
    @Schema(description = "事件来源系统标识", example = "EMERGENCY_PLATFORM")
    @NotBlank String sourceSystem,
    @Schema(description = "事件所属行政区域") @Valid @NotNull RegionSelector region,
    @Schema(description = "灾害事件类型。可选：RIVER_FLOOD、URBAN_WATERLOGGING、FLASH_FLOOD、"
        + "EMBANKMENT_BREACH、DAM_BREAK、OTHER", example = "RIVER_FLOOD")
    @NotNull EventType eventType,
    @Schema(description = "便于识别的事件名称", example = "浦口区河流洪水")
    @NotBlank String eventName,
    @Schema(description = "事件开始时间，ISO 8601 格式，必须使用北京时间偏移 +08:00，最多毫秒精度",
        example = "2026-08-21T08:00:00.000+08:00") @NotNull OffsetDateTime startTime,
    @Schema(description = "事件结束时间，填写时必须使用北京时间偏移 +08:00。ONGOING 时必须为空；"
        + "ENDED 时必填且不得早于 startTime",
        nullable = true) OffsetDateTime endTime,
    @Schema(description = "事件状态：ONGOING 的 endTime 必须为空；ENDED 的 endTime 必填；"
        + "CANCELLED 的 endTime 可空", example = "ONGOING") @NotNull EventStatus status,
    @Schema(description = "可选的首条灾情观测；提供后将与事件在同一事务中写入")
    @Valid EventObservationRequest initialObservation
) {}
