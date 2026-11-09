package com.example.cafe.feature.payment.dto.response;

import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.common.response.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PersonalRevenueResponse(LocalDate date, String timeZone, Instant updatedAt,
    BigDecimal totalAmount, long paymentCount, List<MethodSummary> methods,
    PageResponse<Transaction> payments) {
  public record MethodSummary(PaymentMethod method, BigDecimal amount, long paymentCount) {}
  public record Transaction(Long id, Long orderId, PaymentMethod method, BigDecimal amount, Instant paidAt) {}
}
