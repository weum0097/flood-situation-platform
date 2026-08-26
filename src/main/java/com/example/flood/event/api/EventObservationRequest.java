package com.example.flood.event.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Schema(description = "某一观测时刻的灾害强度与受灾影响数据")
public record EventObservationRequest(
    @Schema(description = "来源系统中的观测编号；可用于识别重复观测", example = "OBS-EXT-001")
    String externalObservationId,
    @Schema(description = "观测时间，必须使用北京时间偏移 +08:00，不得早于事件开始时间，最多毫秒精度",
        example = "2026-08-21T09:00:00.000+08:00") @NotNull OffsetDateTime observedAt,
    @Schema(description = "灾害强度指标；未知指标可以为空") @Valid HazardRequest hazard,
    @Schema(description = "受灾影响指标；所有数值必须大于或等于 0")
    @Valid @NotNull ImpactRequest impact
) {}
