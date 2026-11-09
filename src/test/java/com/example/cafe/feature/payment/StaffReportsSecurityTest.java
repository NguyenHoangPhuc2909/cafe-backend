package com.example.cafe.feature.payment;

import com.example.cafe.config.SecurityConfig;
import com.example.cafe.feature.payment.controller.PersonalRevenueController;
import com.example.cafe.feature.payment.dto.response.PersonalRevenueResponse;
import com.example.cafe.feature.payment.service.PersonalRevenueService;
import com.example.cafe.common.response.PageResponse;
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

  @Configuration @EnableWebMvc
  static class TestConfig {
    @Bean JwtAuthenticationFilter jwtAuthenticationFilter() { return new JwtAuthenticationFilter(null, null); }
    @Bean UserDetailsService userDetailsService() { return name -> User.withUsername(name).password("unused").roles("STAFF").build(); }
    @Bean PersonalRevenueController controller() { return new PersonalRevenueController(service()); }
    @Bean PersonalRevenueService service() {
      return (username, page, size) -> {
        assertEquals("cashier", username);
        return new PersonalRevenueResponse(LocalDate.of(2026, 11, 9), "Asia/Ho_Chi_Minh", Instant.EPOCH,
            BigDecimal.ZERO, 0, List.of(), new PageResponse<>(List.of(), 0, 20, 0, 0, true));
      };
    }
  }
}
