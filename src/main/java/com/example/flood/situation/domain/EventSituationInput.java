package com.example.flood.situation.domain;

import com.example.flood.event.domain.EventType;
import java.math.BigDecimal;
import java.util.Map;

public record EventSituationInput(EventType eventType, Map<MetricCode, BigDecimal> metrics) {
    public EventSituationInput { metrics = Map.copyOf(metrics); }
}
