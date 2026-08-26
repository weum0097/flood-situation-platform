package com.example.flood.situation.application;

import java.time.Instant;
import java.util.Optional;

public interface SituationSnapshotLookup {
    Optional<SavedSituationSnapshot> findLatest(
        long regionDatabaseId, Instant startInclusive, Instant endExclusive);
}
