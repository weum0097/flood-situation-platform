package com.example.flood.event.application;

import com.example.flood.event.domain.EventObservation;

public record AppendObservationCommand(EventObservation observation) {}
