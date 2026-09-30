package com.example.cafe.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
  private Long id;
  private Long userId;
  private BigDecimal totalAmount;
  private String status;
  private LocalDateTime createdAt;
  private List<OrderItemResponse> items;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class OrderItemResponse {
    private Long id;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private List<String> toppingNames;
  }
}
