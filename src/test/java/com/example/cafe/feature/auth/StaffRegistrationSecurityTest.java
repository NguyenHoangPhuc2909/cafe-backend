package com.example.cafe.feature.auth;

import com.example.cafe.config.SecurityConfig;
import com.example.cafe.feature.auth.controller.AuthController;
import com.example.cafe.feature.auth.dto.request.LoginRequest;
import com.example.cafe.feature.auth.dto.request.RegisterRequest;
import com.example.cafe.feature.auth.dto.response.AuthResponse;
import com.example.cafe.feature.auth.dto.response.StaffAccountResponse;
import com.example.cafe.feature.auth.service.AuthService;
import com.example.cafe.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StaffRegistrationSecurityTest {
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mvc;
  private static final String BODY = """
      {"username":"staffnew01","password":"test-password","email":"staff@example.test","fullName":"Staff"}
      """;

  @BeforeEach void setup() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(SecurityConfig.class, TestConfig.class);
    context.refresh();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach void cleanup() { context.close(); }

  @Test void anonymousCannotRegister() throws Exception {
    mvc.perform(post("/api/auth/register").contentType("application/json").content(BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test void staffCannotCreateAnotherAccount() throws Exception {
    mvc.perform(post("/api/auth/register").with(user("cashier").roles("STAFF"))
        .contentType("application/json").content(BODY)).andExpect(status().isForbidden());
  }

  @Test void adminReceivesAccountInformationWithoutCredentialsOrToken() throws Exception {
    mvc.perform(post("/api/auth/register").with(user("manager").roles("ADMIN"))
        .contentType("application/json").content(BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.username").value("staffnew01"))
        .andExpect(jsonPath("$.data.role").value("STAFF"))
        .andExpect(jsonPath("$.data.accessToken").doesNotExist())
        .andExpect(jsonPath("$.data.password").doesNotExist());
  }

  @Test void loginRemainsPublic() throws Exception {
    mvc.perform(post("/api/auth/login").contentType("application/json")
        .content("{\"username\":\"staffnew01\",\"password\":\"test-password\"}"))
        .andExpect(status().isOk());
  }

  @Configuration
  @EnableWebMvc
  static class TestConfig {
    @Bean JwtAuthenticationFilter jwtAuthenticationFilter() { return new JwtAuthenticationFilter(null, null); }
    @Bean UserDetailsService userDetailsService() {
      return username -> User.withUsername(username).password("unused").roles("STAFF").build();
    }
    @Bean AuthController authController() {
      return new AuthController(new AuthService() {
        public StaffAccountResponse register(RegisterRequest request) {
          return new StaffAccountResponse(1L, request.getUsername(), request.getEmail(), request.getFullName(), "STAFF");
        }
        public AuthResponse login(LoginRequest request) {
          return AuthResponse.builder().username(request.getUsername()).role("STAFF").accessToken("test-only").build();
        }
      });
    }
  }
}
