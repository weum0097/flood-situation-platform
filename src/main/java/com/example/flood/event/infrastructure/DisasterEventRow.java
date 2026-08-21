package com.example.flood.event.infrastructure;

import java.time.Instant;

public record DisasterEventRow(
    long id, String publicId, String externalEventId, String sourceSystem,
    long regionId, String regionCode, String regionName, String eventType,
    String eventName, Instant startTime, Instant endTime, String status,
    long createdByClientId, Instant createdAt, Instant updatedAt
) {}
