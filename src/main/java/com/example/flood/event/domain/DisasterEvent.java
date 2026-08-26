package com.example.flood.event.domain;

import java.time.Instant;

public record DisasterEvent(
    long id, String publicId, String externalEventId, String sourceSystem,
    long regionId, EventType eventType, String eventName, Instant startTime,
    Instant endTime, EventStatus status, long createdByClientId,
    Instant createdAt, Instant updatedAt
) {}
