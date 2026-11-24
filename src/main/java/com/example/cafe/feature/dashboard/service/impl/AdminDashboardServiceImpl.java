package com.example.cafe.feature.dashboard.service.impl;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.common.enums.ProductType;
import com.example.cafe.common.enums.TableSessionStatus;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.dashboard.dto.response.AdminDashboardResponse;
import com.example.cafe.feature.dashboard.dto.response.AdminDashboardResponse.DailyTrendPoint;
import com.example.cafe.feature.dashboard.dto.response.AdminDashboardResponse.LowStockAlertItem;
import com.example.cafe.feature.dashboard.dto.response.AdminDashboardResponse.LowStockSummary;
import com.example.cafe.feature.dashboard.dto.response.AdminDashboardResponse.TableSummary;
import com.example.cafe.feature.dashboard.dto.response.AdminDashboardResponse.TodayOrderSummary;
import com.example.cafe.feature.dashboard.dto.response.AdminDashboardResponse.TodayRevenueSummary;
import com.example.cafe.feature.dashboard.service.AdminDashboardService;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.feature.table.repository.CafeTableRepository;
import com.example.cafe.feature.table.repository.TableSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final int RECENT_DAYS = 7;

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final CafeTableRepository cafeTableRepository;
  private final TableSessionRepository tableSessionRepository;
  private final IngredientRepository ingredientRepository;
  private final ProductRepository productRepository;
  private final Clock clock;

  @Autowired
  public AdminDashboardServiceImpl(
      PaymentRepository paymentRepository,
      OrderRepository orderRepository,
      CafeTableRepository cafeTableRepository,
      TableSessionRepository tableSessionRepository,
      IngredientRepository ingredientRepository,
      ProductRepository productRepository
  ) {
    this(
        paymentRepository,
        orderRepository,
        cafeTableRepository,
        tableSessionRepository,
        ingredientRepository,
        productRepository,
        Clock.systemUTC()
    );
  }

  public AdminDashboardServiceImpl(
      PaymentRepository paymentRepository,
      OrderRepository orderRepository,
      CafeTableRepository cafeTableRepository,
      TableSessionRepository tableSessionRepository,
      IngredientRepository ingredientRepository,
      ProductRepository productRepository,
      Clock clock
  ) {
    this.paymentRepository = paymentRepository;
    this.orderRepository = orderRepository;
    this.cafeTableRepository = cafeTableRepository;
    this.tableSessionRepository = tableSessionRepository;
    this.ingredientRepository = ingredientRepository;
    this.productRepository = productRepository;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public AdminDashboardResponse getSummary() {
    Instant now = clock.instant();
    LocalDate today = now.atZone(ZONE).toLocalDate();
    LocalDate trendStart = today.minusDays(RECENT_DAYS - 1L);

    Instant trendFromInstant = trendStart.atStartOfDay(ZONE).toInstant();
    Instant tomorrowInstant = today.plusDays(1).atStartOfDay(ZONE).toInstant();

    // 1. Doanh thu hôm nay + xu hướng 7 ngày từ đơn COMPLETED & PAID theo paidAt
    var payments = paymentRepository.findCompletedPaymentsInRange(trendFromInstant, tomorrowInstant);
    Map<LocalDate, TrendAccumulator> trendMap = new LinkedHashMap<>();
    for (LocalDate d = trendStart; !d.isAfter(today); d = d.plusDays(1)) {
      trendMap.put(d, new TrendAccumulator());
    }

    BigDecimal todayTotal = BigDecimal.ZERO;
    BigDecimal todayCash = BigDecimal.ZERO;
    BigDecimal todayBank = BigDecimal.ZERO;
    long todayPaidCount = 0;

    for (var slice : payments) {
      if (slice.getAmount() == null || slice.getMethod() == null || slice.getPaidAt() == null) {
        continue;
      }
      LocalDate paidDate = slice.getPaidAt().atZone(ZONE).toLocalDate();
      BigDecimal amount = slice.getAmount();

      TrendAccumulator acc = trendMap.get(paidDate);
      if (acc != null) {
        acc.add(amount);
      }

      if (paidDate.equals(today)) {
        todayTotal = todayTotal.add(amount);
        todayPaidCount++;
        if (slice.getMethod() == PaymentMethod.CASH) {
          todayCash = todayCash.add(amount);
        } else if (slice.getMethod() == PaymentMethod.BANK_TRANSFER) {
          todayBank = todayBank.add(amount);
        }
      }
    }

    TodayRevenueSummary todayRevenue = new TodayRevenueSummary(
        todayTotal,
        todayPaidCount,
        todayCash,
        todayBank
    );

    List<DailyTrendPoint> recentRevenue = trendMap.entrySet().stream()
        .map(entry -> new DailyTrendPoint(entry.getKey(), entry.getValue().revenue, entry.getValue().count))
        .toList();

    // 2. Đơn tạo hôm nay theo trạng thái (createdAt)
    LocalDateTime todayStart = today.atStartOfDay();
    LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
    Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
    for (var row : orderRepository.countByStatusInRange(todayStart, tomorrowStart)) {
      if (row.getStatus() != null) {
        statusCounts.put(row.getStatus(), row.getOrderCount());
      }
    }
    long pending = statusCounts.getOrDefault(OrderStatus.PENDING, 0L);
    long preparing = statusCounts.getOrDefault(OrderStatus.PREPARING, 0L);
    long completed = statusCounts.getOrDefault(OrderStatus.COMPLETED, 0L);
    long expired = statusCounts.getOrDefault(OrderStatus.EXPIRED, 0L);
    long totalOrders = pending + preparing + completed + expired;
    TodayOrderSummary todayOrders = new TodayOrderSummary(
        totalOrders, pending, preparing, completed, expired
    );

    // 3. Tình hình bàn hiện tại
    List<CafeTable> allTables = cafeTableRepository.findAll();
    Set<Long> openTableIds = new HashSet<>();
    for (TableSession session : tableSessionRepository.findByStatus(TableSessionStatus.OPEN)) {
      if (session.getTable() != null && session.getTable().getId() != null) {
        openTableIds.add(session.getTable().getId());
      }
    }
    long totalTables = allTables.size();
    long activeTotal = 0;
    long openCount = 0;
    long inactiveCount = 0;
    for (CafeTable table : allTables) {
      if (Boolean.TRUE.equals(table.getIsActive())) {
        activeTotal++;
        if (openTableIds.contains(table.getId())) {
          openCount++;
        }
      } else {
        inactiveCount++;
      }
    }
    long idleCount = Math.max(0, activeTotal - openCount);
    TableSummary tables = new TableSummary(totalTables, activeTotal, openCount, idleCount, inactiveCount);

    // 4. Cảnh báo kho (nguyên liệu <= minThreshold + hàng đóng gói <= 0)
    List<Ingredient> lowIngredients = ingredientRepository.findLowStockIngredients();
    List<Product> packagedProducts = productRepository.findByProductTypeOrderByIdAsc(ProductType.PACKAGED);

    long lowIngredientCount = 0;
    long outOfStockIngredientCount = 0;
    long outOfStockPackagedCount = 0;
    List<LowStockAlertItem> alertItems = new ArrayList<>();

    for (Ingredient ingredient : lowIngredients) {
      double qty = ingredient.getStockQuantity() == null ? 0.0 : ingredient.getStockQuantity();
      double threshold = ingredient.getMinThreshold() == null ? 0.0 : ingredient.getMinThreshold();
      boolean out = qty <= 0.0;
      if (out) {
        outOfStockIngredientCount++;
      } else {
        lowIngredientCount++;
      }
      alertItems.add(new LowStockAlertItem(
          "INGREDIENT",
          ingredient.getId(),
          ingredient.getName(),
          ingredient.getUnit(),
          qty,
          threshold,
          out
      ));
    }

    for (Product product : packagedProducts) {
      int qty = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
      if (qty <= 0) {
        outOfStockPackagedCount++;
        alertItems.add(new LowStockAlertItem(
            "PACKAGED",
            product.getId(),
            product.getName(),
            product.getUnit(),
            qty,
            null,
            true
        ));
      }
    }

    alertItems.sort(Comparator
        .comparing(LowStockAlertItem::outOfStock).reversed()
        .thenComparingDouble(LowStockAlertItem::stockQuantity)
        .thenComparing(LowStockAlertItem::name, String.CASE_INSENSITIVE_ORDER));

    LowStockSummary lowStock = new LowStockSummary(
        lowIngredientCount,
        outOfStockIngredientCount,
        outOfStockPackagedCount,
        alertItems.size(),
        alertItems
    );

    return new AdminDashboardResponse(
        today,
        ZONE.getId(),
        now,
        todayRevenue,
        todayOrders,
        tables,
        lowStock,
        recentRevenue
    );
  }

  private static final class TrendAccumulator {
    private BigDecimal revenue = BigDecimal.ZERO;
    private long count = 0;

    void add(BigDecimal amount) {
      revenue = revenue.add(amount);
      count++;
    }
  }
}
