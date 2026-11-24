package com.example.cafe.feature.table.repository;

import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.TableSessionStatus;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
  Optional<TableSession> findByTable_IdAndStatus(Long tableId, TableSessionStatus status);

  boolean existsByTable_Id(Long tableId);

  List<TableSession> findByStatus(TableSessionStatus status);

  @Query("SELECT DISTINCT s.table.id FROM TableSession s")
  List<Long> findDistinctTableIds();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM TableSession s WHERE s.id = :id")
  Optional<TableSession> findByIdForUpdate(@Param("id") Long id);
}
