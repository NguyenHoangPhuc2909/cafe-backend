package com.example.cafe.service;

import com.example.cafe.dto.request.OrderRequest;
import com.example.cafe.dto.response.OrderResponse;

public interface OrderService {
  OrderResponse createOrder(OrderRequest request);
  OrderResponse getOrderById(Long id);
}
