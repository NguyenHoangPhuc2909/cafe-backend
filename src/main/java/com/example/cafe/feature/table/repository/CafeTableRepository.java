package com.example.cafe.feature.table.repository;

import com.example.cafe.common.entity.table.CafeTable;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CafeTableRepository extends JpaRepository<CafeTable, Long> {
  Optional<CafeTable> findByQrCode(String qrCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM CafeTable t WHERE t.id = :id")
  Optional<CafeTable> findByIdForUpdate(@Param("id") Long id);

  boolean existsByName(String name);
}
