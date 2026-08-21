package com.example.flood.event.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record EventObservationRequest(
    String externalObservationId,
    @NotNull OffsetDateTime observedAt,
    @Valid HazardRequest hazard,
    @Valid @NotNull ImpactRequest impact
) {}
