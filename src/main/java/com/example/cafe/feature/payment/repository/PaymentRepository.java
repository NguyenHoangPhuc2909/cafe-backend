package com.example.cafe.feature.payment.repository;


import com.example.cafe.common.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import com.example.cafe.common.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByOrder_Id(Long orderId);

  interface CashTotal {
    BigDecimal getAmount();
    long getPaymentCount();
    long getLastPaymentId();
  }

  @Query("""
      SELECT COALESCE(SUM(p.amount), 0) AS amount, COUNT(p) AS paymentCount,
        COALESCE(MAX(p.id), 0) AS lastPaymentId
      FROM Payment p WHERE p.method = com.example.cafe.common.enums.PaymentMethod.CASH
        AND p.paidAt >= :from AND p.paidAt < :until
      """)
  CashTotal summarizeSharedCashDay(@Param("from") Instant from, @Param("until") Instant until);

  interface MethodTotal {
    PaymentMethod getMethod();
    long getPaymentCount();
    BigDecimal getAmount();
  }

  @Query("""
      SELECT p.method AS method, COUNT(p) AS paymentCount, SUM(p.amount) AS amount
      FROM Payment p WHERE p.confirmedBy.username = :username
        AND p.paidAt >= :from AND p.paidAt < :until
      GROUP BY p.method
      """)
  List<MethodTotal> summarizePersonalDay(@Param("username") String username,
      @Param("from") Instant from, @Param("until") Instant until);

  Page<Payment> findByConfirmedBy_UsernameAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtDescIdDesc(
      String username, Instant from, Instant until, Pageable pageable);
}
