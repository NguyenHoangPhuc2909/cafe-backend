package com.example.cafe.feature.cash.repository;

import com.example.cafe.common.entity.cash.CashMovement;
import com.example.cafe.common.enums.CashMovementType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
  Optional<CashMovement> findByPerformedBy_IdAndRequestId(Long staffId, UUID requestId);
  Page<CashMovement> findByBusinessDateOrderByCreatedAtDescIdDesc(LocalDate date, Pageable pageable);
  interface MovementTotal {
    CashMovementType getType();
    BigDecimal getAmount();
  }
  @Query("SELECT m.type AS type, SUM(m.amount) AS amount FROM CashMovement m WHERE m.businessDate = :date GROUP BY m.type")
  List<MovementTotal> summarizeDay(@Param("date") LocalDate date);
}
