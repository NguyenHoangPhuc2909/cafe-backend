package com.example.cafe.feature.order.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.order.dto.response.OrderResponse;
import com.example.cafe.feature.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;

  // 2. Khách hoặc Admin xem lại chi tiết tờ bill hóa đơn
  @GetMapping("/{id}")
  public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
    return ApiResponse.ok(orderService.getOrderById(id));
  }
}
