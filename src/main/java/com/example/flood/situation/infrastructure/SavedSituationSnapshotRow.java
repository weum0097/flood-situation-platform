package com.example.flood.situation.infrastructure;

import java.time.Instant;

public record SavedSituationSnapshotRow(long databaseId, String publicId, String level,
    long affectedPopulation, long trappedPopulation, long evacuatedPopulation,
    long vulnerablePopulation, Instant assessmentTime) {}
