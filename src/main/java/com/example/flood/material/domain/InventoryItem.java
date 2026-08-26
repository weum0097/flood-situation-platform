package com.example.flood.material.domain;

import java.math.BigDecimal;

public record InventoryItem(String materialCode, String unit, BigDecimal quantity) {}
