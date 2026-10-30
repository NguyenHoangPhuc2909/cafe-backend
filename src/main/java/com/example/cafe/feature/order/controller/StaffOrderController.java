package com.example.cafe.feature.order.controller;

import com.example.cafe.common.enums.OrderPaymentStatus;
import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.order.dto.response.OrderResponse;
import com.example.cafe.feature.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {
  private final OrderService orderService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "paymentStatus", required = false)
      OrderPaymentStatus paymentStatus,
      @RequestParam(name = "sessionId", required = false) Long sessionId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    PageResponse<OrderResponse> response = orderService.getStaffOrders(
        status,
        paymentStatus,
        sessionId,
        page,
        size
    );

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
      @PathVariable("orderId") Long orderId
  ) {
    OrderResponse response = orderService.getOrderById(orderId);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }
}
