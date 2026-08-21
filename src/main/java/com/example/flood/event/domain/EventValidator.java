package com.example.flood.event.domain;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class EventValidator {
    public void validateTimes(EventStatus status, Instant start, Instant end) {
        if (start == null || status == null
            || (status == EventStatus.ONGOING && end != null)
            || (status == EventStatus.ENDED && end == null)
            || (end != null && end.isBefore(start))) invalid("Invalid event time/status combination");
    }

    public void validateObservation(Instant eventStart, EventObservation value) {
        if (value == null || value.observedAt() == null || value.observedAt().isBefore(eventStart))
            invalid("Observation time must not precede event start");
        if (Stream.of(value.rainfall24hMm(), value.waterLevelOverWarningM(),
                value.maxWaterDepthM(), value.affectedAreaKm2())
            .filter(java.util.Objects::nonNull).anyMatch(number -> number.signum() < 0))
            invalid("Hazard metrics must not be negative");
        long[] counts = { value.affectedPopulation(), value.trappedPopulation(),
            value.evacuatedPopulation(), value.vulnerablePopulation(), value.injuredPopulation(),
            value.missingPopulation(), value.deathPopulation(), value.damagedHouseholds(),
            value.collapsedHouses(), value.roadInterruptions(), value.criticalFacilitiesAffected(),
            value.powerOutageHouseholds() };
        for (long count : counts) if (count < 0) invalid("Impact metrics must not be negative");
        if (value.trappedPopulation() > value.affectedPopulation()
            || value.evacuatedPopulation() > value.affectedPopulation()
            || value.vulnerablePopulation() > value.affectedPopulation())
            invalid("Affected population must cover each impact subgroup");
    }

    private static void invalid(String message) {
        throw new ApiException(ErrorCode.VALIDATION_ERROR, message);
    }
}
