package com.example.cafe.feature.payment;

import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.common.exception.GlobalExceptionHandler;
import com.example.cafe.config.SecurityConfig;
import com.example.cafe.feature.order.repository.OrderItemRepository;
import com.example.cafe.feature.payment.controller.AdminReportController;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.feature.payment.service.impl.AdminReportServiceImpl;
import com.example.cafe.security.JwtAuthenticationFilter;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminReportTest {
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
  void defaultRangeUsesVietnamTodayAndHandlesEmptyDataCleanly() throws Exception {
    fixture.paymentsData = List.of();
    fixture.topProductsData = List.of();

    // Fixed clock: 2026-11-24T17:30:00Z -> 2026-11-25 00:30 in Asia/Ho_Chi_Minh
    mvc.perform(get("/api/admin/reports/revenue").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.fromDate").value("2026-11-25"))
        .andExpect(jsonPath("$.data.toDate").value("2026-11-25"))
        .andExpect(jsonPath("$.data.timeZone").value("Asia/Ho_Chi_Minh"))
        .andExpect(jsonPath("$.data.totalRevenue").value(0))
        .andExpect(jsonPath("$.data.paymentCount").value(0))
        .andExpect(jsonPath("$.data.averageOrderValue").value(0))
        .andExpect(jsonPath("$.data.methods.length()").value(2))
        .andExpect(jsonPath("$.data.daily.length()").value(1))
        .andExpect(jsonPath("$.data.daily[0].date").value("2026-11-25"))
        .andExpect(jsonPath("$.data.daily[0].revenue").value(0))
        .andExpect(jsonPath("$.data.topProducts.length()").value(0));

    ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
    assertEquals(LocalDate.of(2026, 11, 25).atStartOfDay(vn).toInstant(), fixture.capturedFrom);
    assertEquals(LocalDate.of(2026, 11, 26).atStartOfDay(vn).toInstant(), fixture.capturedUntil);
  }

  @Test
  void multiDayReportAggregatesMethodsFillsZeroDaysAndIncludesTopProducts() throws Exception {
    // 2026-11-20 22:00 VN = 2026-11-20T15:00:00Z; 2026-11-22 08:30 VN = 2026-11-22T01:30:00Z
    fixture.paymentsData = List.of(
        slice(PaymentMethod.CASH, new BigDecimal("50000"), Instant.parse("2026-11-20T15:00:00Z")),
        slice(PaymentMethod.BANK_TRANSFER, new BigDecimal("40000"), Instant.parse("2026-11-20T16:30:00Z")),
        slice(PaymentMethod.CASH, new BigDecimal("60000"), Instant.parse("2026-11-22T01:30:00Z"))
    );
    fixture.topProductsData = List.of(
        topProduct(1L, "Cà phê sữa đá", 12L, new BigDecimal("420000")),
        topProduct(2L, "Trà đào", 5L, new BigDecimal("175000"))
    );

    mvc.perform(get("/api/admin/reports/revenue").with(user("admin").roles("ADMIN"))
            .param("fromDate", "2026-11-20")
            .param("toDate", "2026-11-22")
            .param("topLimit", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalRevenue").value(150000))
        .andExpect(jsonPath("$.data.paymentCount").value(3))
        .andExpect(jsonPath("$.data.averageOrderValue").value(50000))
        .andExpect(jsonPath("$.data.methods[0].method").value("CASH"))
        .andExpect(jsonPath("$.data.methods[0].amount").value(110000))
        .andExpect(jsonPath("$.data.methods[0].paymentCount").value(2))
        .andExpect(jsonPath("$.data.methods[1].method").value("BANK_TRANSFER"))
        .andExpect(jsonPath("$.data.methods[1].amount").value(40000))
        .andExpect(jsonPath("$.data.methods[1].paymentCount").value(1))
        .andExpect(jsonPath("$.data.daily.length()").value(3))
        .andExpect(jsonPath("$.data.daily[0].date").value("2026-11-20"))
        .andExpect(jsonPath("$.data.daily[0].revenue").value(90000))
        .andExpect(jsonPath("$.data.daily[0].cashRevenue").value(50000))
        .andExpect(jsonPath("$.data.daily[0].bankTransferRevenue").value(40000))
        .andExpect(jsonPath("$.data.daily[1].date").value("2026-11-21"))
        .andExpect(jsonPath("$.data.daily[1].revenue").value(0))
        .andExpect(jsonPath("$.data.daily[1].paymentCount").value(0))
        .andExpect(jsonPath("$.data.daily[2].date").value("2026-11-22"))
        .andExpect(jsonPath("$.data.daily[2].revenue").value(60000))
        .andExpect(jsonPath("$.data.topProducts[0].productName").value("Cà phê sữa đá"))
        .andExpect(jsonPath("$.data.topProducts[0].quantitySold").value(12))
        .andExpect(jsonPath("$.data.topProducts[0].revenue").value(420000));

    assertNotNull(fixture.capturedTopPageable);
    assertEquals(5, fixture.capturedTopPageable.getPageSize());
  }

  @Test
  void invalidDateRangeOrTopLimitReturns400WithoutQueryingDatabase() throws Exception {
    mvc.perform(get("/api/admin/reports/revenue").with(user("admin").roles("ADMIN"))
            .param("fromDate", "2026-11-25").param("toDate", "2026-11-20"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/admin/reports/revenue").with(user("admin").roles("ADMIN"))
            .param("fromDate", "2025-01-01").param("toDate", "2026-11-20"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/admin/reports/revenue").with(user("admin").roles("ADMIN"))
            .param("topLimit", "0"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/admin/reports/revenue").with(user("admin").roles("ADMIN"))
            .param("topLimit", "51"))
        .andExpect(status().isBadRequest());

    assertTrue(fixture.payments.calls.isEmpty());
    assertTrue(fixture.orderItems.calls.isEmpty());
  }

  @Test
  void onlyAdminMayReadStoreRevenueReport() throws Exception {
    mvc.perform(get("/api/admin/reports/revenue")).andExpect(status().isUnauthorized());
    for (String role : List.of("STAFF", "CUSTOMER")) {
      mvc.perform(get("/api/admin/reports/revenue").with(user("other").roles(role)))
          .andExpect(status().isForbidden());
    }
    assertTrue(fixture.payments.calls.isEmpty());
  }

  private static PaymentRepository.CompletedPaymentSlice slice(
      PaymentMethod method, BigDecimal amount, Instant paidAt) {
    return new PaymentRepository.CompletedPaymentSlice() {
      @Override public PaymentMethod getMethod() { return method; }
      @Override public BigDecimal getAmount() { return amount; }
      @Override public Instant getPaidAt() { return paidAt; }
    };
  }

  private static OrderItemRepository.TopProductAggregate topProduct(
      Long id, String name, Long qty, BigDecimal rev) {
    return new OrderItemRepository.TopProductAggregate() {
      @Override public Long getProductId() { return id; }
      @Override public String getProductName() { return name; }
      @Override public Long getQuantitySold() { return qty; }
      @Override public BigDecimal getRevenue() { return rev; }
    };
  }

  static class Fixture {
    final RepositoryStub<PaymentRepository> payments = new RepositoryStub<>(PaymentRepository.class);
    final RepositoryStub<OrderItemRepository> orderItems = new RepositoryStub<>(OrderItemRepository.class);

    List<PaymentRepository.CompletedPaymentSlice> paymentsData = List.of();
    List<OrderItemRepository.TopProductAggregate> topProductsData = List.of();
    Instant capturedFrom;
    Instant capturedUntil;
    Pageable capturedTopPageable;

    Fixture() {
      payments.on("findCompletedPaymentsInRange", args -> {
        capturedFrom = (Instant) args[0];
        capturedUntil = (Instant) args[1];
        return paymentsData;
      });
      orderItems.on("findTopProductsCompletedInRange", args -> {
        capturedTopPageable = (Pageable) args[2];
        return topProductsData;
      });
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
    AdminReportController controller(Fixture fixture) {
      Clock fixedClock = Clock.fixed(Instant.parse("2026-11-24T17:30:00Z"), ZoneOffset.UTC);
      return new AdminReportController(new AdminReportServiceImpl(
          fixture.payments.value,
          fixture.orderItems.value,
          fixedClock
      ));
    }

    @Bean
    GlobalExceptionHandler errors() {
      return new GlobalExceptionHandler();
    }
  }
}
