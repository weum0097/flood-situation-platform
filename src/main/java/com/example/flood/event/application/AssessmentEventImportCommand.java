package com.example.flood.event.application;

import com.example.flood.event.domain.EventObservation;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import java.time.Instant;

public record AssessmentEventImportCommand(
    String externalEventId,
    String sourceSystem,
    EventType eventType,
    String eventName,
    Instant startTime,
    Instant endTime,
    EventStatus status,
    EventObservation observation
) {}
