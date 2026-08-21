package com.example.flood.material.infrastructure;

import java.math.BigDecimal;

public record MaterialStandardRow(
    long id, long regionScopeId, String materialCode, String materialName, String unit,
    String populationBasis, BigDecimal perPersonPerDay, BigDecimal fixedBaseQuantity,
    BigDecimal levelFactor, BigDecimal reserveRatio, BigDecimal packageSize,
    BigDecimal minimumQuantity
) {}
