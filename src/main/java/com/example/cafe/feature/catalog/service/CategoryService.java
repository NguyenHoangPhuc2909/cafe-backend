package com.example.cafe.feature.catalog.service;

import com.example.cafe.feature.catalog.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
  List<CategoryResponse> getCategories();
}