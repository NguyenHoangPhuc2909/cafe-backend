package com.example.cafe.feature.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GuestOrderRequest {
  @NotEmpty(message = "Đơn hàng phải có ít nhất một món")
  @Valid
  private List<@NotNull ItemRequest> items;

  private String note;

  @Data
  public static class ItemRequest {
    @NotNull
    @Min(1)
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;
  }
}
