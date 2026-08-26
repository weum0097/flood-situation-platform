package com.example.flood.material.application;

import com.example.flood.material.domain.InventoryItem;
import com.example.flood.material.domain.PopulationSnapshot;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.situation.domain.SituationLevel;
import java.math.BigDecimal;
import java.util.List;

public record DirectMaterialCalculationCommand(
    RegionSelector region,
    SituationLevel situationLevel,
    BigDecimal supplyDurationHours,
    BigDecimal reserveRatio,
    PopulationSnapshot population,
    List<InventoryItem> currentInventory
) {
    public DirectMaterialCalculationCommand {
        currentInventory = currentInventory == null ? List.of() : List.copyOf(currentInventory);
    }
}
