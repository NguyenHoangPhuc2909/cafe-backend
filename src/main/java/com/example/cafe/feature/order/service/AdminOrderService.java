package com.example.cafe.feature.order.service;

import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.order.dto.response.OrderResponse;

import java.time.LocalDate;

public interface AdminOrderService {
  PageResponse<OrderResponse> getCompletedOrders(
      LocalDate fromDate,
      LocalDate toDate,
      Long sessionId,
      int page,
      int size
  );
}
