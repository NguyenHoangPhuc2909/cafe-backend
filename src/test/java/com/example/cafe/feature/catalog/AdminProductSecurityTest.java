package com.example.cafe.feature.catalog;

import com.example.cafe.config.SecurityConfig;
import com.example.cafe.common.exception.GlobalExceptionHandler;
import com.example.cafe.feature.catalog.controller.AdminProductController;
import com.example.cafe.feature.catalog.controller.ProductController;
import com.example.cafe.feature.catalog.dto.request.AdminProductRequest;
import com.example.cafe.feature.catalog.dto.response.AdminProductDetailResponse;
import com.example.cafe.feature.catalog.dto.response.ProductResponse;
import com.example.cafe.feature.catalog.service.ProductService;
import com.example.cafe.security.JwtAuthenticationFilter;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminProductSecurityTest {
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mvc;
  private static final String BODY = """
      {"product":{"name":"Cà phê","price":30000,"categoryId":1,"unit":"ly","productType":"MADE_TO_ORDER","sizeEnabled":false},
       "recipes":[{"ingredientId":2,"quantity":20}]}
      """;

  @BeforeEach void setup() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(SecurityConfig.class, TestConfig.class);
    context.refresh();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }
  @AfterEach void cleanup() { context.close(); }

  @Test void anonymousAndStaffCannotManageProductsOrReadRecipes() throws Exception {
    mvc.perform(post("/api/admin/products").contentType("application/json").content(BODY)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/admin/products").with(user("staff").roles("STAFF")).contentType("application/json").content(BODY)).andExpect(status().isForbidden());
    mvc.perform(put("/api/admin/products/1").with(user("staff").roles("STAFF")).contentType("application/json").content(BODY)).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/products/1").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/ingredients").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
  }

  @Test void adminCreateReturns201AndOmittedSizeBecomesM() throws Exception {
    mvc.perform(post("/api/admin/products").with(user("admin").roles("ADMIN")).contentType("application/json").content(BODY))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true));
    var request = context.getBean(ServiceBoundary.class).lastRequest;
    assertEquals(com.example.cafe.common.enums.DrinkSize.M, request.recipes().getFirst().size());
  }

  @Test void nestedFieldsAndRecipeRowsAreValidated() throws Exception {
    for (String body : List.of(BODY.replace("\"name\":\"Cà phê\"", "\"name\":\" \""),
        BODY.replace("\"quantity\":20", "\"quantity\":0"), BODY.replace("\"quantity\":20", "\"quantity\":null"),
        BODY.replace("\"recipes\":[{\"ingredientId\":2,\"quantity\":20}]", "\"recipes\":null"))) {
      mvc.perform(post("/api/admin/products").with(user("admin").roles("ADMIN")).contentType("application/json").content(body))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }
    assertNull(context.getBean(ServiceBoundary.class).lastRequest);
  }

  @Test void adminCanReadAndUpdateButStaffCannotBypassUsingOldProductRoutes() throws Exception {
    mvc.perform(get("/api/admin/products/1").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    mvc.perform(put("/api/admin/products/1").with(user("admin").roles("ADMIN")).contentType("application/json").content(BODY)).andExpect(status().isOk());
    mvc.perform(post("/api/products").with(user("staff").roles("STAFF")).contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(put("/api/products/1").with(user("staff").roles("STAFF")).contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
  }

  static class ServiceBoundary {
    final RepositoryStub<ProductService> stub = new RepositoryStub<>(ProductService.class);
    AdminProductRequest lastRequest;
    ServiceBoundary() {
      var detail = new AdminProductDetailResponse(ProductResponse.builder().id(1L).build(), List.of());
      stub.on("createAdminProduct", args -> { lastRequest = (AdminProductRequest) args[0]; return detail; });
      stub.on("updateAdminProduct", args -> { lastRequest = (AdminProductRequest) args[1]; return detail; });
      stub.on("getAdminProduct", args -> detail);
    }
  }
  @Configuration @EnableWebMvc static class TestConfig {
    @Bean JwtAuthenticationFilter jwtAuthenticationFilter() { return new JwtAuthenticationFilter(null, null); }
    @Bean UserDetailsService userDetailsService() { return name -> User.withUsername(name).password("unused").roles("STAFF").build(); }
    @Bean ServiceBoundary boundary() { return new ServiceBoundary(); }
    @Bean AdminProductController adminProducts(ServiceBoundary boundary) { return new AdminProductController(boundary.stub.value); }
    @Bean ProductController products(ServiceBoundary boundary) { return new ProductController(boundary.stub.value); }
    @Bean GlobalExceptionHandler errors() { return new GlobalExceptionHandler(); }
  }
}
