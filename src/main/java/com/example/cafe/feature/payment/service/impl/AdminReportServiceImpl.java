package com.example.cafe.feature.payment.service.impl;

import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.order.repository.OrderItemRepository;
import com.example.cafe.feature.payment.dto.response.AdminRevenueReportResponse;
import com.example.cafe.feature.payment.dto.response.AdminRevenueReportResponse.DailyRevenuePoint;
import com.example.cafe.feature.payment.dto.response.AdminRevenueReportResponse.MethodBreakdown;
import com.example.cafe.feature.payment.dto.response.AdminRevenueReportResponse.TopProductItem;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.feature.payment.service.AdminReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportServiceImpl implements AdminReportService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final long MAX_RANGE_DAYS = 365;

  private final PaymentRepository paymentRepository;
  private final OrderItemRepository orderItemRepository;
  private final Clock clock;

  @Autowired
  public AdminReportServiceImpl(
      PaymentRepository paymentRepository,
      OrderItemRepository orderItemRepository
  ) {
    this(paymentRepository, orderItemRepository, Clock.systemUTC());
  }

  public AdminReportServiceImpl(
      PaymentRepository paymentRepository,
      OrderItemRepository orderItemRepository,
      Clock clock
  ) {
    this.paymentRepository = paymentRepository;
    this.orderItemRepository = orderItemRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public AdminRevenueReportResponse getRevenueReport(
      LocalDate fromDate,
      LocalDate toDate,
      int topLimit
  ) {
    Instant now = clock.instant();
    LocalDate today = now.atZone(ZONE).toLocalDate();

    LocalDate resolvedFrom = fromDate != null ? fromDate : (toDate != null ? toDate : today);
    LocalDate resolvedTo = toDate != null ? toDate : (fromDate != null ? fromDate : today);

    if (resolvedFrom.isAfter(resolvedTo)
        || LocalDate.MAX.equals(resolvedTo)
        || ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) > MAX_RANGE_DAYS
        || topLimit < 1
        || topLimit > 50) {
      throw new AppException(ErrorCode.INVALID_REPORT_QUERY);
    }

    Instant fromInstant = resolvedFrom.atStartOfDay(ZONE).toInstant();
    Instant untilExclusive = resolvedTo.plusDays(1).atStartOfDay(ZONE).toInstant();

    var payments = paymentRepository.findCompletedPaymentsInRange(fromInstant, untilExclusive);

    Map<PaymentMethod, BigDecimal> methodAmounts = new EnumMap<>(PaymentMethod.class);
    Map<PaymentMethod, Long> methodCounts = new EnumMap<>(PaymentMethod.class);
    for (PaymentMethod method : PaymentMethod.values()) {
      methodAmounts.put(method, BigDecimal.ZERO);
      methodCounts.put(method, 0L);
    }

    Map<LocalDate, DailyAccumulator> dailyMap = new LinkedHashMap<>();
    for (LocalDate d = resolvedFrom; !d.isAfter(resolvedTo); d = d.plusDays(1)) {
      dailyMap.put(d, new DailyAccumulator());
    }

    BigDecimal totalRevenue = BigDecimal.ZERO;
    long paymentCount = 0;

    for (var slice : payments) {
      if (slice.getAmount() == null || slice.getMethod() == null || slice.getPaidAt() == null) {
        continue;
      }
      BigDecimal amount = slice.getAmount();
      PaymentMethod method = slice.getMethod();
      LocalDate businessDate = slice.getPaidAt().atZone(ZONE).toLocalDate();

      totalRevenue = totalRevenue.add(amount);
      paymentCount++;
      methodAmounts.put(method, methodAmounts.get(method).add(amount));
      methodCounts.put(method, methodCounts.get(method) + 1);

      DailyAccumulator day = dailyMap.get(businessDate);
      if (day != null) {
        day.add(method, amount);
      }
    }

    BigDecimal averageOrderValue = paymentCount == 0
        ? BigDecimal.ZERO
        : totalRevenue.divide(BigDecimal.valueOf(paymentCount), 0, RoundingMode.HALF_UP);

    List<MethodBreakdown> methods = new ArrayList<>();
    for (PaymentMethod method : PaymentMethod.values()) {
      methods.add(new MethodBreakdown(method, methodAmounts.get(method), methodCounts.get(method)));
    }

    List<DailyRevenuePoint> daily = dailyMap.entrySet().stream()
        .map(entry -> entry.getValue().toPoint(entry.getKey()))
        .toList();

    List<TopProductItem> topProducts = orderItemRepository
        .findTopProductsCompletedInRange(fromInstant, untilExclusive, PageRequest.of(0, topLimit))
        .stream()
        .map(item -> new TopProductItem(
            item.getProductId(),
            item.getProductName(),
            item.getQuantitySold() == null ? 0L : item.getQuantitySold(),
            item.getRevenue() == null ? BigDecimal.ZERO : item.getRevenue()
        ))
        .toList();

    return new AdminRevenueReportResponse(
        resolvedFrom,
        resolvedTo,
        ZONE.getId(),
        now,
        totalRevenue,
        paymentCount,
        averageOrderValue,
        methods,
        daily,
        topProducts
    );
  }

  private static final class DailyAccumulator {
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal cashRevenue = BigDecimal.ZERO;
    private BigDecimal bankTransferRevenue = BigDecimal.ZERO;
    private long paymentCount = 0;

    void add(PaymentMethod method, BigDecimal amount) {
      revenue = revenue.add(amount);
      paymentCount++;
      if (method == PaymentMethod.CASH) {
        cashRevenue = cashRevenue.add(amount);
      } else if (method == PaymentMethod.BANK_TRANSFER) {
        bankTransferRevenue = bankTransferRevenue.add(amount);
      }
    }

    DailyRevenuePoint toPoint(LocalDate date) {
      return new DailyRevenuePoint(date, revenue, cashRevenue, bankTransferRevenue, paymentCount);
    }
  }
}
