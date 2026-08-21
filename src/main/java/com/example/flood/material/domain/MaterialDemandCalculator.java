package com.example.flood.material.domain;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaterialDemandCalculator {
    private static final int SCALE = 4;

    public int supplyDays(BigDecimal durationHours) {
        if (durationHours == null || durationHours.signum() <= 0)
            throw validation("durationHours must be positive");
        return durationHours.divide(BigDecimal.valueOf(24), 0, RoundingMode.CEILING).intValueExact();
    }

    public List<MaterialDemandLine> calculate(MaterialDemandInput input,
        List<MaterialStandardDefinition> standards) {
        int days = supplyDays(input.durationHours());
        if (input.reserveRatioOverride() != null
            && (input.reserveRatioOverride().signum() < 0
                || input.reserveRatioOverride().compareTo(BigDecimal.ONE) > 0))
            throw validation("reserveRatioOverride must be between 0 and 1");
        Map<String, InventoryItem> inventory = inventory(input.inventory());
        return standards.stream().map(standard -> line(input, standard, days, inventory)).toList();
    }

    private MaterialDemandLine line(MaterialDemandInput input, MaterialStandardDefinition standard,
        int days, Map<String, InventoryItem> inventoryByCode) {
        long basisPopulation = input.population().forBasis(standard.populationBasis());
        BigDecimal raw = standard.populationBasis() == PopulationBasis.FIXED
            ? required(standard.fixedBaseQuantity(), "fixedBaseQuantity")
            : BigDecimal.valueOf(basisPopulation)
                .multiply(required(standard.perPersonPerDay(), "perPersonPerDay"))
                .multiply(BigDecimal.valueOf(days));
        BigDecimal reserve = input.reserveRatioOverride() == null
            ? standard.reserveRatio() : input.reserveRatioOverride();
        BigDecimal adjusted = raw.multiply(required(standard.levelFactor(), "levelFactor"))
            .multiply(BigDecimal.ONE.add(required(reserve, "reserveRatio")));
        BigDecimal packageSize = required(standard.packageSize(), "packageSize");
        if (packageSize.signum() <= 0) throw validation("packageSize must be positive");
        BigDecimal gross = adjusted.max(required(standard.minimumQuantity(), "minimumQuantity"))
            .divide(packageSize, 0, RoundingMode.CEILING).multiply(packageSize);
        InventoryItem stock = inventoryByCode.get(standard.materialCode());
        BigDecimal current = stock == null ? BigDecimal.ZERO : stock.quantity();
        if (stock != null && !standard.unit().equals(stock.unit()))
            throw new ApiException(ErrorCode.UNIT_MISMATCH,
                "Inventory unit does not match material standard for " + standard.materialCode());
        BigDecimal net = gross.subtract(current).max(BigDecimal.ZERO);
        return new MaterialDemandLine(standard.materialCode(), standard.materialName(), standard.unit(),
            standard.populationBasis(), basisPopulation, scale(gross), scale(current), scale(net));
    }

    private Map<String, InventoryItem> inventory(List<InventoryItem> items) {
        Map<String, InventoryItem> result = new HashMap<>();
        for (InventoryItem item : items) {
            if (item.quantity() == null || item.quantity().signum() < 0)
                throw validation("Inventory quantity must not be negative");
            if (result.putIfAbsent(item.materialCode(), item) != null)
                throw validation("Inventory materialCode must be unique");
        }
        return result;
    }
    private static BigDecimal required(BigDecimal value, String field) {
        if (value == null) throw validation(field + " is required by the standard");
        return value;
    }
    private static BigDecimal scale(BigDecimal value) { return value.setScale(SCALE, RoundingMode.UNNECESSARY); }
    private static ApiException validation(String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message);
    }
}
