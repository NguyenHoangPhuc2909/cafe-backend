package com.example.cafe.common.entity.order;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.enums.DrinkSize;
import com.example.cafe.common.enums.DrinkOptionLevel;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Quan hệ Many-to-One: Nhiều chi tiết đơn thuộc về cùng một đơn hàng
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  // Quan hệ Many-to-One: Nhiều chi tiết đơn có thể tham chiếu đến cùng một sản phẩm
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false)
  private Integer quantity;

  // Giá của sản phẩm tại thời điểm mua (để phòng trường hợp sau này Product đổi giá)
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(name = "drink_size", length = 1)
  private DrinkSize size;

  @Enumerated(EnumType.STRING)
  @Column(name = "ice_level", length = 10)
  private DrinkOptionLevel iceLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "sweetness_level", length = 10)
  private DrinkOptionLevel sweetnessLevel;

}
