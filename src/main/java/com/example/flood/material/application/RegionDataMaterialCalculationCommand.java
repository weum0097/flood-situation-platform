package com.example.flood.material.application;

import com.example.flood.material.domain.InventoryItem;
import com.example.flood.region.application.RegionSelector;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RegionDataMaterialCalculationCommand(
    RegionSelector region,
    Instant startTime,
    Instant endTime,
    BigDecimal supplyDurationHours,
    List<InventoryItem> currentInventory
) {
    public RegionDataMaterialCalculationCommand {
        currentInventory = currentInventory == null ? List.of() : List.copyOf(currentInventory);
    }
}
