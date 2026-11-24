package com.example.cafe.feature.payment.dto.response;

import com.example.cafe.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminRevenueReportResponse(
    LocalDate fromDate,
    LocalDate toDate,
    String timeZone,
    Instant generatedAt,
    BigDecimal totalRevenue,
    long paymentCount,
    BigDecimal averageOrderValue,
    List<MethodBreakdown> methods,
    List<DailyRevenuePoint> daily,
    List<TopProductItem> topProducts
) {
  public record MethodBreakdown(
      PaymentMethod method,
      BigDecimal amount,
      long paymentCount
  ) {}

  public record DailyRevenuePoint(
      LocalDate date,
      BigDecimal revenue,
      BigDecimal cashRevenue,
      BigDecimal bankTransferRevenue,
      long paymentCount
  ) {}

  public record TopProductItem(
      Long productId,
      String productName,
      long quantitySold,
      BigDecimal revenue
  ) {}
}
