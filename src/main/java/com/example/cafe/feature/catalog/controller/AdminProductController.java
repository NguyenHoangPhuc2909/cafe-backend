package com.example.cafe.feature.catalog.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.catalog.dto.request.AdminProductRequest;
import com.example.cafe.feature.catalog.dto.response.AdminProductDetailResponse;
import com.example.cafe.feature.catalog.dto.response.ProductResponse;
import com.example.cafe.feature.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

  private final ProductService productService;

  @GetMapping("/{id}")
  public ApiResponse<AdminProductDetailResponse> getProduct(@PathVariable("id") Long id) {
    return ApiResponse.ok(productService.getAdminProduct(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<AdminProductDetailResponse> createProduct(@Valid @RequestBody AdminProductRequest request) {
    return ApiResponse.ok(productService.createAdminProduct(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<AdminProductDetailResponse> updateProduct(
      @PathVariable("id") Long id, @Valid @RequestBody AdminProductRequest request) {
    return ApiResponse.ok(productService.updateAdminProduct(id, request));
  }

  @GetMapping
  public ApiResponse<PageResponse<ProductResponse>> getProducts(
      @RequestParam(name = "keyword", defaultValue = "") String keyword,
      @RequestParam(name = "categoryId", required = false) Long categoryId,
      @RequestParam(name = "active", required = false) Boolean active,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    return ApiResponse.ok(
        productService.getAdminProducts(
            keyword, categoryId, active, page, size));
  }
}
