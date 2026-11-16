package com.example.cafe.feature.catalog.service.impl;

import com.example.cafe.feature.catalog.dto.response.CategoryResponse;
import com.example.cafe.feature.catalog.repository.CategoryRepository;
import com.example.cafe.feature.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  private final CategoryRepository categoryRepository;

  @Override
  @Transactional(readOnly = true)
  public List<CategoryResponse> getCategories() {
    return categoryRepository.findAll(Sort.by("name", "id"))
        .stream()
        .map(category -> new CategoryResponse(
            category.getId(),
            category.getName()))
        .toList();
  }
}