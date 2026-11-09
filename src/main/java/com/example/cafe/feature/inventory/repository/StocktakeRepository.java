package com.example.cafe.feature.inventory.repository;

import com.example.cafe.common.entity.inventory.Stocktake;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StocktakeRepository extends JpaRepository<Stocktake, Long> {
  Optional<Stocktake> findByPerformedBy_IdAndRequestId(Long staffId, UUID requestId);
}
