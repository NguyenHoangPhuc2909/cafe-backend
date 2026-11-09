package com.example.cafe.feature.payment.service.impl;

import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.payment.Payment;
import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PersonalRevenueTest {
  private final RepositoryStub<PaymentRepository> payments = new RepositoryStub<>(PaymentRepository.class);

  private void expectWindow(String now, String from, String until, List<PaymentRepository.MethodTotal> totals) {
    payments.on("summarizePersonalDay", args -> {
      assertEquals("cashier", args[0]); assertEquals(Instant.parse(from), args[1]); assertEquals(Instant.parse(until), args[2]);
      return totals;
    });
    payments.on("findByConfirmedBy_UsernameAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtDescIdDesc", args -> {
      assertEquals("cashier", args[0]); assertEquals(Instant.parse(from), args[1]); assertEquals(Instant.parse(until), args[2]);
      return Page.empty((Pageable) args[3]);
    });
  }

  @Test void midnightUsesVietnamDateRatherThanUtcDateAndReturnsZeros() {
    String now = "2026-11-08T17:00:00Z";
    expectWindow(now, "2026-11-08T17:00:00Z", "2026-11-09T17:00:00Z", List.of());
    var result = new PersonalRevenueServiceImpl(payments.value, Clock.fixed(Instant.parse(now), ZoneOffset.UTC)).today("cashier", 0, 20);
    assertEquals(LocalDate.of(2026, 11, 9), result.date());
    assertEquals(0, result.totalAmount().signum()); assertEquals(0, result.paymentCount());
    assertEquals(2, result.methods().size()); assertTrue(result.payments().getContent().isEmpty());
  }

  @Test void justBeforeMidnightStillUsesThePreviousVietnamDay() {
    String now = "2026-11-08T16:59:59Z";
    expectWindow(now, "2026-11-07T17:00:00Z", "2026-11-08T17:00:00Z", List.of());
    var result = new PersonalRevenueServiceImpl(payments.value, Clock.fixed(Instant.parse(now), ZoneOffset.UTC)).today("cashier", -1, 999);
    assertEquals(LocalDate.of(2026, 11, 8), result.date()); assertEquals(0, result.payments().getPage()); assertEquals(100, result.payments().getSize());
  }

  @Test void totalIncludesAllPagesAndRetainsDecimalPrecision() {
    String now = "2026-11-09T10:00:00Z";
    expectWindow(now, "2026-11-08T17:00:00Z", "2026-11-09T17:00:00Z", List.of(
        total(PaymentMethod.CASH, "100000.10", 8), total(PaymentMethod.BANK_TRANSFER, "200000.20", 9)));
    var payment = new Payment(); var order = new Order(); order.setId(55L);
    payment.setId(9L); payment.setOrder(order); payment.setAmount(new BigDecimal("15000.00"));
    payment.setMethod(PaymentMethod.CASH); payment.setPaidAt(Instant.parse(now));
    payments.on("findByConfirmedBy_UsernameAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtDescIdDesc", args ->
        new PageImpl<>(List.of(payment), (Pageable) args[3], 17));
    var result = new PersonalRevenueServiceImpl(payments.value, Clock.fixed(Instant.parse(now), ZoneOffset.UTC)).today("cashier", 1, 10);
    assertEquals(new BigDecimal("300000.30"), result.totalAmount()); assertEquals(17, result.paymentCount());
    assertEquals(1, result.payments().getContent().size()); assertEquals(55L, result.payments().getContent().getFirst().orderId());
  }

  private PaymentRepository.MethodTotal total(PaymentMethod method, String amount, long count) {
    return new PaymentRepository.MethodTotal() {
      public PaymentMethod getMethod() { return method; }
      public BigDecimal getAmount() { return new BigDecimal(amount); }
      public long getPaymentCount() { return count; }
    };
  }
}
