package com.example.flood.event.application;

import com.example.flood.event.domain.EventObservation;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import java.time.Instant;

public record ImportedAssessmentEvent(
    long eventDatabaseId,
    long observationDatabaseId,
    String eventId,
    String externalEventId,
    String observationId,
    EventType eventType,
    EventStatus status,
    Instant startTime,
    Instant endTime,
    EventObservation observation,
    boolean eventCreated,
    boolean eventUpdated,
    boolean observationCreated
) {
    public ImportedAssessmentEvent withStatus(EventStatus replacement) {
        return new ImportedAssessmentEvent(eventDatabaseId, observationDatabaseId, eventId,
            externalEventId, observationId, eventType, replacement, startTime, endTime,
            observation, eventCreated, eventUpdated, observationCreated);
    }
}
