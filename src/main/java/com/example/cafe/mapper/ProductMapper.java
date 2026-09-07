package com.example.cafe.mapper;

import com.example.cafe.dto.request.ProductRequest;
import com.example.cafe.dto.response.ProductResponse;
import com.example.cafe.entity.Category;
import com.example.cafe.entity.Product;

public class ProductMapper {
  public static ProductResponse toResponse(Product product){
    if(product == null) return null;

    return ProductResponse.builder()
    .id(product.getId())
        .name(product.getName())
        .description(product.getDescription())
        .price(product.getPrice())
        .imageUrl(product.getImageUrl())
        .isActive(product.getIsActive())
        .categoryName(product.getCategory() != null ?
        product.getCategory().getName() : null).build();
  }

  public static Product toEntity(ProductRequest request, Category category) {
    if (request == null) {
      return null;
    }
    Product product = new Product();
    product.setName(request.getName());
    product.setDescription(request.getDescription());
    product.setPrice(request.getPrice());
    product.setImageUrl(request.getImageUrl());

    // Gán object Category vào Product
    product.setCategory(category);

    // Mặc định khi tạo mới thì sản phẩm sẽ kích hoạt luôn
    product.setIsActive(true);

    return product;}
}
