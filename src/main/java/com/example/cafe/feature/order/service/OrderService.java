package com.example.cafe.feature.order.service;

import com.example.cafe.common.enums.OrderPaymentStatus;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.order.dto.request.GuestOrderRequest;
import com.example.cafe.feature.order.dto.request.OrderRequest;
import com.example.cafe.feature.order.dto.response.OrderResponse;


public interface OrderService {
  OrderResponse createOrder(OrderRequest request);
  OrderResponse getOrderById(Long id);
  OrderResponse createGuestOrder(String token, GuestOrderRequest request);
  boolean expireUnpaidOrder(Long orderId);

  PageResponse<OrderResponse> getStaffOrders(
      String status,
      OrderPaymentStatus paymentStatus,
      Long sessionId,
      int page,
      int size);

  OrderResponse getGuestOrder(String token, Long orderId);
}
