package com.example.cafe.feature.catalog.repository;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.enums.ProductType;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.id = :id")
  Optional<Product> findByIdForUpdate(@Param("id") Long id);

  List<Product> findByIsActiveTrueOrderByIdAsc();

  List<Product> findByProductTypeOrderByIdAsc(ProductType productType);

  @EntityGraph(attributePaths = "category")
  @Query("""
    SELECT p FROM Product p
    WHERE (:keyword = '' OR LOCATE(LOWER(:keyword), LOWER(p.name)) > 0)
      AND (:categoryId IS NULL OR p.category.id = :categoryId)
      AND (:active IS NULL OR COALESCE(p.isActive, false) = :active)
    """)
  Page<Product> findForAdmin(
      @Param("keyword") String keyword,
      @Param("categoryId") Long categoryId,
      @Param("active") Boolean active,
      Pageable pageable
  );
}
