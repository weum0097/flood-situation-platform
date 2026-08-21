package com.example.flood.event.api;

import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record UpdateEventRequest(
    @NotNull EventType eventType, @NotBlank String eventName,
    @NotNull OffsetDateTime startTime, OffsetDateTime endTime,
    @NotNull EventStatus status
) {}
