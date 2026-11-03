package com.example.cafe.feature.payment.service;

import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.feature.payment.dto.response.PaymentResponse;

public interface PaymentService {
  PaymentResponse confirmPayment (
      Long orderId,
      PaymentMethod method,
      String username
  );
}
