package com.example.flood.material.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.region.application.RegionSelector;
import com.example.flood.situation.domain.SituationLevel;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaterialRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsValuesThatCannotBePersistedWithoutRounding() {
        DirectMaterialCalculationRequest request = new DirectMaterialCalculationRequest(
            new RegionSelector("320111", null), SituationLevel.HIGH,
            new BigDecimal("24.0001"), new BigDecimal("0.1234567"),
            new PopulationRequest(10, 0, 0, 0),
            List.of(new InventoryRequest("WATER", new BigDecimal("1.00001"), "L")));

        assertThat(validator.validate(request))
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("supplyDurationHours", "reserveRatio", "currentInventory[0].quantity");
    }
}
