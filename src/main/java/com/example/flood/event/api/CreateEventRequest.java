package com.example.flood.event.api;

import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.region.application.RegionSelector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateEventRequest(
    @NotBlank String externalEventId, @NotBlank String sourceSystem,
    @Valid @NotNull RegionSelector region, @NotNull EventType eventType,
    @NotBlank String eventName, @NotNull OffsetDateTime startTime,
    OffsetDateTime endTime, @NotNull EventStatus status,
    @Valid EventObservationRequest initialObservation
) {}
