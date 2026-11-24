package com.example.cafe.feature.order;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.order.OrderItem;
import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.OrderPaymentStatus;
import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.enums.OrderType;
import com.example.cafe.common.exception.GlobalExceptionHandler;
import com.example.cafe.config.SecurityConfig;
import com.example.cafe.feature.order.controller.AdminOrderController;
import com.example.cafe.feature.order.repository.OrderItemRepository;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.order.service.impl.AdminOrderServiceImpl;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminOrderHistoryTest {
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
  void adminQueryAlwaysFiltersCompletedOrdersAndAppliesInclusiveEndDateBounds() throws Exception {
    mvc.perform(get("/api/admin/orders").with(user("admin").roles("ADMIN"))
            .param("fromDate", "2026-11-10")
            .param("toDate", "2026-11-15")
            .param("sessionId", "5")
            .param("page", "1")
            .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(501))
        .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.content[0].tableName").value("Bàn 01"))
        .andExpect(jsonPath("$.data.content[0].items[0].productName").value("Cà phê sữa"));

    assertNotNull(fixture.capturedPageable);
    assertEquals(1, fixture.capturedPageable.getPageNumber());
    assertEquals(20, fixture.capturedPageable.getPageSize());
    assertEquals(
        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
        fixture.capturedPageable.getSort()
    );

    List<String> predicates = evaluateSpec(fixture.capturedSpec);
    assertTrue(predicates.contains("equal(status, COMPLETED)"));
    assertTrue(predicates.contains("greaterThanOrEqualTo(createdAt, " + LocalDate.of(2026, 11, 10).atStartOfDay() + ")"));
    assertTrue(predicates.contains("lessThan(createdAt, " + LocalDate.of(2026, 11, 16).atStartOfDay() + ")"));
    assertTrue(predicates.contains("equal(tableSession.id, 5)"));
  }

  @Test
  void omittedDatesStillEnforceCompletedOnlyWithoutNullDatePredicates() throws Exception {
    mvc.perform(get("/api/admin/orders").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    List<String> predicates = evaluateSpec(fixture.capturedSpec);
    assertEquals(List.of("equal(status, COMPLETED)"), predicates);
  }

  @Test
  void invalidDateRangeSessionOrPaginationReturns400WithoutQueryingDatabase() throws Exception {
    mvc.perform(get("/api/admin/orders").with(user("admin").roles("ADMIN"))
            .param("fromDate", "2026-11-20").param("toDate", "2026-11-10"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/admin/orders").with(user("admin").roles("ADMIN"))
            .param("page", "-1"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/admin/orders").with(user("admin").roles("ADMIN"))
            .param("size", "0"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/admin/orders").with(user("admin").roles("ADMIN"))
            .param("size", "101"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/admin/orders").with(user("admin").roles("ADMIN"))
            .param("sessionId", "0"))
        .andExpect(status().isBadRequest());

    assertFalse(fixture.orders.calls.containsKey("findAll"));
  }

  @Test
  void onlyAdminMayAccessAdminOrderHistoryEndpoint() throws Exception {
    mvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
    for (String role : List.of("STAFF", "CUSTOMER")) {
      mvc.perform(get("/api/admin/orders").with(user("other").roles(role)))
          .andExpect(status().isForbidden());
    }
    assertTrue(fixture.orders.calls.isEmpty());
  }

  @SuppressWarnings("unchecked")
  private static List<String> evaluateSpec(Specification<Order> spec) {
    assertNotNull(spec);
    List<String> recorded = new ArrayList<>();
    Predicate dummyPredicate = (Predicate) Proxy.newProxyInstance(
        Predicate.class.getClassLoader(),
        new Class<?>[]{Predicate.class},
        (proxy, method, args) -> null
    );
    Root<Order> root = (Root<Order>) createPathProxy(Root.class, "");
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
    final RepositoryStub<OrderRepository> orders = new RepositoryStub<>(OrderRepository.class);
    final RepositoryStub<OrderItemRepository> items = new RepositoryStub<>(OrderItemRepository.class);

    Specification<Order> capturedSpec;
    Pageable capturedPageable;

    @SuppressWarnings("unchecked")
    Fixture() {
      CafeTable table = new CafeTable();
      table.setId(1L);
      table.setName("Bàn 01");

      TableSession session = new TableSession();
      session.setId(5L);
      session.setTable(table);

      Order order = new Order();
      order.setId(501L);
      order.setTableSession(session);
      order.setOrderType(OrderType.DINE_IN);
      order.setTotalAmount(new BigDecimal("35000.00"));
      order.setStatus(OrderStatus.COMPLETED);
      order.setPaymentStatus(OrderPaymentStatus.PAID);
      order.setCreatedAt(LocalDateTime.of(2026, 11, 15, 23, 59, 59));

      Product coffee = new Product();
      coffee.setId(10L);
      coffee.setName("Cà phê sữa");

      OrderItem item = new OrderItem();
      item.setId(901L);
      item.setOrder(order);
      item.setProduct(coffee);
      item.setQuantity(1);
      item.setPrice(new BigDecimal("35000.00"));

      orders.on("findAll", args -> {
        capturedSpec = (Specification<Order>) args[0];
        capturedPageable = (Pageable) args[1];
        return new PageImpl<>(List.of(order), capturedPageable, 1);
      });
      items.on("findByOrderId", args -> List.of(item));
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
    AdminOrderController controller(Fixture fixture) {
      return new AdminOrderController(new AdminOrderServiceImpl(fixture.orders.value, fixture.items.value));
    }

    @Bean
    GlobalExceptionHandler errors() {
      return new GlobalExceptionHandler();
    }
  }
}
