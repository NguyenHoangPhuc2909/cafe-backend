package com.example.cafe.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@Data 
public class OrderRequest {
  @NotNull(message = "ID người dùng không được để trống")
  private Long userId;

  @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
  @Valid 
  private List<OrderItemRequest> items;
  private String note;

  @Data 
  public static class OrderItemRequest {
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
    private List<Long> toppingIds;
  }
}
