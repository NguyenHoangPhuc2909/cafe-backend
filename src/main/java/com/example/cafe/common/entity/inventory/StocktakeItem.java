package com.example.cafe.common.entity.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Snapshot names and units so historical reports survive catalog changes.
@Getter
@Setter
@Embeddable
public class StocktakeItem {
  private Long ingredientId;
  private Long productId;
  @Column(nullable = false)
  private String itemName;
  private String unit;
  @Column(nullable = false)
  private Double expectedQuantity;
  private Double actualQuantity;
  private Double difference;
  @Column(nullable = false, length = 500)
  private String note;
}
