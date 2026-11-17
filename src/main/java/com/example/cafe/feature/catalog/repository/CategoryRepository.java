package com.example.cafe.feature.catalog.repository;

import com.example.cafe.common.entity.catalog.Category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("SELECT c FROM Category c WHERE c.id = :id")
  java.util.Optional<Category> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
