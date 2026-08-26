package com.example.flood.event.application;

import com.example.flood.event.domain.EventObservation;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.region.application.RegionSelector;
import java.time.Instant;

public record CreateEventCommand(
    String externalEventId, String sourceSystem, RegionSelector region, EventType eventType,
    String eventName, Instant startTime, Instant endTime, EventStatus status,
    EventObservation initialObservation
) {}
