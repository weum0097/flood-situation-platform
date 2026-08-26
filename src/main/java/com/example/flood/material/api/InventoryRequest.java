package com.example.flood.material.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "当前可用物资库存")
public record InventoryRequest(
    @Schema(description = "业务方物资标准中的物资编码", example = "DRINKING_WATER")
    @NotBlank String materialCode,
    @Schema(description = "当前可用库存数量，必须大于或等于 0", example = "5000")
    @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 4) BigDecimal quantity,
    @Schema(description = "库存单位，必须与物资标准单位一致", example = "L") @NotBlank String unit
) {}
