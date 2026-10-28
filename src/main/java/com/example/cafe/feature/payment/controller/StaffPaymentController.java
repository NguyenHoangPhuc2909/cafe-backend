package com.example.cafe.feature.payment.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.payment.dto.request.ConfirmPaymentRequest;
import com.example.cafe.feature.payment.dto.response.PaymentResponse;
import com.example.cafe.feature.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/orders")
@RequiredArgsConstructor
public class StaffPaymentController {
  private final PaymentService paymentService;

  @PostMapping("/{orderId}/payments")
  public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
      @PathVariable("orderId") Long orderId,
      @Valid @RequestBody ConfirmPaymentRequest request, Authentication authentication) {
    PaymentResponse response = paymentService.confirmPayment(orderId, request.getMethod(), authentication.getName());

    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }
}
