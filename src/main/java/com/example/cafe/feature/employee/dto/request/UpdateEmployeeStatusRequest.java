package com.example.cafe.feature.employee.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeStatusRequest(
    @NotNull(message = "Trạng thái không được để trống")
    Boolean isActive
) {}