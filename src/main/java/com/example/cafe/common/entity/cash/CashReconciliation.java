package com.example.cafe.common.entity.cash;

import com.example.cafe.common.entity.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "cash_reconciliations", uniqueConstraints = @UniqueConstraint(
    name = "uk_cash_reconciliation_staff_request", columnNames = {"performed_by", "request_id"}))
public class CashReconciliation {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, updatable = false)
  private LocalDate businessDate;
  @Column(name = "request_id", nullable = false, updatable = false)
  private UUID requestId;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "performed_by", nullable = false, updatable = false)
  private User performedBy;
  @Column(nullable = false, updatable = false)
  private String performedByName;
  @Column(nullable = false, updatable = false)
  private Instant createdAt;
  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal openingAmount;
  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal cashSales;
  @Column(nullable = false, updatable = false)
  private long cashPaymentCount;
  @Column(nullable = false, updatable = false)
  private long lastPaymentId;
  @Column(nullable = false, updatable = false)
  private long revision;
  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal topUps;
  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal payouts;
  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal expectedAmount;
  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal actualAmount;
  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal difference;
  @Column(nullable = false, updatable = false, length = 500)
  private String note;
}
