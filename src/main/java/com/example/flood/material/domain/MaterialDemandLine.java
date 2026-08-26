package com.example.flood.material.domain;

import java.math.BigDecimal;

public record MaterialDemandLine(
    String materialCode, String materialName, String unit, PopulationBasis populationBasis,
    long basisPopulation, BigDecimal grossDemand, BigDecimal currentInventory,
    BigDecimal netDemand
) {}
