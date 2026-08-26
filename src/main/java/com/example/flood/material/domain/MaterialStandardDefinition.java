package com.example.flood.material.domain;

import java.math.BigDecimal;

public record MaterialStandardDefinition(
    long id, long regionScopeId, String materialCode, String materialName, String unit,
    PopulationBasis populationBasis, BigDecimal perPersonPerDay,
    BigDecimal fixedBaseQuantity, BigDecimal levelFactor, BigDecimal reserveRatio,
    BigDecimal packageSize, BigDecimal minimumQuantity
) {}
