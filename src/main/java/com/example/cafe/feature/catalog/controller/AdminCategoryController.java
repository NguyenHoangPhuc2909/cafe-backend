package com.example.cafe.feature.catalog.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.catalog.dto.response.CategoryResponse;
import com.example.cafe.feature.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

  private final CategoryService categoryService;

  @GetMapping
  public ApiResponse<List<CategoryResponse>> getCategories() {
    return ApiResponse.ok(categoryService.getCategories());
  }
}