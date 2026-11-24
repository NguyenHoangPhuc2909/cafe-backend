package com.example.cafe.feature.table.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminTableRequest(
    @NotBlank(message = "Tên bàn không được để trống")
    @Size(max = 20, message = "Tên bàn tối đa 20 ký tự")
    String name
) {}
