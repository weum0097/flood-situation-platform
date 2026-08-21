package com.example.flood.material.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InventoryRequest(
    @NotBlank String materialCode,
    @NotNull @DecimalMin("0") BigDecimal quantity,
    @NotBlank String unit
) {}
