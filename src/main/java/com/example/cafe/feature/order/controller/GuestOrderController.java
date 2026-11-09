package com.example.cafe.feature.order.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.order.dto.request.CreateOrderRequest;
import com.example.cafe.feature.order.dto.response.OrderResponse;
import com.example.cafe.feature.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guest/orders")
@RequiredArgsConstructor
public class GuestOrderController {
  private final OrderService orderService;

  @PostMapping
  ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestHeader( value="X-Table-Session-Token", required = false)
                                                         String token, @Valid @RequestBody CreateOrderRequest request){
    OrderResponse response = orderService.createGuestOrder(token, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
      @RequestHeader(
          value = "X-Table-Session-Token",
          required = false
      ) String token, @PathVariable("orderId") Long orderId) {
    OrderResponse response = orderService.getGuestOrder(token, orderId);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
      @RequestHeader(value = "X-Table-Session-Token", required = false) String token,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(orderService.getGuestOrders(token, page, size)));
  }
}
