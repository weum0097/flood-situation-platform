package com.example.flood.event.api;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record HazardRequest(
    @PositiveOrZero BigDecimal rainfall24hMm,
    @PositiveOrZero BigDecimal waterLevelOverWarningM,
    @PositiveOrZero BigDecimal maxWaterDepthM,
    @PositiveOrZero BigDecimal affectedAreaKm2
) {}
