package com.example.cafe.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
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
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  // Trạng thái đơn hàng: PENDING, COMPLETED, CANCELLED...
  @Column(nullable = false, length = 50)
  private String status = "PENDING";

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;
}
