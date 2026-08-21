package com.example.flood.situation.application;

import com.example.flood.situation.domain.SituationLevel;
import java.time.Instant;

public record SavedSituationSnapshot(
    long databaseId,
    String publicId,
    SituationLevel level,
    long affectedPopulation,
    long trappedPopulation,
    long evacuatedPopulation,
    long vulnerablePopulation,
    Instant assessmentTime
) {}
