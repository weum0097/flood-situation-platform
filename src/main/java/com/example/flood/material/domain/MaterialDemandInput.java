package com.example.flood.material.domain;

import com.example.flood.situation.domain.SituationLevel;
import java.math.BigDecimal;
import java.util.List;

public record MaterialDemandInput(
    BigDecimal durationHours, SituationLevel situationLevel, PopulationSnapshot population,
    BigDecimal reserveRatioOverride, List<InventoryItem> inventory
) {
    public MaterialDemandInput { inventory = inventory == null ? List.of() : List.copyOf(inventory); }
}
