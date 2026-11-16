package com.example.cafe.feature.catalog.service;

import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.catalog.dto.request.AdminProductRequest;
import com.example.cafe.feature.catalog.dto.request.ProductRequest;
import com.example.cafe.feature.catalog.dto.response.AdminProductDetailResponse;
import com.example.cafe.feature.catalog.dto.response.ProductResponse;


import java.util.List;

public interface ProductService {
  List<ProductResponse> getAllProducts();

  ProductResponse getProductById(Long id);

  ProductResponse createProduct(ProductRequest request);

  ProductResponse updateProduct(Long id, ProductRequest request);

  List<ProductResponse> getPublicMenu();

  ProductResponse updateProductActive(Long id, boolean active);

  AdminProductDetailResponse getAdminProduct(Long id);

  AdminProductDetailResponse createAdminProduct(AdminProductRequest request);

  AdminProductDetailResponse updateAdminProduct(Long id, AdminProductRequest request);

  PageResponse<ProductResponse> getAdminProducts(
      String keyword,
      Long categoryId,
      Boolean active,
      int page,
      int size
  );
}
