package com.example.cafe.feature.payment.service.impl;

import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.payment.dto.response.PersonalRevenueResponse;
import com.example.cafe.feature.payment.dto.response.PersonalRevenueResponse.MethodSummary;
import com.example.cafe.feature.payment.dto.response.PersonalRevenueResponse.Transaction;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.feature.payment.service.PersonalRevenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class PersonalRevenueServiceImpl implements PersonalRevenueService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final PaymentRepository payments;
  private final Clock clock;

  @Autowired
  public PersonalRevenueServiceImpl(PaymentRepository payments) {
    this(payments, Clock.systemUTC());
  }
  PersonalRevenueServiceImpl(PaymentRepository payments, Clock clock) {
    this.payments = payments;
    this.clock = clock;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PersonalRevenueResponse today(String username, int page, int size) {
    Instant now = clock.instant();
    LocalDate date = now.atZone(ZONE).toLocalDate();
    Instant from = date.atStartOfDay(ZONE).toInstant();
    Instant until = date.plusDays(1).atStartOfDay(ZONE).toInstant();
    var totals = payments.summarizePersonalDay(username, from, until);
    var methods = new ArrayList<MethodSummary>();
    for (var method : PaymentMethod.values()) {
      var match = totals.stream().filter(total -> total.getMethod() == method).findFirst();
      methods.add(match.map(total -> new MethodSummary(method, total.getAmount(), total.getPaymentCount()))
          .orElseGet(() -> new MethodSummary(method, BigDecimal.ZERO, 0)));
    }
    var transactions = payments
        .findByConfirmedBy_UsernameAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtDescIdDesc(
            username, from, until, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))))
        .map(payment -> new Transaction(payment.getId(), payment.getOrder().getId(),
            payment.getMethod(), payment.getAmount(), payment.getPaidAt()));
    return new PersonalRevenueResponse(date, ZONE.getId(), now,
        methods.stream().map(MethodSummary::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
        methods.stream().mapToLong(MethodSummary::paymentCount).sum(), methods, PageResponse.from(transactions));
  }
}
