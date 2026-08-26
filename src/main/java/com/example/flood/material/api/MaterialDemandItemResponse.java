package com.example.flood.material.api;

import com.example.flood.material.domain.PopulationBasis;
import java.math.BigDecimal;

public record MaterialDemandItemResponse(
    String materialCode,
    String materialName,
    PopulationBasis populationBasis,
    long basisPopulation,
    BigDecimal grossDemand,
    BigDecimal currentInventory,
    BigDecimal netDemand,
    String unit
) {}
