package com.example.flood.event.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "受灾影响指标。受困、转移和脆弱人群数均不得超过受影响人数")
public record ImpactRequest(
    @Schema(description = "受影响人数", example = "30000") @PositiveOrZero long affectedPopulation,
    @Schema(description = "受困人数", example = "2000") @PositiveOrZero long trappedPopulation,
    @Schema(description = "已转移或疏散人数", example = "100") @PositiveOrZero long evacuatedPopulation,
    @Schema(description = "需要重点保障的脆弱人群数", example = "500")
    @PositiveOrZero long vulnerablePopulation,
    @Schema(description = "受伤人数", example = "10") @PositiveOrZero long injuredPopulation,
    @Schema(description = "失踪人数", example = "0") @PositiveOrZero long missingPopulation,
    @Schema(description = "死亡人数", example = "0") @PositiveOrZero long deathPopulation,
    @Schema(description = "受损户数", example = "200") @PositiveOrZero long damagedHouseholds,
    @Schema(description = "倒塌房屋数", example = "5") @PositiveOrZero long collapsedHouses,
    @Schema(description = "中断道路数量", example = "8") @PositiveOrZero int roadInterruptions,
    @Schema(description = "受影响的重要设施数量", example = "2")
    @PositiveOrZero int criticalFacilitiesAffected,
    @Schema(description = "停电户数", example = "1000") @PositiveOrZero long powerOutageHouseholds
) {}
