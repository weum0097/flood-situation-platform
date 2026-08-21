package com.example.flood.situation.api;

import com.example.flood.event.api.EventObservationRequest;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SituationEventImportRequest(
    @NotBlank String externalEventId,
    @NotBlank String sourceSystem,
    @NotNull EventType eventType,
    @NotBlank String eventName,
    @NotNull OffsetDateTime startTime,
    OffsetDateTime endTime,
    @NotNull EventStatus status,
    @Valid @NotNull EventObservationRequest observation
) {}
