package com.example.cafe.feature.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record StocktakeRequest(
    @NotNull UUID requestId,
    @NotNull @Size(max = 500) String note,
    @NotEmpty @Size(max = 2000) List<@NotNull @Valid Line> items
) {
  public record Line(Long ingredientId, Long productId,
      @NotNull Double expectedQuantity, Double actualQuantity,
      @NotNull @Size(max = 500) String note) {}
}
