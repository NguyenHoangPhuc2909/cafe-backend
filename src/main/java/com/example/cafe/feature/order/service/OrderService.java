package com.example.cafe.feature.order.service;

import com.example.cafe.feature.order.dto.request.GuestOrderRequest;
import com.example.cafe.feature.order.dto.request.OrderRequest;
import com.example.cafe.feature.order.dto.response.OrderResponse;


public interface OrderService {
  OrderResponse createOrder(OrderRequest request);
  OrderResponse getOrderById(Long id);
  OrderResponse createGuestOrder(String token, GuestOrderRequest request);
  boolean expireUnpaidOrder(Long orderId);
}
