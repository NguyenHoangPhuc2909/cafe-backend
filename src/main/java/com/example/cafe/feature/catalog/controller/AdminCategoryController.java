package com.example.cafe.feature.catalog.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.catalog.dto.response.CategoryResponse;
import com.example.cafe.feature.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.example.cafe.feature.catalog.dto.request.CategoryRequest;
import com.example.cafe.feature.catalog.service.impl.AdminCategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

  private final CategoryService categoryService;
  private final AdminCategoryService adminCategoryService;

  @PostMapping
  public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(adminCategoryService.save(null, request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<CategoryResponse> update(@PathVariable("id") Long id, @Valid @RequestBody CategoryRequest request) {
    return ApiResponse.ok(adminCategoryService.save(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable("id") Long id) {
    adminCategoryService.delete(id);
    return ApiResponse.ok(null);
  }

  @GetMapping
  public ApiResponse<List<CategoryResponse>> getCategories() {
    return ApiResponse.ok(categoryService.getCategories());
  }
}
