package com.example.cafe.mapper;

import com.example.cafe.dto.response.OrderResponse;
import com.example.cafe.entity.Order;
import com.example.cafe.entity.OrderItem;
import com.example.cafe.entity.Topping;

import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {
  public static OrderResponse toResponse(Order order, List<OrderItem> orderItemLists){
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

    return OrderResponse.builder().id(order.getId())
        .userId(order.getUser().getId())
        .totalAmount(order.getTotalAmount())
        .status(order.getStatus())
        .createdAt(order.getCreatedAt())
        .items(itemResponses).build();
  }
}
