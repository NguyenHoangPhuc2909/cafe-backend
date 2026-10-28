package com.example.cafe.feature.auth.repository;

import com.example.cafe.common.entity.auth.User;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  // Lấy toàn bộ thông tin User thông qua username
  Optional<User> findByUsername(String username);
}
