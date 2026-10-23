package com.example.cafe.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.example.cafe.entity.OrderPaymentStatus;
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
  private String status;
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
    private List<String> toppingNames;
  }
}
