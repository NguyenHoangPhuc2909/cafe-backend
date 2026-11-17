package com.example.cafe.feature.catalog;

import com.example.cafe.config.SecurityConfig;
import com.example.cafe.common.entity.catalog.Category;
import com.example.cafe.common.exception.GlobalExceptionHandler;
import com.example.cafe.feature.catalog.controller.AdminCategoryController;
import com.example.cafe.feature.catalog.repository.CategoryRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.catalog.service.impl.AdminCategoryService;
import com.example.cafe.feature.catalog.service.impl.CategoryServiceImpl;
import com.example.cafe.security.JwtAuthenticationFilter;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminCategoryTest {
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mvc;
  private Fixture fixture;

  @BeforeEach void setup() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(SecurityConfig.class, TestConfig.class);
    context.refresh();
    fixture = context.getBean(Fixture.class);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }
  @AfterEach void close() { context.close(); }

  @Test void adminCreatesWith201AndTrimsName() throws Exception {
    mvc.perform(post("/api/admin/categories").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content("{\"name\":\"  Cà phê  \"}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.data.name").value("Cà phê"));
    assertEquals("Cà phê", fixture.item.getName());
  }
  @Test void adminListsAndRenamesExistingCategory() throws Exception {
    mvc.perform(get("/api/admin/categories").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
    mvc.perform(put("/api/admin/categories/1").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content("{\"name\":\"Trà\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
    assertEquals("Trà", fixture.item.getName());
  }
  @Test void duplicatesAreRejectedWithoutWriting() throws Exception {
    fixture.duplicate = true;
    for (var request : List.of(post("/api/admin/categories"), put("/api/admin/categories/1")))
      mvc.perform(request.with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"name\":\"Cà phê\"}"))
          .andExpect(status().isConflict());
    assertFalse(fixture.categories.calls.containsKey("saveAndFlush"));
  }
  @Test void categoryWithProductsCannotBeDeleted() throws Exception {
    fixture.inUse = true;
    mvc.perform(delete("/api/admin/categories/1").with(user("admin").roles("ADMIN")))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    assertFalse(fixture.deleted);
  }
  @Test void unusedCategoryCanBeDeleted() throws Exception {
    mvc.perform(delete("/api/admin/categories/1").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    assertTrue(fixture.deleted);
  }
  @Test void missingCategoryReturns404() throws Exception {
    fixture.missing = true;
    mvc.perform(delete("/api/admin/categories/999").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
    mvc.perform(put("/api/admin/categories/999").with(user("admin").roles("ADMIN"))
        .contentType("application/json").content("{\"name\":\"Trà\"}")).andExpect(status().isNotFound());
  }
  @Test void invalidNamesNeverReachPersistence() throws Exception {
    for (String name : List.of("", "   ", "a".repeat(101)))
      mvc.perform(post("/api/admin/categories").with(user("admin").roles("ADMIN"))
          .contentType("application/json").content("{\"name\":\"" + name + "\"}"))
          .andExpect(status().isBadRequest());
    assertFalse(fixture.categories.calls.containsKey("saveAndFlush"));
  }
  @Test void onlyAdminCanReadOrWriteCategories() throws Exception {
    for (var request : List.of(get("/api/admin/categories"), post("/api/admin/categories"),
        put("/api/admin/categories/1"), delete("/api/admin/categories/1"))) {
      mvc.perform(request).andExpect(status().isUnauthorized());
      for (String role : List.of("STAFF", "CUSTOMER"))
        mvc.perform(request.with(user("other").roles(role))).andExpect(status().isForbidden());
    }
    assertTrue(fixture.categories.calls.isEmpty());
  }

  static class Fixture {
    final RepositoryStub<CategoryRepository> categories = new RepositoryStub<>(CategoryRepository.class);
    final RepositoryStub<ProductRepository> products = new RepositoryStub<>(ProductRepository.class);
    Category item = new Category();
    boolean duplicate, inUse, missing, deleted;
    Fixture() {
      item.setId(1L); item.setName("Cà phê");
      categories.on("findAll", args -> List.of(item));
      categories.on("findByIdForUpdate", args -> missing ? Optional.empty() : Optional.of(item));
      categories.on("existsByNameIgnoreCaseAndIdNot", args -> duplicate);
      categories.on("saveAndFlush", args -> { item = (Category) args[0]; item.setId(1L); return item; });
      categories.on("delete", args -> { deleted = true; return null; });
      categories.on("flush", args -> null);
      products.on("existsByCategory_Id", args -> inUse);
    }
  }
  @Configuration @EnableWebMvc static class TestConfig {
    @Bean JwtAuthenticationFilter filter() { return new JwtAuthenticationFilter(null, null); }
    @Bean UserDetailsService users() { return name -> User.withUsername(name).password("unused").roles("STAFF").build(); }
    @Bean Fixture fixture() { return new Fixture(); }
    @Bean AdminCategoryController controller(Fixture fixture) {
      return new AdminCategoryController(new CategoryServiceImpl(fixture.categories.value),
          new AdminCategoryService(fixture.categories.value, fixture.products.value));
    }
    @Bean GlobalExceptionHandler errors() { return new GlobalExceptionHandler(); }
  }
}
