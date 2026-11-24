package com.example.cafe.feature.dashboard;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.common.enums.ProductType;
import com.example.cafe.common.enums.TableSessionStatus;
import com.example.cafe.common.exception.GlobalExceptionHandler;
import com.example.cafe.config.SecurityConfig;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.dashboard.controller.AdminDashboardController;
import com.example.cafe.feature.dashboard.service.impl.AdminDashboardServiceImpl;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.feature.table.repository.CafeTableRepository;
import com.example.cafe.feature.table.repository.TableSessionRepository;
import com.example.cafe.security.JwtAuthenticationFilter;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminDashboardTest {
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mvc;
  private Fixture fixture;

  @BeforeEach
  void setup() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(SecurityConfig.class, TestConfig.class);
    context.refresh();
    fixture = context.getBean(Fixture.class);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach
  void close() {
    context.close();
  }

  @Test
  void adminDashboardAggregatesRevenueOrdersTablesLowStockAndSevenDayTrend() throws Exception {
    // Fixed clock: 2026-11-24T05:00:00Z -> 2026-11-24 12:00 VN
    mvc.perform(get("/api/admin/dashboard").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.businessDate").value("2026-11-24"))
        .andExpect(jsonPath("$.data.timeZone").value("Asia/Ho_Chi_Minh"))
        .andExpect(jsonPath("$.data.todayRevenue.totalRevenue").value(125000))
        .andExpect(jsonPath("$.data.todayRevenue.completedPaymentCount").value(2))
        .andExpect(jsonPath("$.data.todayRevenue.cashRevenue").value(55000))
        .andExpect(jsonPath("$.data.todayRevenue.bankTransferRevenue").value(70000))
        .andExpect(jsonPath("$.data.todayOrders.total").value(6))
        .andExpect(jsonPath("$.data.todayOrders.pending").value(1))
        .andExpect(jsonPath("$.data.todayOrders.preparing").value(2))
        .andExpect(jsonPath("$.data.todayOrders.completed").value(2))
        .andExpect(jsonPath("$.data.todayOrders.expired").value(1))
        .andExpect(jsonPath("$.data.tables.total").value(3))
        .andExpect(jsonPath("$.data.tables.activeTotal").value(2))
        .andExpect(jsonPath("$.data.tables.openCount").value(1))
        .andExpect(jsonPath("$.data.tables.idleCount").value(1))
        .andExpect(jsonPath("$.data.tables.inactiveCount").value(1))
        .andExpect(jsonPath("$.data.lowStock.lowIngredientCount").value(1))
        .andExpect(jsonPath("$.data.lowStock.outOfStockIngredientCount").value(1))
        .andExpect(jsonPath("$.data.lowStock.outOfStockPackagedCount").value(1))
        .andExpect(jsonPath("$.data.lowStock.totalAlerts").value(3))
        .andExpect(jsonPath("$.data.lowStock.items.length()").value(3))
        .andExpect(jsonPath("$.data.recentRevenue.length()").value(7))
        .andExpect(jsonPath("$.data.recentRevenue[0].date").value("2026-11-18"))
        .andExpect(jsonPath("$.data.recentRevenue[0].revenue").value(40000))
        .andExpect(jsonPath("$.data.recentRevenue[6].date").value("2026-11-24"))
        .andExpect(jsonPath("$.data.recentRevenue[6].revenue").value(125000));

    ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
    assertEquals(LocalDate.of(2026, 11, 18).atStartOfDay(vn).toInstant(), fixture.capturedPayFrom);
    assertEquals(LocalDate.of(2026, 11, 25).atStartOfDay(vn).toInstant(), fixture.capturedPayUntil);
    assertEquals(LocalDateTime.of(2026, 11, 24, 0, 0), fixture.capturedOrderFrom);
    assertEquals(LocalDateTime.of(2026, 11, 25, 0, 0), fixture.capturedOrderUntil);
  }

  @Test
  void onlyAdminMayAccessDashboardSummary() throws Exception {
    mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    for (String role : List.of("STAFF", "CUSTOMER")) {
      mvc.perform(get("/api/admin/dashboard").with(user("other").roles(role)))
          .andExpect(status().isForbidden());
    }
    assertTrue(fixture.payments.calls.isEmpty());
    assertTrue(fixture.orders.calls.isEmpty());
  }

  private static PaymentRepository.CompletedPaymentSlice slice(
      PaymentMethod method, BigDecimal amount, Instant paidAt) {
    return new PaymentRepository.CompletedPaymentSlice() {
      @Override public PaymentMethod getMethod() { return method; }
      @Override public BigDecimal getAmount() { return amount; }
      @Override public Instant getPaidAt() { return paidAt; }
    };
  }

  private static OrderRepository.OrderStatusCount statusCount(OrderStatus status, long count) {
    return new OrderRepository.OrderStatusCount() {
      @Override public OrderStatus getStatus() { return status; }
      @Override public long getOrderCount() { return count; }
    };
  }

  static class Fixture {
    final RepositoryStub<PaymentRepository> payments = new RepositoryStub<>(PaymentRepository.class);
    final RepositoryStub<OrderRepository> orders = new RepositoryStub<>(OrderRepository.class);
    final RepositoryStub<CafeTableRepository> tables = new RepositoryStub<>(CafeTableRepository.class);
    final RepositoryStub<TableSessionRepository> sessions = new RepositoryStub<>(TableSessionRepository.class);
    final RepositoryStub<IngredientRepository> ingredients = new RepositoryStub<>(IngredientRepository.class);
    final RepositoryStub<ProductRepository> products = new RepositoryStub<>(ProductRepository.class);

    Instant capturedPayFrom;
    Instant capturedPayUntil;
    LocalDateTime capturedOrderFrom;
    LocalDateTime capturedOrderUntil;

    Fixture() {
      payments.on("findCompletedPaymentsInRange", args -> {
        capturedPayFrom = (Instant) args[0];
        capturedPayUntil = (Instant) args[1];
        return List.of(
            slice(PaymentMethod.CASH, new BigDecimal("40000"), Instant.parse("2026-11-18T02:00:00Z")),
            slice(PaymentMethod.CASH, new BigDecimal("55000"), Instant.parse("2026-11-24T01:30:00Z")),
            slice(PaymentMethod.BANK_TRANSFER, new BigDecimal("70000"), Instant.parse("2026-11-24T04:15:00Z"))
        );
      });

      orders.on("countByStatusInRange", args -> {
        capturedOrderFrom = (LocalDateTime) args[0];
        capturedOrderUntil = (LocalDateTime) args[1];
        return List.of(
            statusCount(OrderStatus.PENDING, 1),
            statusCount(OrderStatus.PREPARING, 2),
            statusCount(OrderStatus.COMPLETED, 2),
            statusCount(OrderStatus.EXPIRED, 1)
        );
      });

      CafeTable t1 = new CafeTable(); t1.setId(1L); t1.setName("Bàn 01"); t1.setIsActive(true);
      CafeTable t2 = new CafeTable(); t2.setId(2L); t2.setName("Bàn 02"); t2.setIsActive(true);
      CafeTable t3 = new CafeTable(); t3.setId(3L); t3.setName("Bàn 03"); t3.setIsActive(false);
      tables.on("findAll", args -> List.of(t1, t2, t3));

      TableSession openSession = new TableSession();
      openSession.setId(10L);
      openSession.setTable(t1);
      openSession.setStatus(TableSessionStatus.OPEN);
      sessions.on("findByStatus", args -> List.of(openSession));

      Ingredient i1 = new Ingredient();
      i1.setId(1L); i1.setName("Cà phê hạt"); i1.setUnit("g"); i1.setStockQuantity(100.0); i1.setMinThreshold(500.0);
      Ingredient i2 = new Ingredient();
      i2.setId(2L); i2.setName("Bột cacao"); i2.setUnit("g"); i2.setStockQuantity(0.0); i2.setMinThreshold(100.0);
      ingredients.on("findLowStockIngredients", args -> List.of(i1, i2));

      Product p1 = new Product();
      p1.setId(5L); p1.setName("Nước suối"); p1.setUnit("chai"); p1.setProductType(ProductType.PACKAGED); p1.setStockQuantity(0);
      Product p2 = new Product();
      p2.setId(6L); p2.setName("Bánh quy"); p2.setUnit("gói"); p2.setProductType(ProductType.PACKAGED); p2.setStockQuantity(12);
      products.on("findByProductTypeOrderByIdAsc", args -> List.of(p1, p2));
    }
  }

  @Configuration
  @EnableWebMvc
  static class TestConfig {
    @Bean
    JwtAuthenticationFilter filter() {
      return new JwtAuthenticationFilter(null, null);
    }

    @Bean
    UserDetailsService users() {
      return name -> User.withUsername(name).password("unused").roles("STAFF").build();
    }

    @Bean
    Fixture fixture() {
      return new Fixture();
    }

    @Bean
    AdminDashboardController controller(Fixture fixture) {
      Clock fixedClock = Clock.fixed(Instant.parse("2026-11-24T05:00:00Z"), ZoneOffset.UTC);
      return new AdminDashboardController(new AdminDashboardServiceImpl(
          fixture.payments.value,
          fixture.orders.value,
          fixture.tables.value,
          fixture.sessions.value,
          fixture.ingredients.value,
          fixture.products.value,
          fixedClock
      ));
    }

    @Bean
    GlobalExceptionHandler errors() {
      return new GlobalExceptionHandler();
    }
  }
}
