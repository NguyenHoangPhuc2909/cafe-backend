package com.example.cafe.feature.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductActiveRequest {

  @NotNull(message = "Trạng thái active không được để trống")
  private Boolean active;
}