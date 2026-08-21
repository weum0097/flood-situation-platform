package com.example.flood.event.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;

public record EventQueryRow(
    String eventId, String externalEventId, String sourceSystem,
    String regionCode, String regionName, String eventType, String eventName,
    Instant startTime, Instant endTime, String status,
    String observationId, Instant observedAt,
    BigDecimal rainfall24hMm, BigDecimal waterLevelOverWarningM,
    BigDecimal maxWaterDepthM, BigDecimal affectedAreaKm2,
    Long affectedPopulation, Long trappedPopulation, Long evacuatedPopulation,
    Long vulnerablePopulation, Long injuredPopulation, Long missingPopulation,
    Long deathPopulation, Long damagedHouseholds, Long collapsedHouses,
    Integer roadInterruptions, Integer criticalFacilitiesAffected,
    Long powerOutageHouseholds
) {}
