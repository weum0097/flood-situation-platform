package com.example.flood.event.api;

import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.region.application.RegionSelector;
import java.time.OffsetDateTime;

public record EventResponse(
    String eventId, String externalEventId, String sourceSystem, RegionSelector region,
    EventType eventType, String eventName, OffsetDateTime startTime, OffsetDateTime endTime,
    EventStatus status, String observationId, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {}
