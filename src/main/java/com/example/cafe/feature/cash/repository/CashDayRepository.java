package com.example.cafe.feature.cash.repository;

import com.example.cafe.common.entity.cash.CashDay;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface CashDayRepository extends JpaRepository<CashDay, LocalDate> {
  // A unique date and PostgreSQL upsert also serialize the first writes of a new day.
  @Modifying(flushAutomatically = true)
  @Query(value = "INSERT INTO cash_days (business_date, opening_amount, revision) VALUES (:date, 500000, 0) ON CONFLICT (business_date) DO NOTHING", nativeQuery = true)
  void ensureDay(@Param("date") LocalDate date);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT d FROM CashDay d WHERE d.businessDate = :date")
  Optional<CashDay> findForUpdate(@Param("date") LocalDate date);
}
