package com.example.cafe.feature.inventory;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.entity.inventory.StockMovement;
import com.example.cafe.common.enums.ProductType;
import com.example.cafe.common.enums.StockMovementType;
import com.example.cafe.common.exception.GlobalExceptionHandler;
import com.example.cafe.config.SecurityConfig;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.inventory.controller.InventoryController;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.inventory.repository.StockMovementRepository;
import com.example.cafe.feature.inventory.service.impl.InventoryServiceImpl;
import com.example.cafe.security.JwtAuthenticationFilter;
import com.example.cafe.support.RepositoryStub;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StockReceiptHistoryTest {
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mvc;
  private Fixture fixture;

  private static final String RECEIPT_BODY = """
      {"requestId":"%s","ingredientId":1,"quantity":500,"note":"Nhập sáng"}
      """.formatted(UUID.randomUUID());

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
  void adminCanReadHistoryAcrossStaffAndCannotReceiveStock() throws Exception {
    mvc.perform(get("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.content[0].performedById").value(10))
        .andExpect(jsonPath("$.data.content[0].performedByName").value("Nguyễn Văn A"))
        .andExpect(jsonPath("$.data.content[0].itemName").value("Cà phê hạt"))
        .andExpect(jsonPath("$.data.content[0].availableBefore").value(200.0))
        .andExpect(jsonPath("$.data.content[0].availableAfter").value(700.0))
        .andExpect(jsonPath("$.data.content[1].performedById").value(11))
        .andExpect(jsonPath("$.data.content[1].performedByName").value("staff_b"))
        .andExpect(jsonPath("$.data.content[1].itemName").value("Nước suối"));

    mvc.perform(post("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content(RECEIPT_BODY))
        .andExpect(status().isForbidden());

    assertFalse(fixture.movements.calls.containsKey("saveAndFlush"));
  }

  @Test
  void staffCanReadHistoryAndReceiveStockWhileAnonymousAndCustomerAreBlocked() throws Exception {
    mvc.perform(get("/api/staff/inventory/receipts")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/staff/inventory/receipts").contentType("application/json").content(RECEIPT_BODY))
        .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/staff/inventory/receipts").with(user("guest").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/staff/inventory/receipts").with(user("guest").roles("CUSTOMER"))
            .contentType("application/json").content(RECEIPT_BODY))
        .andExpect(status().isForbidden());

    mvc.perform(get("/api/staff/inventory/receipts").with(user("staff_a").roles("STAFF")))
        .andExpect(status().isOk());

    mvc.perform(post("/api/staff/inventory/receipts").with(user("staff_a").roles("STAFF"))
            .contentType("application/json").content(RECEIPT_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.performedByName").value("Nguyễn Văn A"))
        .andExpect(jsonPath("$.data.availableAfter").value(700.0));
  }

  @Test
  void filtersApplyHoChiMinhDateRangeIngredientAndPerformedByIdWithSafePagination() throws Exception {
    mvc.perform(get("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN"))
            .param("page", "-3")
            .param("size", "250")
            .param("fromDate", "2026-11-10")
            .param("toDate", "2026-11-15")
            .param("ingredientId", "7")
            .param("performedById", "10"))
        .andExpect(status().isOk());

    assertNotNull(fixture.capturedPageable);
    assertEquals(0, fixture.capturedPageable.getPageNumber());
    assertEquals(100, fixture.capturedPageable.getPageSize());
    assertEquals(
        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
        fixture.capturedPageable.getSort()
    );

    List<String> predicates = evaluateSpec(fixture.capturedSpec);
    ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
    Instant expectedFrom = LocalDate.of(2026, 11, 10).atStartOfDay(vn).toInstant();
    Instant expectedToExclusive = LocalDate.of(2026, 11, 16).atStartOfDay(vn).toInstant();

    assertTrue(predicates.contains("equal(type, RECEIPT)"));
    assertTrue(predicates.contains("greaterThanOrEqualTo(createdAt, " + expectedFrom + ")"));
    assertTrue(predicates.contains("lessThan(createdAt, " + expectedToExclusive + ")"));
    assertTrue(predicates.contains("equal(ingredient.id, 7)"));
    assertTrue(predicates.contains("equal(performedBy.id, 10)"));
  }

  @Test
  void invalidFilterParametersReturn400WithoutQueryingDatabase() throws Exception {
    mvc.perform(get("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN"))
            .param("fromDate", "2026-11-20").param("toDate", "2026-11-10"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN"))
            .param("ingredientId", "1").param("productId", "2"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN"))
            .param("ingredientId", "0"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN"))
            .param("productId", "-1"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN"))
            .param("performedById", "0"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/staff/inventory/receipts").with(user("admin").roles("ADMIN"))
            .param("performedById", "-5"))
        .andExpect(status().isBadRequest());

    assertFalse(fixture.movements.calls.containsKey("findAll"));
  }

  @SuppressWarnings("unchecked")
  private static List<String> evaluateSpec(Specification<StockMovement> spec) {
    assertNotNull(spec);
    List<String> recorded = new ArrayList<>();
    Predicate dummyPredicate = (Predicate) Proxy.newProxyInstance(
        Predicate.class.getClassLoader(),
        new Class<?>[]{Predicate.class},
        (proxy, method, args) -> null
    );
    Root<StockMovement> root = (Root<StockMovement>) createPathProxy(Root.class, "");
    CriteriaQuery<?> query = (CriteriaQuery<?>) Proxy.newProxyInstance(
        CriteriaQuery.class.getClassLoader(),
        new Class<?>[]{CriteriaQuery.class},
        (proxy, method, args) -> null
    );
    CriteriaBuilder cb = (CriteriaBuilder) Proxy.newProxyInstance(
        CriteriaBuilder.class.getClassLoader(),
        new Class<?>[]{CriteriaBuilder.class},
        (proxy, method, args) -> {
          String name = method.getName();
          if ("equal".equals(name) || "greaterThanOrEqualTo".equals(name) || "lessThan".equals(name)) {
            recorded.add(name + "(" + args[0] + ", " + args[1] + ")");
          }
          return dummyPredicate;
        }
    );
    spec.toPredicate(root, query, cb);
    return recorded;
  }

  private static Object createPathProxy(Class<?> iface, String prefix) {
    return Proxy.newProxyInstance(
        iface.getClassLoader(),
        new Class<?>[]{iface},
        (proxy, method, args) -> {
          if ("get".equals(method.getName()) && args != null && args.length == 1) {
            String segment = String.valueOf(args[0]);
            String nextPath = prefix.isEmpty() ? segment : prefix + "." + segment;
            return createPathProxy(Path.class, nextPath);
          }
          if ("toString".equals(method.getName())) {
            return prefix;
          }
          return null;
        }
    );
  }

  static class Fixture {
    final RepositoryStub<ProductRepository> products = new RepositoryStub<>(ProductRepository.class);
    final RepositoryStub<UserRepository> users = new RepositoryStub<>(UserRepository.class);
    final RepositoryStub<IngredientRepository> ingredients = new RepositoryStub<>(IngredientRepository.class);
    final RepositoryStub<StockMovementRepository> movements = new RepositoryStub<>(StockMovementRepository.class);

    Specification<StockMovement> capturedSpec;
    Pageable capturedPageable;

    @SuppressWarnings("unchecked")
    Fixture() {
      User staffA = new User();
      staffA.setId(10L);
      staffA.setUsername("staff_a");
      staffA.setFullName("Nguyễn Văn A");

      User staffB = new User();
      staffB.setId(11L);
      staffB.setUsername("staff_b");
      staffB.setFullName("   ");

      Ingredient coffee = new Ingredient();
      coffee.setId(1L);
      coffee.setName("Cà phê hạt");
      coffee.setUnit("g");
      coffee.setStockQuantity(200.0);

      Product water = new Product();
      water.setId(2L);
      water.setName("Nước suối");
      water.setUnit("chai");
      water.setProductType(ProductType.PACKAGED);
      water.setStockQuantity(24);

      StockMovement m1 = new StockMovement();
      m1.setId(101L);
      m1.setType(StockMovementType.RECEIPT);
      m1.setIngredient(coffee);
      m1.setQuantityDelta(500.0);
      m1.setAvailableBefore(200.0);
      m1.setAvailableAfter(700.0);
      m1.setPerformedBy(staffA);
      m1.setNote("Nhập sáng");
      m1.setCreatedAt(Instant.parse("2026-11-15T01:30:00Z"));

      StockMovement m2 = new StockMovement();
      m2.setId(102L);
      m2.setType(StockMovementType.RECEIPT);
      m2.setProduct(water);
      m2.setQuantityDelta(12.0);
      m2.setAvailableBefore(12.0);
      m2.setAvailableAfter(24.0);
      m2.setPerformedBy(staffB);
      m2.setNote("Nhập chiều");
      m2.setCreatedAt(Instant.parse("2026-11-15T08:00:00Z"));

      users.on("findByUsernameForUpdate", args -> Optional.of(staffA));
      ingredients.on("findByIdForUpdate", args -> Optional.of(coffee));
      movements.on("findByPerformedBy_IdAndRequestId", args -> Optional.empty());
      movements.on("saveAndFlush", args -> {
        StockMovement saved = (StockMovement) args[0];
        saved.setId(103L);
        saved.setCreatedAt(Instant.parse("2026-11-16T02:00:00Z"));
        return saved;
      });
      movements.on("findAll", args -> {
        capturedSpec = (Specification<StockMovement>) args[0];
        capturedPageable = (Pageable) args[1];
        return new PageImpl<>(List.of(m1, m2), capturedPageable, 2);
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
    UserDetailsService userDetailsService() {
      return name -> org.springframework.security.core.userdetails.User
          .withUsername(name).password("unused").roles("STAFF").build();
    }

    @Bean
    Fixture fixture() {
      return new Fixture();
    }

    @Bean
    InventoryController controller(Fixture fixture) {
      return new InventoryController(new InventoryServiceImpl(
          fixture.products.value,
          fixture.users.value,
          fixture.ingredients.value,
          fixture.movements.value
      ));
    }

    @Bean
    GlobalExceptionHandler errors() {
      return new GlobalExceptionHandler();
    }
  }
}
