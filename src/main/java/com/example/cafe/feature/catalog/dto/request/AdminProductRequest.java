package com.example.cafe.feature.catalog.dto.request;

import com.example.cafe.common.enums.DrinkSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record AdminProductRequest(
    @NotNull @Valid ProductRequest product,
    @NotNull List<@NotNull @Valid RecipeItem> recipes
) {
  public record RecipeItem(
      @NotNull @Positive Long ingredientId,
      @NotNull @Positive Double quantity,
      DrinkSize size
  ) {
    public RecipeItem {
      size = size == null ? DrinkSize.M : size;
    }
  }
}
