package com.example.cafe.controller;

import com.example.cafe.dto.request.GuestOrderRequest;
import com.example.cafe.dto.response.ApiResponse;
import com.example.cafe.dto.response.OrderResponse;
import com.example.cafe.service.OrderService;
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
                                                         String token, @Valid @RequestBody GuestOrderRequest request){
    OrderResponse response = orderService.createGuestOrder(token, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }
}
