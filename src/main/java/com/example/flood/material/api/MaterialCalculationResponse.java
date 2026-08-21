package com.example.flood.material.api;

import com.example.flood.region.application.RegionSelector;
import com.example.flood.situation.domain.SituationLevel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MaterialCalculationResponse(
    String calculationId,
    String sourceType,
    String assessmentId,
    RegionSelector region,
    SituationLevel situationLevel,
    BigDecimal supplyDurationHours,
    int supplyDays,
    List<MaterialDemandItemResponse> items,
    String standardVersion,
    List<String> warnings,
    OffsetDateTime createdAt
) {
    public MaterialCalculationResponse {
        items = List.copyOf(items);
        warnings = List.copyOf(warnings);
    }
}
