package com.example.flood.event.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PublicIdGenerator;
import com.example.flood.common.time.BeijingTime;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultEventApplicationService implements EventApplicationService, EventAssessmentImportPort {
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
        OffsetDateTime now = BeijingTime.now(clock);
        return new EventResponse(eventId, command.externalEventId(), command.sourceSystem(),
            new RegionSelector(region.regionCode(), region.regionName()), command.eventType(),
            command.eventName(), beijing(command.startTime()), beijing(command.endTime()), command.status(),
            observationId, now, now);
    }

    @Override @Transactional
    public EventResponse update(String eventId, UpdateEventCommand command, ApiPrincipal principal) {
        validator.validateTimes(command.status(), command.startTime(), command.endTime());
        DisasterEventRow existing = eventMapper.findByPublicIdForUpdate(eventId)
            .orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND, "Event was not found"));
        eventMapper.updateMutable(existing.id(), command.eventType().name(), command.eventName(),
            command.startTime(), command.endTime(), command.status().name());
        OffsetDateTime now = BeijingTime.now(clock);
        return new EventResponse(existing.publicId(), existing.externalEventId(), existing.sourceSystem(),
            new RegionSelector(existing.regionCode(), existing.regionName()), command.eventType(),
            command.eventName(), beijing(command.startTime()), beijing(command.endTime()), command.status(),
            null, beijing(existing.createdAt()), now);
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

    @Override @Transactional
    public List<ImportedAssessmentEvent> importForAssessment(ResolvedRegion rootRegion,
        java.time.Instant assessmentTime, List<AssessmentEventImportCommand> commands,
        ApiPrincipal principal) {
        List<ImportedAssessmentEvent> imported = new ArrayList<>();
        for (AssessmentEventImportCommand command : commands) {
            validator.validateTimes(command.status(), command.startTime(), command.endTime());
            validator.validateObservation(command.startTime(), command.observation());
            DisasterEventRow existing = eventMapper.findByExternalForUpdate(
                command.sourceSystem(), command.externalEventId()).orElse(null);
            boolean created = existing == null;
            boolean updated = false;
            if (created) {
                String eventId = ids.next("EVT_");
                try {
                    eventMapper.insertEvent(eventId, command.externalEventId(), command.sourceSystem(),
                        rootRegion.id(), command.eventType().name(), command.eventName(),
                        command.startTime(), command.endTime(), command.status().name(), principal.clientId());
                } catch (DuplicateKeyException duplicate) {
                    existing = eventMapper.findByExternalForUpdate(
                        command.sourceSystem(), command.externalEventId()).orElseThrow(() -> duplicate);
                    created = false;
                }
                if (created) {
                    existing = eventMapper.findByExternalForUpdate(
                        command.sourceSystem(), command.externalEventId()).orElseThrow();
                }
            }
            ensureStableFields(existing, rootRegion, command);
            if (!created) {
                updated = !Objects.equals(existing.eventName(), command.eventName())
                    || !Objects.equals(existing.endTime(), command.endTime())
                    || !existing.status().equals(command.status().name());
                if (updated) {
                    eventMapper.updateAssessmentMutable(existing.id(), command.eventName(),
                        command.endTime(), command.status().name());
                }
            }
            SavedObservation saved = saveImportedObservation(existing.id(), command.observation());
            imported.add(new ImportedAssessmentEvent(existing.id(), saved.row().id(),
                existing.publicId(), existing.externalEventId(), saved.row().publicId(),
                command.eventType(), command.status(), command.startTime(), command.endTime(),
                command.observation(), created, updated, saved.created()));
        }
        return List.copyOf(imported);
    }

    private SavedObservation saveImportedObservation(long eventId, EventObservation candidate) {
        var existing = observationMapper.findByNaturalKey(
            eventId, candidate.externalObservationId(), candidate.observedAt());
        if (existing.isPresent()) {
            if (!equivalent(existing.get().observation(), candidate)) {
                throw new ApiException(ErrorCode.OBSERVATION_CONFLICT,
                    "Observation natural key already exists with different data");
            }
            return new SavedObservation(existing.get(), false);
        }
        String publicId = ids.next("OBS_");
        try {
            observationMapper.insertObservation(eventId, publicId, candidate);
        } catch (DuplicateKeyException duplicate) {
            EventObservationRow concurrent = observationMapper.findByNaturalKey(
                eventId, candidate.externalObservationId(), candidate.observedAt())
                .orElseThrow(() -> duplicate);
            if (!equivalent(concurrent.observation(), candidate)) {
                throw new ApiException(ErrorCode.OBSERVATION_CONFLICT,
                    "Observation natural key already exists with different data");
            }
            return new SavedObservation(concurrent, false);
        }
        EventObservationRow row = observationMapper.findByNaturalKey(
            eventId, candidate.externalObservationId(), candidate.observedAt())
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                "Inserted observation could not be reloaded"));
        return new SavedObservation(row, true);
    }

    private static void ensureStableFields(DisasterEventRow existing, ResolvedRegion root,
        AssessmentEventImportCommand command) {
        if (existing.regionId() != root.id()
            || !existing.sourceSystem().equals(command.sourceSystem())
            || !existing.externalEventId().equals(command.externalEventId())
            || !existing.eventType().equals(command.eventType().name())
            || !existing.startTime().equals(command.startTime())) {
            throw new ApiException(ErrorCode.EVENT_CONFLICT,
                "Existing event has different stable fields");
        }
    }

    private static ObservationResponse observationResponse(String publicId, EventObservation o) {
        return new ObservationResponse(publicId, beijing(o.observedAt()),
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

    private static OffsetDateTime beijing(java.time.Instant value) {
        return BeijingTime.from(value);
    }
    private static ApiException conflict() {
        return new ApiException(ErrorCode.EVENT_CONFLICT, "An event with this source identity already exists");
    }

    private record SavedObservation(EventObservationRow row, boolean created) {}
}
