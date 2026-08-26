package com.example.flood.material.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "物资需求计算使用的受灾人口快照")
public record PopulationRequest(
    @Schema(description = "受影响总人数", example = "30000")
    @PositiveOrZero long affectedPopulation,
    @Schema(description = "受困人数", example = "2000") @PositiveOrZero long trappedPopulation,
    @Schema(description = "已转移或疏散人数", example = "100")
    @PositiveOrZero long evacuatedPopulation,
    @Schema(description = "需要重点保障的脆弱人群数", example = "500")
    @PositiveOrZero long vulnerablePopulation
) {}
