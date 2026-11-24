package com.example.cafe.feature.order.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.order.dto.response.OrderResponse;
import com.example.cafe.feature.order.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
  private final AdminOrderService adminOrderService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getCompletedOrders(
      @RequestParam(name = "fromDate", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate fromDate,
      @RequestParam(name = "toDate", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate toDate,
      @RequestParam(name = "sessionId", required = false) Long sessionId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    PageResponse<OrderResponse> response = adminOrderService.getCompletedOrders(
        fromDate,
        toDate,
        sessionId,
        page,
        size
    );

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }
}
