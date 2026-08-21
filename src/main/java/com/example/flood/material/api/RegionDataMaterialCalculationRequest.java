package com.example.flood.material.api;

import com.example.flood.region.application.RegionSelector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record RegionDataMaterialCalculationRequest(
    @Valid @NotNull RegionSelector region,
    @Valid @NotNull AssessmentTimeRange assessmentTimeRange,
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
        BigDecimal supplyDurationHours,
    @Valid List<InventoryRequest> currentInventory
) {
    public RegionDataMaterialCalculationRequest {
        currentInventory = currentInventory == null ? List.of() : List.copyOf(currentInventory);
    }

    public record AssessmentTimeRange(
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime
    ) {}
}
