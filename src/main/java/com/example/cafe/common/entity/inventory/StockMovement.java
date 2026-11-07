package com.example.cafe.common.entity.inventory;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "stock_movements",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_stock_movement_staff_request",
            columnNames = {"performed_by", "request_id"}
        )
    }
)
public class StockMovement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_id", updatable = false)
  private UUID requestId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private StockMovementType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingredient_id", updatable = false)
  private Ingredient ingredient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", updatable = false)
  private Product product;

  @Column(name = "quantity_delta", nullable = false, updatable = false)
  private Double quantityDelta;

  @Column(name = "available_before", nullable = false, updatable = false)
  private Double availableBefore;

  @Column(name = "available_after", nullable = false, updatable = false)
  private Double availableAfter;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "performed_by", nullable = false, updatable = false)
  private User performedBy;

  @Column(nullable = false, updatable = false, length = 500)
  private String note;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  private void beforeInsert() {
    if ((ingredient == null) == (product == null)) {
      throw new IllegalStateException(
          "Phải chọn đúng một nguyên liệu hoặc sản phẩm"
      );
    }

    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}