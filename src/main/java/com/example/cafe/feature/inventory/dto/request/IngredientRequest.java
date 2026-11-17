package com.example.cafe.feature.inventory.dto.request;

import jakarta.validation.constraints.*;

public record IngredientRequest(
    @NotBlank(message = "Nhập tên nguyên liệu") @Size(max = 255) String name,
    @NotBlank(message = "Nhập đơn vị tính") @Size(max = 50) String unit,
    @NotNull(message = "Nhập ngưỡng cảnh báo")
    @DecimalMin(value = "0", message = "Ngưỡng cảnh báo không được âm") Double minThreshold) {}
