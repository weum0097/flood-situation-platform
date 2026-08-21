package com.example.flood.material.api;

import com.example.flood.region.application.RegionSelector;
import com.example.flood.situation.domain.SituationLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record DirectMaterialCalculationRequest(
    @Valid @NotNull RegionSelector region,
    @NotNull SituationLevel situationLevel,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal supplyDurationHours,
    @DecimalMin("0") @DecimalMax("1") BigDecimal reserveRatio,
    @Valid @NotNull PopulationRequest population,
    @Valid List<InventoryRequest> currentInventory
) {
    public DirectMaterialCalculationRequest {
        currentInventory = currentInventory == null ? List.of() : List.copyOf(currentInventory);
    }
}
