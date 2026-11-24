package com.example.cafe.feature.dashboard.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
    LocalDate businessDate,
    String timeZone,
    Instant generatedAt,
    TodayRevenueSummary todayRevenue,
    TodayOrderSummary todayOrders,
    TableSummary tables,
    LowStockSummary lowStock,
    List<DailyTrendPoint> recentRevenue
) {
  public record TodayRevenueSummary(
      BigDecimal totalRevenue,
      long completedPaymentCount,
      BigDecimal cashRevenue,
      BigDecimal bankTransferRevenue
  ) {}

  public record TodayOrderSummary(
      long total,
      long pending,
      long preparing,
      long completed,
      long expired
  ) {}

  public record TableSummary(
      long total,
      long activeTotal,
      long openCount,
      long idleCount,
      long inactiveCount
  ) {}

  public record LowStockSummary(
      long lowIngredientCount,
      long outOfStockIngredientCount,
      long outOfStockPackagedCount,
      long totalAlerts,
      List<LowStockAlertItem> items
  ) {}

  public record LowStockAlertItem(
      String itemType,
      Long id,
      String name,
      String unit,
      double stockQuantity,
      Double minThreshold,
      boolean outOfStock
  ) {}

  public record DailyTrendPoint(
      LocalDate date,
      BigDecimal revenue,
      long paymentCount
  ) {}
}
