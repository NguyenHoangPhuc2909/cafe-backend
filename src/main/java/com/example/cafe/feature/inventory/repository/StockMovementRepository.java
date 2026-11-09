package com.example.cafe.feature.inventory.repository;

import com.example.cafe.common.entity.inventory.StockMovement;
import com.example.cafe.common.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {
  Optional<StockMovement> findByPerformedBy_IdAndRequestId(
      Long staffId,
      UUID requestId
  );

  @EntityGraph(attributePaths = {
      "ingredient",
      "product",
      "performedBy"
  })
  Page<StockMovement> findByType(StockMovementType type, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {
      "ingredient",
      "product",
      "performedBy"
  })
  Page<StockMovement> findAll(Specification<StockMovement> spec, Pageable pageable);
}