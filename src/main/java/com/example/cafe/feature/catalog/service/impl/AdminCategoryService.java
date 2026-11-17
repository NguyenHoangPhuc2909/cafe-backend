package com.example.cafe.feature.catalog.service.impl;

import com.example.cafe.common.entity.catalog.Category;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.catalog.dto.request.CategoryRequest;
import com.example.cafe.feature.catalog.dto.response.CategoryResponse;
import com.example.cafe.feature.catalog.repository.CategoryRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AdminCategoryService {
  private final CategoryRepository categories;
  private final ProductRepository products;

  @Transactional
  public CategoryResponse save(Long id, CategoryRequest request) {
    Category category = id == null ? new Category() : categories.findByIdForUpdate(id)
        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    String name = request.name().strip();
    if (categories.existsByNameIgnoreCaseAndIdNot(name, id == null ? -1L : id))
      throw new AppException(ErrorCode.CATEGORY_NAME_EXISTS);
    category.setName(name);
    Category saved = categories.saveAndFlush(category);
    return new CategoryResponse(saved.getId(), saved.getName());
  }

  @Transactional
  public void delete(Long id) {
    Category category = categories.findByIdForUpdate(id)
        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    if (products.existsByCategory_Id(id)) throw new AppException(ErrorCode.CATEGORY_IN_USE);
    try {
      categories.delete(category);
      categories.flush();
    } catch (DataIntegrityViolationException e) {
      throw new AppException(ErrorCode.CATEGORY_IN_USE);
    }
  }
}
