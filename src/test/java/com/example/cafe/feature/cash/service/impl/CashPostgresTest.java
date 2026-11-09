package com.example.cafe.feature.cash.service.impl;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.payment.Payment;
import com.example.cafe.common.enums.*;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.cash.dto.request.*;
import com.example.cafe.feature.cash.repository.*;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.Embeddable;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// Opt in: CAFE_CASH_PG_TEST=true ./mvnw.cmd -Dtest=CashPostgresTest test
// Every table and seed row lives in a unique temporary schema, never the application's schema.
@EnabledIfEnvironmentVariable(named = "CAFE_CASH_PG_TEST", matches = "true")
class CashPostgresTest {
  @Test void postgresRepositoriesPersistSharedCashAndExcludeTransfersAndOtherDays() throws Exception {
    var credentials = new Properties();
    try (var reader = Files.newBufferedReader(Path.of(".env"), StandardCharsets.UTF_8)) { credentials.load(reader); }
    String dbName = Objects.requireNonNull(credentials.getProperty("DB_NAME"));
    String password = Objects.requireNonNull(credentials.getProperty("DB_PASSWORD"));
    String baseUrl = "jdbc:postgresql://localhost:5432/" + dbName;
    String schema = "codex_cash_verify_" + UUID.randomUUID().toString().replace("-", "");
    boolean created = false;
    try (var admin = DriverManager.getConnection(baseUrl, "postgres", password)) {
      try {
        try (var statement = admin.createStatement()) { statement.execute("CREATE SCHEMA " + schema); created = true; }
        var configuration = new Configuration()
            .setProperty("hibernate.connection.url", baseUrl + "?currentSchema=" + schema)
            .setProperty("hibernate.connection.username", "postgres")
            .setProperty("hibernate.connection.password", password)
            .setProperty("hibernate.default_schema", schema)
            .setProperty("hibernate.hbm2ddl.auto", "create-drop")
            .setProperty("hibernate.show_sql", "false");
        Path entityRoot = Path.of("src/main/java/com/example/cafe/common/entity");
        try (var files = Files.walk(entityRoot)) {
          for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
            String name = "com.example.cafe.common.entity." + entityRoot.relativize(file).toString()
                .replace('\\', '.').replace('/', '.').replaceAll("\\.java$", "");
            Class<?> type = Class.forName(name);
            if (type.isAnnotationPresent(Entity.class) || type.isAnnotationPresent(Embeddable.class)) configuration.addAnnotatedClass(type);
          }
        }
        try (var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
          em.getTransaction().begin();
          try {
            var first = staff("cashier_a"); var second = staff("cashier_b"); em.persist(first); em.persist(second);
            seedPayment(em, first, "500000", PaymentMethod.CASH, "2026-11-09T08:00:00Z");
            seedPayment(em, second, "300000", PaymentMethod.CASH, "2026-11-09T09:00:00Z");
            seedPayment(em, first, "900000", PaymentMethod.BANK_TRANSFER, "2026-11-09T09:00:00Z");
            seedPayment(em, first, "700000", PaymentMethod.CASH, "2026-11-08T16:59:59Z");
            seedPayment(em, second, "600000", PaymentMethod.CASH, "2026-11-09T17:00:00Z");
            em.flush();
            var repositories = new JpaRepositoryFactory(em);
            var dayRepository = repositories.getRepository(CashDayRepository.class);
            var service = new CashServiceImpl(dayRepository, repositories.getRepository(CashMovementRepository.class),
                repositories.getRepository(CashReconciliationRepository.class), repositories.getRepository(PaymentRepository.class),
                repositories.getRepository(UserRepository.class), repositories.getRepository(OrderRepository.class),
                Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
            var date = LocalDate.of(2026, 11, 9);
            var initial = service.today(0, 10);
            assertMoney("500000", initial.openingAmount()); assertMoney("800000", initial.cashSales()); assertEquals(2, initial.cashPaymentCount());
            var payout = new CashMovementRequest(UUID.randomUUID(), date, CashMovementType.PAYOUT, new BigDecimal("100000"), "Mua vật dụng");
            var saved = service.recordMovement(payout, "cashier_a");
            assertEquals(saved.id(), service.recordMovement(payout, "cashier_a").id());
            service.recordMovement(new CashMovementRequest(UUID.randomUUID(), date, CashMovementType.TOP_UP, new BigDecimal("200000"), "Bổ sung"), "cashier_b");
            var day = service.today(0, 1);
            assertMoney("1400000", day.expectedAmount()); assertEquals(2, day.revision()); assertEquals(2, day.movements().getTotalElements());
            var count = new CashReconciliationRequest(UUID.randomUUID(), date, day.revision(), day.cashPaymentCount(), day.lastPaymentId(), new BigDecimal("1400000"), "");
            var report = service.reconcile(count, "cashier_b");
            assertMoney("0", report.difference()); assertEquals("cashier_b", report.performedByName());
            assertEquals(report.id(), service.reconcile(count, "cashier_b").id());
            assertEquals(1, service.history(0, 10).getTotalElements());
            // PostgreSQL's ON CONFLICT initialization must not reset an existing day's revision.
            dayRepository.ensureDay(date); em.flush(); em.clear();
            assertEquals(2, dayRepository.findById(date).orElseThrow().getRevision());
          } finally { if (em.getTransaction().isActive()) em.getTransaction().rollback(); }
        }
      } finally {
        if (created && schema.matches("codex_cash_verify_[0-9a-f]{32}")) {
          try (var statement = admin.createStatement()) { statement.execute("DROP SCHEMA " + schema + " CASCADE"); }
        }
      }
    }
  }

  private User staff(String name) {
    var staff = new User(); staff.setUsername(name); staff.setPassword("test-only"); staff.setEmail(name + "@example.test");
    staff.setRole("STAFF"); return staff;
  }
  private void seedPayment(jakarta.persistence.EntityManager em, User staff, String amount, PaymentMethod method, String paidAt) {
    var order = new Order(); order.setTotalAmount(new BigDecimal(amount)); order.setStatus(OrderStatus.COMPLETED); order.setPaymentStatus(OrderPaymentStatus.PAID); em.persist(order);
    var payment = new Payment(); payment.setOrder(order); payment.setConfirmedBy(staff); payment.setAmount(new BigDecimal(amount));
    payment.setMethod(method); payment.setPaidAt(Instant.parse(paidAt)); em.persist(payment);
  }
  private void assertMoney(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
}
