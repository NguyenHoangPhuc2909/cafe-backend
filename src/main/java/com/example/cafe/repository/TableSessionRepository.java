package com.example.cafe.repository;

import com.example.cafe.entity.TableSession;
import com.example.cafe.entity.TableSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
  Optional<TableSession> findByTable_IdAndStatus(Long tableId, TableSessionStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM TableSession s WHERE s.id = :id")
  Optional<TableSession> findByIdForUpdate(@Param("id") Long id);
}
