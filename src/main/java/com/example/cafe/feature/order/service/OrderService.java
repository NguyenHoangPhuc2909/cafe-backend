package com.example.cafe.feature.order.service;

import com.example.cafe.common.enums.OrderPaymentStatus;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.order.dto.request.CreateOrderRequest;
import com.example.cafe.feature.order.dto.request.StaffOrderRequest;
import com.example.cafe.feature.order.dto.response.OrderResponse;


public interface OrderService {
  OrderResponse getOrderById(Long id);
  OrderResponse createGuestOrder(String token, CreateOrderRequest request);
  boolean expireUnpaidOrder(Long orderId);

  PageResponse<OrderResponse> getStaffOrders(
      String status,
      OrderPaymentStatus paymentStatus,
      Long sessionId,
      int page,
      int size);

  OrderResponse getGuestOrder(String token, Long orderId);

  PageResponse<OrderResponse> getGuestOrders(String token, int page, int size);

  OrderResponse completeOrder(Long orderId);

  OrderResponse createStaffOrder(String username, StaffOrderRequest request);
}
