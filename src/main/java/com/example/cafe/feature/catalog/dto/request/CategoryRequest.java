package com.example.cafe.feature.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Nhập tên danh mục") @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự") String name) {}
