package com.example.cafe.controller;

import com.example.cafe.dto.request.ProductRequest;
import com.example.cafe.dto.response.ApiResponse;
import com.example.cafe.dto.response.ProductResponse;
import com.example.cafe.entity.Product;
import com.example.cafe.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
  private final ProductService productService;

  // 1. Lấy danh sách tất cả sản phẩm
  @GetMapping
  public ApiResponse<List<ProductResponse>> getAllProducts(){
    return ApiResponse.ok(productService.getAllProducts());
  }

  // 2. Xem chi tiết 1 sản phẩm
  @GetMapping("/{id}")
  public ApiResponse<ProductResponse> getProductById(@PathVariable Long id){
    return ApiResponse.ok(productService.getProductById(id));
  }

  // 3. Thêm sản phẩm mới (Dùng @Valid chặn dữ liệu bẩn)
  @PostMapping
  public ApiResponse<ProductResponse> createProduct(@RequestBody @Valid ProductRequest request){
    return ApiResponse.ok(productService.createProduct(request));
  }

  // 4. Cập nhật thông tin sản phẩm
  @PutMapping("/{id}")
  public ApiResponse<ProductResponse> updateProduct(@PathVariable Long id, @RequestBody @Valid ProductRequest request) {
    return ApiResponse.ok(productService.updateProduct(id, request));
  }

  // 5. Xóa sản phẩm
  @DeleteMapping("/{id}")
  public ApiResponse<String> deleteProduct(@PathVariable Long id) {
    productService.deleteProduct(id);
    return ApiResponse.ok("Đã xóa sản phẩm thành công!");
  }
}
