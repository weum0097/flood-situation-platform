package com.example.flood.event.api;

import jakarta.validation.constraints.PositiveOrZero;

public record ImpactRequest(
    @PositiveOrZero long affectedPopulation, @PositiveOrZero long trappedPopulation,
    @PositiveOrZero long evacuatedPopulation, @PositiveOrZero long vulnerablePopulation,
    @PositiveOrZero long injuredPopulation, @PositiveOrZero long missingPopulation,
    @PositiveOrZero long deathPopulation, @PositiveOrZero long damagedHouseholds,
    @PositiveOrZero long collapsedHouses, @PositiveOrZero int roadInterruptions,
    @PositiveOrZero int criticalFacilitiesAffected, @PositiveOrZero long powerOutageHouseholds
) {}
