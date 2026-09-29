package com.example.cafe.repository;

import com.example.cafe.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
  // Phục vụ cho chức năng báo cáo đầu/cuối ca.
  @Query("SELECT i FROM Ingredient i WHERE i.stockQuantity <= i.minThreshold")
  List<Ingredient> findLowStockIngredients();
}
