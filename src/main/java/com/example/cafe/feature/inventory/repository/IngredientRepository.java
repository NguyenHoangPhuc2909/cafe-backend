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
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

  @Query("""
      SELECT i.id FROM Ingredient i
      WHERE EXISTS (SELECT r.id FROM Recipe r WHERE r.ingredient = i)
         OR EXISTS (SELECT m.id FROM StockMovement m WHERE m.ingredient = i)
         OR EXISTS (SELECT s.id FROM StockReservation s WHERE s.ingredient = i)
      """)
  List<Long> findReferencedIds();

  @Query("""
      SELECT (COUNT(i) > 0) FROM Ingredient i WHERE i.id = :id AND (
          EXISTS (SELECT r.id FROM Recipe r WHERE r.ingredient = i)
          OR EXISTS (SELECT m.id FROM StockMovement m WHERE m.ingredient = i)
          OR EXISTS (SELECT s.id FROM StockReservation s WHERE s.ingredient = i))
      """)
  boolean isReferenced(@Param("id") Long id);
  // Phục vụ cho chức năng báo cáo đầu/cuối ca.
  @Query("SELECT i FROM Ingredient i WHERE i.stockQuantity <= i.minThreshold")
  List<Ingredient> findLowStockIngredients();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Ingredient i WHERE i.id = :id")
  Optional<Ingredient> findByIdForUpdate(@Param("id") Long id);

  @Query("SELECT i.id FROM Ingredient i WHERE i.code = :code")
  Optional<Long> findIdByCode(@Param("code") String code);
}
