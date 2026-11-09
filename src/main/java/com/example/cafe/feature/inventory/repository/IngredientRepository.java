package com.example.cafe.feature.inventory.repository;

import com.example.cafe.common.entity.inventory.Ingredient;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
  // Phục vụ cho chức năng báo cáo đầu/cuối ca.
  @Query("SELECT i FROM Ingredient i WHERE i.stockQuantity <= i.minThreshold")
  List<Ingredient> findLowStockIngredients();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Ingredient i WHERE i.id = :id")
  Optional<Ingredient> findByIdForUpdate(@Param("id") Long id);
}
