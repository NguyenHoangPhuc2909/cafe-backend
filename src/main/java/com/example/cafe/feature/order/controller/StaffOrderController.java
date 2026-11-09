package com.example.cafe.feature.order.controller;

import com.example.cafe.common.enums.OrderPaymentStatus;
import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.order.dto.request.StaffOrderRequest;
import com.example.cafe.feature.order.dto.response.OrderResponse;
import com.example.cafe.feature.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

  @PatchMapping("/{orderId}/complete")
  public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(@PathVariable("orderId") Long orderId) {
    OrderResponse response = orderService.completeOrder(orderId);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
      @Valid @RequestBody StaffOrderRequest request,
      Authentication authentication) {

    OrderResponse response = orderService.createStaffOrder(
        authentication.getName(),
        request
    );

    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }
}
