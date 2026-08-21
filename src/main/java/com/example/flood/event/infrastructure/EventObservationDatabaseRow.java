package com.example.flood.event.infrastructure;

import com.example.flood.event.domain.EventObservation;
import java.math.BigDecimal;
import java.time.Instant;

public record EventObservationDatabaseRow(
    long id, long eventId, String publicId, String externalObservationId, Instant observedAt,
    BigDecimal rainfall24hMm, BigDecimal waterLevelOverWarningM, BigDecimal maxWaterDepthM,
    BigDecimal affectedAreaKm2, long affectedPopulation, long trappedPopulation,
    long evacuatedPopulation, long vulnerablePopulation, long injuredPopulation,
    long missingPopulation, long deathPopulation, long damagedHouseholds,
    long collapsedHouses, int roadInterruptions, int criticalFacilitiesAffected,
    long powerOutageHouseholds
) {
    EventObservationRow toRow() {
        return new EventObservationRow(id, eventId, publicId, new EventObservation(
            externalObservationId, observedAt, rainfall24hMm, waterLevelOverWarningM,
            maxWaterDepthM, affectedAreaKm2, affectedPopulation, trappedPopulation,
            evacuatedPopulation, vulnerablePopulation, injuredPopulation, missingPopulation,
            deathPopulation, damagedHouseholds, collapsedHouses, roadInterruptions,
            criticalFacilitiesAffected, powerOutageHouseholds));
    }
}
