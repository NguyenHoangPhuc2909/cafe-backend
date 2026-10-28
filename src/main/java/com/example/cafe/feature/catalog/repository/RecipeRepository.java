package com.example.cafe.feature.catalog.repository;

import com.example.cafe.common.entity.catalog.Recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
  // Hàm này giúp lấy toàn bộ công thức (những nguyên liệu cần dùng) của 1 món nước cụ thể
  List<Recipe> findByProductId(Long productId);
}
