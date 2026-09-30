package com.example.cafe.controller;

import com.example.cafe.dto.request.OrderRequest;
import com.example.cafe.dto.response.ApiResponse;
import com.example.cafe.dto.response.OrderResponse;
import com.example.cafe.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;

  // 1. Khách hàng bấm nút đặt hàng
  @PostMapping
  public ApiResponse<OrderResponse> createOrder(@RequestBody @Valid OrderRequest request){
    return ApiResponse.ok(orderService.createOrder(request));
  }

  // 2. Khách hoặc Admin xem lại chi tiết tờ bill hóa đơn
  @GetMapping("/{id}")
  public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
    return ApiResponse.ok(orderService.getOrderById(id));
  }
}
