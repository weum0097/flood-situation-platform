package com.example.flood.event.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Schema(description = "灾害强度指标；所有已知数值必须大于或等于 0")
public record HazardRequest(
    @Schema(description = "24 小时累计降雨量，单位毫米", example = "200")
    @PositiveOrZero BigDecimal rainfall24hMm,
    @Schema(description = "超过警戒水位的高度，单位米", example = "0.8")
    @PositiveOrZero BigDecimal waterLevelOverWarningM,
    @Schema(description = "最大积水或淹没深度，单位米", example = "1.2")
    @PositiveOrZero BigDecimal maxWaterDepthM,
    @Schema(description = "受影响面积，单位平方千米", example = "15.5")
    @PositiveOrZero BigDecimal affectedAreaKm2
) {}
