package com.example.flood.event.infrastructure;

import com.example.flood.event.domain.EventObservation;

public record EventObservationRow(long id, long eventId, String publicId, EventObservation observation) {}
