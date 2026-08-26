package com.example.flood.material.infrastructure;

import java.time.Instant;

public record MaterialStandardSetRow(long id, String version, String status,
    Instant effectiveFrom, Instant effectiveTo) {}
