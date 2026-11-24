package com.example.cafe.feature.table.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateTableStatusRequest(
    @NotNull(message = "Trạng thái bàn không được để trống")
    Boolean isActive
) {}
