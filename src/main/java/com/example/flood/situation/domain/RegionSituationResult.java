package com.example.flood.situation.domain;

import java.util.List;

public record RegionSituationResult(SituationLevel level, List<EventSituationResult> events) {
    public RegionSituationResult { events = List.copyOf(events); }
}
