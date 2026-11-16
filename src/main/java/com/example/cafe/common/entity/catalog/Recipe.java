package com.example.cafe.common.entity.catalog;

import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.enums.DrinkSize;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

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

  @Enumerated(EnumType.STRING)
  @Column(name = "drink_size", nullable = false, length = 1)
  @ColumnDefault("'M'")
  private DrinkSize size = DrinkSize.M;
}
