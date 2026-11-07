package com.example.cafe.feature.inventory.repository;

import com.example.cafe.common.entity.inventory.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
  Optional<StockMovement> findByPerformedBy_IdAndRequestId(
      Long staffId,
      UUID requestId
  );
}