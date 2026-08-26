package com.example.flood.event.api;

import java.time.OffsetDateTime;

public record ObservationResponse(
    String observationId,
    OffsetDateTime observedAt,
    HazardRequest hazard,
    ImpactRequest impact
) {}
