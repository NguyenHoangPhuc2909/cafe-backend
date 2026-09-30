package com.example.cafe.service.impl;

import com.example.cafe.dto.request.ProductRequest;
import com.example.cafe.dto.response.ProductResponse;
import com.example.cafe.entity.Category;
import com.example.cafe.entity.Product;
import com.example.cafe.exception.AppException;
import com.example.cafe.exception.ErrorCode;
import com.example.cafe.mapper.ProductMapper;
import com.example.cafe.repository.CategoryRepository;
import com.example.cafe.repository.ProductRepository;
import com.example.cafe.service.ProductService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service 
@RequiredArgsConstructor 
public class ProductServiceImpl implements ProductService {
  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  @Override
  public List<ProductResponse> getAllProducts() {
    return productRepository.findAll().stream()
        .map(ProductMapper::toResponse).collect(Collectors.toList());
  }

  @Override
  public ProductResponse getProductById(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    return ProductMapper.toResponse(product);
  }

  @Override
  public ProductResponse createProduct(ProductRequest request) {
    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

    Product product = ProductMapper.toEntity(request, category);
    product = productRepository.save(product);

    return ProductMapper.toResponse(product);
  }

  @Override
  public ProductResponse updateProduct(Long id, ProductRequest request) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

    product.setName(request.getName());
    product.setDescription(request.getDescription());
    product.setPrice(request.getPrice());
    product.setImageUrl(request.getImageUrl());
    product.setCategory(category);

    product = productRepository.save(product);
    return ProductMapper.toResponse(product);
  }

  @Override
  public void deleteProduct(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    product.setIsActive(false);
    productRepository.save(product);
  }
}
