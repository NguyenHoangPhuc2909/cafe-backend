package com.example.cafe.common.entity.cash;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.enums.CashMovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "cash_movements", uniqueConstraints = @UniqueConstraint(
    name = "uk_cash_movement_staff_request", columnNames = {"performed_by", "request_id"}),
    indexes = @Index(name = "idx_cash_movement_day", columnList = "business_date,created_at"))
public class CashMovement {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "business_date", nullable = false, updatable = false)
  private LocalDate businessDate;
  @Column(name = "request_id", nullable = false, updatable = false)
  private UUID requestId;
  @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 20)
  private CashMovementType type;
  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal amount;
  @Column(nullable = false, updatable = false, length = 500)
  private String note;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "performed_by", nullable = false, updatable = false)
  private User performedBy;
  @Column(nullable = false, updatable = false)
  private String performedByName;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
