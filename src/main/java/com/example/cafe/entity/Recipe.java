package com.example.cafe.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "recipes")
public class Recipe {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product; // Của món nào? (VD: Trà sữa)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingredient_id", nullable = false)
  private Ingredient ingredient; // Cần dùng nguyên liệu gì? (VD: Trà đen)

  @Column(nullable = false)
  private Double quantity; // Lượng dùng bao nhiêu? (VD: 50 gram)
}
