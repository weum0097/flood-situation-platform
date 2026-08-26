package com.example.flood.event.application;

import com.example.flood.event.domain.EventStatus;
import com.example.flood.region.application.RegionSelector;
import java.time.Instant;

public record EventQuery(
    RegionSelector region,
    Instant startTime,
    Instant endTime,
    EventStatus status,
    int page,
    int size
) {
    public long offset() {
        return (long) page * size;
    }
}
