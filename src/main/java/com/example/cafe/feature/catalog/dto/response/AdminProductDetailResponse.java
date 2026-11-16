package com.example.cafe.feature.catalog.dto.response;

import com.example.cafe.common.enums.DrinkSize;

import java.util.List;

public record AdminProductDetailResponse(
    ProductResponse product,
    List<RecipeItem> recipes
) {
  public record RecipeItem(
      Long ingredientId,
      String ingredientName,
      String unit,
      Double quantity,
      DrinkSize size
  ) {}
}
