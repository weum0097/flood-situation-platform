package com.example.flood.material.api;

import jakarta.validation.constraints.PositiveOrZero;

public record PopulationRequest(
    @PositiveOrZero long affectedPopulation,
    @PositiveOrZero long trappedPopulation,
    @PositiveOrZero long evacuatedPopulation,
    @PositiveOrZero long vulnerablePopulation
) {}
