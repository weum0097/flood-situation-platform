package com.example.flood.event.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PublicIdGenerator;
import com.example.flood.event.api.EventResponse;
import com.example.flood.event.api.HazardRequest;
import com.example.flood.event.api.ImpactRequest;
import com.example.flood.event.api.ObservationResponse;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventObservation;
import com.example.flood.event.domain.EventType;
import com.example.flood.event.domain.EventValidator;
import com.example.flood.event.infrastructure.DisasterEventMapper;
import com.example.flood.event.infrastructure.DisasterEventRow;
import com.example.flood.event.infrastructure.EventObservationMapper;
import com.example.flood.event.infrastructure.EventObservationRow;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.security.application.ApiPrincipal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultEventApplicationService implements EventApplicationService {
    private final DisasterEventMapper eventMapper;
    private final EventObservationMapper observationMapper;
    private final RegionResolver regionResolver;
    private final EventValidator validator;
    private final PublicIdGenerator ids;
    private final Clock clock;

    public DefaultEventApplicationService(DisasterEventMapper eventMapper,
        EventObservationMapper observationMapper, RegionResolver regionResolver,
        EventValidator validator, PublicIdGenerator ids, Clock clock) {
        this.eventMapper = eventMapper; this.observationMapper = observationMapper;
        this.regionResolver = regionResolver; this.validator = validator; this.ids = ids; this.clock = clock;
    }

    @Override @Transactional
    public EventResponse create(CreateEventCommand command, ApiPrincipal principal) {
        validator.validateTimes(command.status(), command.startTime(), command.endTime());
        if (command.initialObservation() != null)
            validator.validateObservation(command.startTime(), command.initialObservation());
        ResolvedRegion region = regionResolver.resolve(command.region());
        if (eventMapper.countByExternal(command.sourceSystem(), command.externalEventId()) > 0)
            throw conflict();
        String eventId = ids.next("EVT_");
        String observationId = command.initialObservation() == null ? null : ids.next("OBS_");
        try {
            eventMapper.insertEvent(eventId, command.externalEventId(), command.sourceSystem(),
                region.id(), command.eventType().name(), command.eventName(), command.startTime(),
                command.endTime(), command.status().name(), principal.clientId());
            DisasterEventRow saved = eventMapper.findByPublicIdForUpdate(eventId).orElseThrow();
            if (command.initialObservation() != null)
                observationMapper.insertObservation(saved.id(), observationId, command.initialObservation());
        } catch (DuplicateKeyException exception) {
            throw conflict();
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        return new EventResponse(eventId, command.externalEventId(), command.sourceSystem(),
            new RegionSelector(region.regionCode(), region.regionName()), command.eventType(),
            command.eventName(), utc(command.startTime()), utc(command.endTime()), command.status(),
            observationId, now, now);
    }

    @Override @Transactional
    public EventResponse update(String eventId, UpdateEventCommand command, ApiPrincipal principal) {
        validator.validateTimes(command.status(), command.startTime(), command.endTime());
        DisasterEventRow existing = eventMapper.findByPublicIdForUpdate(eventId)
            .orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND, "Event was not found"));
        eventMapper.updateMutable(existing.id(), command.eventType().name(), command.eventName(),
            command.startTime(), command.endTime(), command.status().name());
        OffsetDateTime now = OffsetDateTime.now(clock);
        return new EventResponse(existing.publicId(), existing.externalEventId(), existing.sourceSystem(),
            new RegionSelector(existing.regionCode(), existing.regionName()), command.eventType(),
            command.eventName(), utc(command.startTime()), utc(command.endTime()), command.status(),
            null, utc(existing.createdAt()), now);
    }

    @Override @Transactional
    public ObservationResponse appendObservation(String eventId, AppendObservationCommand command,
        ApiPrincipal principal) {
        DisasterEventRow event = eventMapper.findByPublicIdForUpdate(eventId)
            .orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND, "Event was not found"));
        EventObservation candidate = command.observation();
        validator.validateObservation(event.startTime(), candidate);
        var existing = observationMapper.findByNaturalKey(
            event.id(), candidate.externalObservationId(), candidate.observedAt());
        if (existing.isPresent()) {
            if (!equivalent(existing.get().observation(), candidate)) {
                throw new ApiException(ErrorCode.OBSERVATION_CONFLICT,
                    "Observation natural key already exists with different data");
            }
            return observationResponse(existing.get().publicId(), existing.get().observation());
        }
        String observationId = ids.next("OBS_");
        try {
            observationMapper.insertObservation(event.id(), observationId, candidate);
        } catch (DuplicateKeyException duplicate) {
            EventObservationRow concurrent = observationMapper.findByNaturalKey(
                event.id(), candidate.externalObservationId(), candidate.observedAt())
                .orElseThrow(() -> duplicate);
            if (!equivalent(concurrent.observation(), candidate)) {
                throw new ApiException(ErrorCode.OBSERVATION_CONFLICT,
                    "Observation natural key already exists with different data");
            }
            return observationResponse(concurrent.publicId(), concurrent.observation());
        }
        return observationResponse(observationId, candidate);
    }

    private static ObservationResponse observationResponse(String publicId, EventObservation o) {
        return new ObservationResponse(publicId, utc(o.observedAt()),
            new HazardRequest(o.rainfall24hMm(), o.waterLevelOverWarningM(),
                o.maxWaterDepthM(), o.affectedAreaKm2()),
            new ImpactRequest(o.affectedPopulation(), o.trappedPopulation(),
                o.evacuatedPopulation(), o.vulnerablePopulation(), o.injuredPopulation(),
                o.missingPopulation(), o.deathPopulation(), o.damagedHouseholds(),
                o.collapsedHouses(), o.roadInterruptions(), o.criticalFacilitiesAffected(),
                o.powerOutageHouseholds()));
    }

    private static boolean equivalent(EventObservation left, EventObservation right) {
        return java.util.Objects.equals(left.externalObservationId(), right.externalObservationId())
            && left.observedAt().equals(right.observedAt())
            && decimals(left.rainfall24hMm(), right.rainfall24hMm())
            && decimals(left.waterLevelOverWarningM(), right.waterLevelOverWarningM())
            && decimals(left.maxWaterDepthM(), right.maxWaterDepthM())
            && decimals(left.affectedAreaKm2(), right.affectedAreaKm2())
            && left.affectedPopulation() == right.affectedPopulation()
            && left.trappedPopulation() == right.trappedPopulation()
            && left.evacuatedPopulation() == right.evacuatedPopulation()
            && left.vulnerablePopulation() == right.vulnerablePopulation()
            && left.injuredPopulation() == right.injuredPopulation()
            && left.missingPopulation() == right.missingPopulation()
            && left.deathPopulation() == right.deathPopulation()
            && left.damagedHouseholds() == right.damagedHouseholds()
            && left.collapsedHouses() == right.collapsedHouses()
            && left.roadInterruptions() == right.roadInterruptions()
            && left.criticalFacilitiesAffected() == right.criticalFacilitiesAffected()
            && left.powerOutageHouseholds() == right.powerOutageHouseholds();
    }

    private static boolean decimals(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private static OffsetDateTime utc(java.time.Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
    private static ApiException conflict() {
        return new ApiException(ErrorCode.EVENT_CONFLICT, "An event with this source identity already exists");
    }
}
