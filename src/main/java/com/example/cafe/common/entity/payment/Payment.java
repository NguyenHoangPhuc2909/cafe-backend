package com.example.cafe.common.entity.payment;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false, unique = true, updatable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private PaymentMethod method;

  @Column(nullable = false, precision = 12, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "paid_at", nullable = false, updatable = false)
  private Instant paidAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "confirmed_by",
      nullable = false,
      updatable = false
  )
  private User confirmedBy;
}
