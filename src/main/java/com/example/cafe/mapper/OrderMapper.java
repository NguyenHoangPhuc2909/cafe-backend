package com.example.cafe.mapper;

import com.example.cafe.dto.response.OrderResponse;
import com.example.cafe.entity.*;

import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {
  public static OrderResponse toResponse(Order order, List<OrderItem> orderItemLists) {
    if (order == null) return null;

    List<OrderResponse.OrderItemResponse> itemResponses = orderItemLists.stream()
        .map(item -> OrderResponse.OrderItemResponse.builder()
            .id(item.getId())
            .productName(item.getProduct().getName())
            .quantity(item.getQuantity())
            .price(item.getPrice())
            .toppingNames(item.getToppings() != null
                ? item.getToppings().stream().map(Topping::getName).collect(Collectors.toList()) : null)
            .build()).collect(Collectors.toList());

    TableSession session = order.getTableSession();
    CafeTable table = session != null ? session.getTable() : null;

    return OrderResponse.builder().id(order.getId())
        .sessionId(session != null ? session.getId() : null)
        .tableId(table != null ? table.getId() : null)
        .tableName(table != null ? table.getName() : null)
        .totalAmount(order.getTotalAmount())
        .status(order.getStatus())
        .paymentStatus(order.getPaymentStatus())
        .paymentExpiresAt(order.getPaymentExpiresAt())
        .createdAt(order.getCreatedAt())
        .items(itemResponses).build();
  }
}
