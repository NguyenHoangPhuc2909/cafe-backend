package com.example.cafe.common.entity.order;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.OrderPaymentStatus;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Quan hệ Many-to-One: Nhiều đơn hàng do 1 user đặt
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "table_session_id")
  private TableSession tableSession;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  // Trạng thái đơn hàng: PENDING, COMPLETED, CANCELLED...
  @Column(nullable = false, length = 50)
  private String status = "PENDING";

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false, length = 20)
  private OrderPaymentStatus paymentStatus = OrderPaymentStatus.UNPAID;

  @Column(name = "payment_expires_at")
  private Instant paymentExpiresAt;
}
