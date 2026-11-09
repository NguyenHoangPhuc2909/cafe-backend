package com.example.cafe.feature.payment;

import com.example.cafe.config.SecurityConfig;
import com.example.cafe.feature.payment.controller.PersonalRevenueController;
import com.example.cafe.feature.payment.dto.response.PersonalRevenueResponse;
import com.example.cafe.feature.payment.service.PersonalRevenueService;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.cash.controller.CashController;
import com.example.cafe.feature.cash.service.CashService;
import com.example.cafe.feature.cash.dto.request.*;
import com.example.cafe.feature.cash.dto.response.*;
import com.example.cafe.common.enums.CashMovementType;
import com.example.cafe.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StaffReportsSecurityTest {
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mvc;
  @BeforeEach void setup() {
    context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
    context.register(SecurityConfig.class, TestConfig.class); context.refresh();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }
  @AfterEach void cleanup() { context.close(); }

  @Test void anonymousCannotReadRevenue() throws Exception {
    mvc.perform(get("/api/staff/revenue/today")).andExpect(status().isUnauthorized());
  }
  @Test void customerAndAdminCannotUsePersonalStaffRevenueEndpoint() throws Exception {
    for (String role : List.of("CUSTOMER", "ADMIN"))
      mvc.perform(get("/api/staff/revenue/today").with(user("someone").roles(role))).andExpect(status().isForbidden());
  }
  @Test void staffIdentityComesFromAuthenticationEvenIfAnotherIdIsSupplied() throws Exception {
    mvc.perform(get("/api/staff/revenue/today").with(user("cashier").roles("STAFF"))
        .param("staffId", "999").param("username", "another-user"))
        .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.paymentCount").value(0));
  }
  @Test void inventoryReportsRemainRestrictedToStaffAndAdmin() throws Exception {
    mvc.perform(get("/api/staff/inventory/stocktakes").with(user("customer").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
  }
  @Test void sharedCashIsAvailableToStaffAndAdminButNotAnonymousOrCustomer() throws Exception {
    mvc.perform(get("/api/staff/cash/today")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/staff/cash/today").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
    for (String role : List.of("STAFF", "ADMIN"))
      mvc.perform(get("/api/staff/cash/today").with(user("cashier").roles(role)))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.openingAmount").value(500000));
  }
  @Test void movementActorIsTheAuthenticatedStaffAndCashInputsAreValidated() throws Exception {
    var body = """
        {"requestId":"da7cf2d5-62ba-48de-9d35-73087a6adc5f","businessDate":"2026-11-09","type":"PAYOUT","amount":100000,"note":"Mua ly"}
        """;
    mvc.perform(post("/api/staff/cash/movements").with(user("cashier").roles("STAFF"))
        .param("username", "other").contentType("application/json").content(body))
        .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.performedByName").value("cashier"));
    mvc.perform(post("/api/staff/cash/movements").with(user("cashier").roles("STAFF"))
        .contentType("application/json").content(body.replace("100000", "-1"))).andExpect(status().isBadRequest());
    mvc.perform(post("/api/staff/cash/movements").with(user("customer").roles("CUSTOMER"))
        .contentType("application/json").content(body)).andExpect(status().isForbidden());
  }

  @Configuration @EnableWebMvc
  static class TestConfig {
    @Bean JwtAuthenticationFilter jwtAuthenticationFilter() { return new JwtAuthenticationFilter(null, null); }
    @Bean UserDetailsService userDetailsService() { return name -> User.withUsername(name).password("unused").roles("STAFF").build(); }
    @Bean PersonalRevenueController controller() { return new PersonalRevenueController(service()); }
    @Bean CashController cashController() { return new CashController(cashService()); }
    @Bean CashService cashService() { return new CashService() {
      public CashDayResponse today(int page, int size) {
        return new CashDayResponse(LocalDate.of(2026, 11, 9), "Asia/Ho_Chi_Minh", Instant.EPOCH,
            new BigDecimal("500000"), BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("500000"), new PageResponse<>(List.of(), 0, 10, 0, 0, true));
      }
      public CashMovementResponse recordMovement(CashMovementRequest request, String username) {
        assertEquals("cashier", username);
        return new CashMovementResponse(1L, request.businessDate(), CashMovementType.PAYOUT, request.amount(), request.note(), 7L, username, Instant.EPOCH);
      }
      public CashReconciliationResponse reconcile(CashReconciliationRequest request, String username) { return null; }
      public PageResponse<CashReconciliationResponse> history(int page, int size) { return new PageResponse<>(List.of(), 0, 10, 0, 0, true); }
    }; }
    @Bean PersonalRevenueService service() {
      return (username, page, size) -> {
        assertEquals("cashier", username);
        return new PersonalRevenueResponse(LocalDate.of(2026, 11, 9), "Asia/Ho_Chi_Minh", Instant.EPOCH,
            BigDecimal.ZERO, 0, List.of(), new PageResponse<>(List.of(), 0, 20, 0, 0, true));
      };
    }
  }
}
