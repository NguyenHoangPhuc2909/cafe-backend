package com.example.cafe.feature.inventory.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.inventory.dto.response.IngredientOptionResponse;
import com.example.cafe.feature.inventory.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ingredients")
@RequiredArgsConstructor
public class AdminIngredientController {
  private final IngredientService ingredientService;

  @GetMapping
  public ApiResponse<List<IngredientOptionResponse>> getRecipeIngredients() {
    return ApiResponse.ok(ingredientService.getAllIngredients().stream()
        .filter(ingredient -> !"CUP_M".equals(ingredient.getCode()) && !"CUP_L".equals(ingredient.getCode()))
        .map(ingredient -> new IngredientOptionResponse(ingredient.getId(), ingredient.getName(), ingredient.getUnit()))
        .toList());
  }
}
