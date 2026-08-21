package com.example.flood.event.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PublicIdGenerator;
import com.example.flood.event.api.EventResponse;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.event.domain.EventValidator;
import com.example.flood.event.infrastructure.DisasterEventMapper;
import com.example.flood.event.infrastructure.DisasterEventRow;
import com.example.flood.event.infrastructure.EventObservationMapper;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.security.application.ApiPrincipal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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

    private static OffsetDateTime utc(java.time.Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
    private static ApiException conflict() {
        return new ApiException(ErrorCode.EVENT_CONFLICT, "An event with this source identity already exists");
    }
}
