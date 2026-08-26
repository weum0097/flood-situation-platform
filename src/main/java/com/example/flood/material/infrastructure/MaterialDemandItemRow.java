package com.example.flood.material.infrastructure;

import java.math.BigDecimal;

public record MaterialDemandItemRow(long calculationId, String materialCode,
    String materialName, String unit, String populationBasis, long basisPopulation,
    BigDecimal grossDemand, BigDecimal currentInventory, BigDecimal netDemand,
    String formulaSnapshot) {}
