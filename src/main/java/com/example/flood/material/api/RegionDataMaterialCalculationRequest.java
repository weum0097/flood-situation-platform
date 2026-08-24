package com.example.flood.material.api;

import com.example.flood.region.application.RegionSelector;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "使用数据库中区域态势数据的物资需求计算请求")
public record RegionDataMaterialCalculationRequest(
    @Schema(description = "查询态势与计算物资需求的行政区域")
    @Valid @NotNull RegionSelector region,
    @Schema(description = "态势评估记录的查询时间区间")
    @Valid @NotNull AssessmentTimeRange assessmentTimeRange,
    @Schema(description = "计划保障时长，单位小时，必须大于 0", example = "72")
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
        BigDecimal supplyDurationHours,
    @Schema(description = "可选的当前库存；同物资同单位库存将从毛需求中扣减")
    @Valid List<InventoryRequest> currentInventory
) {
    public RegionDataMaterialCalculationRequest {
        currentInventory = currentInventory == null ? List.of() : List.copyOf(currentInventory);
    }

    @Schema(description = "区域态势评估时间范围")
    public record AssessmentTimeRange(
        @Schema(description = "查询窗口开始时间，必须使用北京时间偏移 +08:00",
            example = "2026-08-21T00:00:00.000+08:00") @NotNull OffsetDateTime startTime,
        @Schema(description = "查询窗口结束时间，必须使用北京时间偏移 +08:00，且晚于或等于 startTime",
            example = "2026-08-22T00:00:00.000+08:00") @NotNull OffsetDateTime endTime
    ) {}
}
