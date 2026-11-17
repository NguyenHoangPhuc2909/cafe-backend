package com.example.cafe.feature.inventory.dto.response;

public record AdminIngredientResponse(Long id, String name, String unit,
    Double stockQuantity, Double minThreshold, boolean systemManaged, boolean unitEditable, boolean deletable) {}
