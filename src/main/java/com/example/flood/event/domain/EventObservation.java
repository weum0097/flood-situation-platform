package com.example.flood.event.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record EventObservation(
    String externalObservationId, Instant observedAt,
    BigDecimal rainfall24hMm, BigDecimal waterLevelOverWarningM,
    BigDecimal maxWaterDepthM, BigDecimal affectedAreaKm2,
    long affectedPopulation, long trappedPopulation, long evacuatedPopulation,
    long vulnerablePopulation, long injuredPopulation, long missingPopulation,
    long deathPopulation, long damagedHouseholds, long collapsedHouses,
    int roadInterruptions, int criticalFacilitiesAffected, long powerOutageHouseholds
) {}
