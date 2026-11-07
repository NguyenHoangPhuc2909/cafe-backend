package com.example.cafe.feature.inventory.dto.response;

public record PackagedStockResponse(
    Long id,
    String name,
    Integer stockQuantity,
    Boolean isActive
) {}