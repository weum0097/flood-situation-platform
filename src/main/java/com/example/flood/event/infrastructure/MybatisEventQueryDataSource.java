package com.example.flood.event.infrastructure;

import com.example.flood.event.api.EventSummaryResponse;
import com.example.flood.event.api.HazardRequest;
import com.example.flood.event.api.ImpactRequest;
import com.example.flood.event.api.ObservationResponse;
import com.example.flood.event.application.EventQuery;
import com.example.flood.event.application.EventQueryDataSource;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.region.application.RegionSelector;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class MybatisEventQueryDataSource implements EventQueryDataSource {
    private final DisasterEventMapper mapper;
    public MybatisEventQueryDataSource(DisasterEventMapper mapper) { this.mapper = mapper; }

    @Override public long count(long regionId, EventQuery query) {
        return mapper.countForQuery(regionId, query);
    }
    @Override public List<EventSummaryResponse> findPage(long regionId, EventQuery query) {
        return mapper.findPageForQuery(regionId, query).stream().map(this::response).toList();
    }
    private EventSummaryResponse response(EventQueryRow row) {
        ObservationResponse observation = row.observationId() == null ? null : new ObservationResponse(
            row.observationId(), row.observedAt().atOffset(ZoneOffset.UTC),
            new HazardRequest(row.rainfall24hMm(), row.waterLevelOverWarningM(),
                row.maxWaterDepthM(), row.affectedAreaKm2()),
            new ImpactRequest(value(row.affectedPopulation()), value(row.trappedPopulation()),
                value(row.evacuatedPopulation()), value(row.vulnerablePopulation()),
                value(row.injuredPopulation()), value(row.missingPopulation()),
                value(row.deathPopulation()), value(row.damagedHouseholds()),
                value(row.collapsedHouses()), intValue(row.roadInterruptions()),
                intValue(row.criticalFacilitiesAffected()), value(row.powerOutageHouseholds())));
        return new EventSummaryResponse(row.eventId(), row.externalEventId(), row.sourceSystem(),
            new RegionSelector(row.regionCode(), row.regionName()), EventType.valueOf(row.eventType()),
            row.eventName(), row.startTime().atOffset(ZoneOffset.UTC),
            row.endTime() == null ? null : row.endTime().atOffset(ZoneOffset.UTC),
            EventStatus.valueOf(row.status()), observation);
    }
    private static long value(Long value) { return value == null ? 0 : value; }
    private static int intValue(Integer value) { return value == null ? 0 : value; }
}
