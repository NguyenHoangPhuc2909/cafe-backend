package com.example.cafe.feature.order.dto.response;

import com.example.cafe.common.enums.OrderPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.enums.DrinkSize;
import com.example.cafe.common.enums.DrinkOptionLevel;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
  private Long id;
  private Long sessionId;
  private Long tableId;
  private String tableName;
  private BigDecimal totalAmount;
  private OrderStatus status;
  private LocalDateTime createdAt;
  private List<OrderItemResponse> items;
  private OrderPaymentStatus paymentStatus;
  private Instant paymentExpiresAt;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class OrderItemResponse {
    private Long id;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private DrinkSize size;
    private DrinkOptionLevel iceLevel;
    private DrinkOptionLevel sweetnessLevel;
  }
}
