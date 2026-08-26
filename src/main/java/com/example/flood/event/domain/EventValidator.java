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
        if (!hasMillisecondPrecision(start) || (end != null && !hasMillisecondPrecision(end)))
            invalid("Event times support at most millisecond precision");
    }

    public void validateObservation(Instant eventStart, EventObservation value) {
        if (value == null || value.observedAt() == null || value.observedAt().isBefore(eventStart))
            invalid("Observation time must not precede event start");
        if (!hasMillisecondPrecision(value.observedAt()))
            invalid("Observation time supports at most millisecond precision");
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

    private static boolean hasMillisecondPrecision(Instant value) {
        return value.getNano() % 1_000_000 == 0;
    }
}
