package com.example.cafe.feature.payment.dto.response;

import com.example.cafe.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
    Long id,
    Long orderId,
    PaymentMethod method,
    BigDecimal amount,
    Instant paidAt,
    Long confirmedById
) {
}
