package com.example.cafe.feature.catalog.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.catalog.dto.request.ProductActiveRequest;
import com.example.cafe.feature.catalog.dto.response.ProductResponse;
import com.example.cafe.feature.catalog.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/products")
@RequiredArgsConstructor
public class StaffProductController {

  private final ProductService productService;

  @GetMapping
  public ApiResponse<List<ProductResponse>> getAllProducts() {
    return ApiResponse.ok(productService.getAllProducts());
  }

  @PatchMapping("/{id}/active")
  public ApiResponse<ProductResponse> updateProductActive(
      @PathVariable("id") Long id,
      @Valid @RequestBody ProductActiveRequest request
  ) {
    return ApiResponse.ok(
        productService.updateProductActive(id, request.getActive())
    );
  }
}