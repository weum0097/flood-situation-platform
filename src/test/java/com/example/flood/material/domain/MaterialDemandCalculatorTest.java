package com.example.flood.material.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.situation.domain.SituationLevel;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaterialDemandCalculatorTest {
    private final MaterialDemandCalculator calculator = new MaterialDemandCalculator();

    @Test void roundsSupplyHoursUpToWholeDays() {
        assertThat(calculator.supplyDays(new BigDecimal("1"))).isEqualTo(1);
        assertThat(calculator.supplyDays(new BigDecimal("24"))).isEqualTo(1);
        assertThat(calculator.supplyDays(new BigDecimal("25"))).isEqualTo(2);
        assertThat(calculator.supplyDays(new BigDecimal("72"))).isEqualTo(3);
    }

    @Test void appliesFactorsReserveMinimumPackageAndInventory() {
        MaterialStandardDefinition standard = standard("WATER", PopulationBasis.AFFECTED,
            "3", null, "1.5", "0.1", "12", "120");
        MaterialDemandInput input = new MaterialDemandInput(new BigDecimal("24"),
            SituationLevel.HIGH, new PopulationSnapshot(2000, 0, 0, 0), null,
            List.of(new InventoryItem("WATER", "L", new BigDecimal("100"))));

        MaterialDemandLine line = calculator.calculate(input, List.of(standard)).getFirst();

        assertThat(line.grossDemand()).isEqualByComparingTo("9900.0000");
        assertThat(line.netDemand()).isEqualByComparingTo("9800.0000");
    }

    @Test void fixedBasisDoesNotMultiplyDaysAndInventoryCannotMakeNegativeNet() {
        var standard = standard("KIT", PopulationBasis.FIXED, null, "10", "1", "0", "1", "0");
        var input = new MaterialDemandInput(new BigDecimal("72"), SituationLevel.LOW,
            new PopulationSnapshot(0, 0, 0, 0), null,
            List.of(new InventoryItem("KIT", "KIT", new BigDecimal("20"))));
        assertThat(calculator.calculate(input, List.of(standard)).getFirst().netDemand())
            .isEqualByComparingTo("0.0000");
    }

    @Test void rejectsDuplicateInventoryNegativeQuantityAndUnitMismatch() {
        var standard = standard("WATER", PopulationBasis.AFFECTED, "1", null, "1", "0", "1", "0");
        var population = new PopulationSnapshot(1, 0, 0, 0);
        assertError(new MaterialDemandInput(BigDecimal.ONE, SituationLevel.LOW, population, null,
            List.of(new InventoryItem("WATER", "L", BigDecimal.ONE),
                new InventoryItem("WATER", "L", BigDecimal.ONE))), ErrorCode.VALIDATION_ERROR, standard);
        assertError(new MaterialDemandInput(BigDecimal.ONE, SituationLevel.LOW, population, null,
            List.of(new InventoryItem("WATER", "L", new BigDecimal("-1")))), ErrorCode.VALIDATION_ERROR, standard);
        assertError(new MaterialDemandInput(BigDecimal.ONE, SituationLevel.LOW, population, null,
            List.of(new InventoryItem("WATER", "KG", BigDecimal.ONE))), ErrorCode.UNIT_MISMATCH, standard);
    }

    private void assertError(MaterialDemandInput input, ErrorCode code, MaterialStandardDefinition standard) {
        assertThatThrownBy(() -> calculator.calculate(input, List.of(standard)))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(code));
    }
    private MaterialStandardDefinition standard(String code, PopulationBasis basis, String daily,
        String fixed, String factor, String reserve, String pack, String minimum) {
        return new MaterialStandardDefinition(1, 0, code, code, basis == PopulationBasis.FIXED ? "KIT" : "L",
            basis, decimal(daily), decimal(fixed), decimal(factor), decimal(reserve), decimal(pack), decimal(minimum));
    }
    private static BigDecimal decimal(String value) { return value == null ? null : new BigDecimal(value); }
}
