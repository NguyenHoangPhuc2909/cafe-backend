package com.example.cafe.common.entity.inventory;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.enums.StockReservationStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_reservations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservation_order_product", columnNames = {"order_id", "product_id"}),
        @UniqueConstraint(name = "uk_reservation_order_ingredient", columnNames = {"order_id", "ingredient_id"})
    })
public class StockReservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingredient_id")
  private Ingredient ingredient;

  @Column(nullable = false)
  private Double quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StockReservationStatus status = StockReservationStatus.HELD;

  @PrePersist
  @PreUpdate
  private void validateReservation() {

    if ((product == null) == (ingredient == null)) {
      throw new IllegalStateException(
          "Phải chọn đúng một sản phẩm hoặc nguyên liệu"
      );
    }

    if (quantity == null || !Double.isFinite(quantity) || quantity <= 0) {
      throw new IllegalStateException(
          "Số lượng giữ phải dương và hữu hạn"
      );
    }

    if (product != null && (quantity != Math.rint(quantity) || quantity > Integer.MAX_VALUE)) {
      throw new IllegalStateException(
          "Số lượng sản phẩm phải là số nguyên hợp lệ"
      );
    }
  }
}
