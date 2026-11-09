package com.example.cafe.feature.cash.repository;

import com.example.cafe.common.entity.cash.CashReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CashReconciliationRepository extends JpaRepository<CashReconciliation, Long> {
  Optional<CashReconciliation> findByPerformedBy_IdAndRequestId(Long staffId, UUID requestId);
}
