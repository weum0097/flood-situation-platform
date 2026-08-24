package com.example.flood.event.api;

import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Schema(description = "灾害事件完整更新请求；未提供局部更新语义")
public record UpdateEventRequest(
    @Schema(description = "灾害事件类型。可选：RIVER_FLOOD、URBAN_WATERLOGGING、FLASH_FLOOD、"
        + "EMBANKMENT_BREACH、DAM_BREAK、OTHER", example = "RIVER_FLOOD")
    @NotNull EventType eventType,
    @Schema(description = "事件名称", example = "浦口区河流洪水") @NotBlank String eventName,
    @Schema(description = "事件开始时间，必须使用北京时间偏移 +08:00，最多毫秒精度",
        example = "2026-08-21T08:00:00.000+08:00") @NotNull OffsetDateTime startTime,
    @Schema(description = "事件结束时间，填写时必须使用北京时间偏移 +08:00。ONGOING 时必须为空；"
        + "ENDED 时必填且不得早于 startTime",
        nullable = true) OffsetDateTime endTime,
    @Schema(description = "目标状态：ONGOING 的 endTime 必须为空；ENDED 的 endTime 必填；"
        + "CANCELLED 的 endTime 可空", example = "ONGOING") @NotNull EventStatus status
) {}
