package com.example.flood.event.infrastructure;

import com.example.flood.event.domain.EventObservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.Instant;
import java.util.Optional;

@Mapper
public interface EventObservationMapper {
    Optional<EventObservationDatabaseRow> findNaturalKeyRow(
        @Param("eventId") long eventId,
        @Param("externalObservationId") String externalObservationId,
        @Param("observedAt") Instant observedAt);

    default Optional<EventObservationRow> findByNaturalKey(long eventId,
        String externalObservationId, Instant observedAt) {
        return findNaturalKeyRow(eventId, externalObservationId, observedAt)
            .map(EventObservationDatabaseRow::toRow);
    }

    @Insert("""
        INSERT INTO disaster_event_observation
          (public_id, event_id, external_observation_id, observed_at, rainfall_24h_mm,
           water_level_over_warning_m, max_water_depth_m, affected_area_km2,
           affected_population, trapped_population, evacuated_population,
           vulnerable_population, injured_population, missing_population, death_population,
           damaged_households, collapsed_houses, road_interruptions,
           critical_facilities_affected, power_outage_households)
        VALUES (#{publicId}, #{eventId}, #{o.externalObservationId}, #{o.observedAt},
          #{o.rainfall24hMm}, #{o.waterLevelOverWarningM}, #{o.maxWaterDepthM},
          #{o.affectedAreaKm2}, #{o.affectedPopulation}, #{o.trappedPopulation},
          #{o.evacuatedPopulation}, #{o.vulnerablePopulation}, #{o.injuredPopulation},
          #{o.missingPopulation}, #{o.deathPopulation}, #{o.damagedHouseholds},
          #{o.collapsedHouses}, #{o.roadInterruptions}, #{o.criticalFacilitiesAffected},
          #{o.powerOutageHouseholds})
        """)
    int insertObservation(@Param("eventId") long eventId, @Param("publicId") String publicId,
        @Param("o") EventObservation observation);
}
