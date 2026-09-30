package com.example.cafe.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
  @NotBlank(message = "Tên sản phẩm không được để trống")
  private String name;

  @NotNull(message = "Giá sản phẩm không được để trống")
  @Min(value = 0, message = "Giá sản phẩm không hợp lệ (phải lớn hơn hoặc bằng 0)")
  private BigDecimal price;
  private String description;
  private String imageUrl;

  @NotNull(message = "Sản phẩm phải thuộc về một danh mục (categoryId)")
  private Long categoryId;
}
