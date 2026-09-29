package com.example.cafe.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "ingredients")
public class Ingredient {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name; // Tên nguyên liệu (Cafe, Trà đen, Sữa tươi...)

  @Column(nullable = false)
  private String unit; // Đơn vị tính (gram, ml, lít, cái...)

  @Column(name = "stock_quantity", nullable = false)
  private Double stockQuantity = 0.0; // Tồn kho thực tế

  @Column(name = "min_threshold", nullable = false)
  private Double minThreshold = 0.0; // Mức báo động sắp hết (VD: Dưới 500ml thì báo đỏ)
}
