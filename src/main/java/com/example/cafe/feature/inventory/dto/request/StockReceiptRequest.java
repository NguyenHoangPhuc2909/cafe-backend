package com.example.cafe.feature.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class StockReceiptRequest {
  @Positive(message = "ID nguyên liệu phải lớn hơn 0")
  private Long ingredientId;

  @Positive(message = "ID sản phẩm phải lớn hơn 0")
  private Long productId;

  @NotNull(message = "Vui lòng nhập số lượng")
  @Positive(message = "Số lượng nhập phải lớn hơn 0")
  private Double quantity;

  @NotBlank(message = "Vui lòng nhập ghi chú nhập kho")
  @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
  private String note;

  @NotNull(message = "Vui lòng cung cấp mã yêu cầu nhập kho")
  private UUID requestId;
}