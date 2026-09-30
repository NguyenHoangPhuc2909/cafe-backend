package com.example.cafe.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;

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

  // Quan hệ Many-to-Many: Một món trong đơn có thể gọi nhiều topping, 1 topping thuộc nhiều món
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "order_item_toppings",
  joinColumns = @JoinColumn(name = "order_item_id"),
  inverseJoinColumns = @JoinColumn(name = "topping_id"))
  private ArrayList<Topping> toppings;
}
