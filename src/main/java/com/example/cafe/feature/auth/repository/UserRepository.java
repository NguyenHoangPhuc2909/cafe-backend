package com.example.cafe.feature.auth.repository;

import com.example.cafe.common.entity.auth.User;


import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  // Lấy toàn bộ thông tin User thông qua username
  Optional<User> findByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.username = :username")
  Optional<User> findByUsernameForUpdate(@Param("username") String username);

  Page<User> findByRole(String role, Pageable pageable);
  boolean existsByEmailAndIdNot(String email, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<User> findByIdAndRole(Long id, String role);
}
