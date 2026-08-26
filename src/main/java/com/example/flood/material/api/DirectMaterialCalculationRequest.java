package com.example.flood.material.api;

import com.example.flood.region.application.RegionSelector;
import com.example.flood.situation.domain.SituationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "直接提供态势与人口数据的物资需求计算请求")
public record DirectMaterialCalculationRequest(
    @Schema(description = "物资需求对应的行政区域") @Valid @NotNull RegionSelector region,
    @Schema(description = "区域情景态势等级：LOW、MEDIUM、HIGH", example = "HIGH")
    @NotNull SituationLevel situationLevel,
    @Schema(description = "计划保障时长，单位小时，必须大于 0；系统据此折算保障天数",
        example = "72")
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
        BigDecimal supplyDurationHours,
    @Schema(description = "额外储备比例，范围 0 到 1；省略时使用当前物资标准的默认值",
        example = "0.10", nullable = true)
    @DecimalMin("0") @DecimalMax("1") @Digits(integer = 1, fraction = 6)
        BigDecimal reserveRatio,
    @Schema(description = "用于计算的受灾人口数据")
    @Valid @NotNull PopulationRequest population,
    @Schema(description = "可选的当前库存；同物资同单位库存将从毛需求中扣减")
    @Valid List<InventoryRequest> currentInventory
) {
    public DirectMaterialCalculationRequest {
        currentInventory = currentInventory == null ? List.of() : List.copyOf(currentInventory);
    }
}
