package com.example.cafe.feature.order.mapper;

import com.example.cafe.common.entity.catalog.Topping;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.order.OrderItem;
import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.feature.order.dto.response.OrderResponse;


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
