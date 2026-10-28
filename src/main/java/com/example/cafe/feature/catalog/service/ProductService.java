package com.example.cafe.feature.catalog.service;

import com.example.cafe.feature.catalog.dto.request.ProductRequest;
import com.example.cafe.feature.catalog.dto.response.ProductResponse;


import java.util.List;

public interface ProductService {
  List<ProductResponse> getAllProducts();

  ProductResponse getProductById(Long id);

  ProductResponse createProduct(ProductRequest request);

  ProductResponse updateProduct(Long id, ProductRequest request);

  void deleteProduct(Long id);

  List<ProductResponse> getPublicMenu();

  ProductResponse updateProductActive(Long id, boolean active);
}