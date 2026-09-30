package com.example.cafe.controller;

import com.example.cafe.dto.response.ApiResponse;
import com.example.cafe.entity.Ingredient;
import com.example.cafe.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {
  private final IngredientService ingredientService;

  @GetMapping("/low-stock")
  public ApiResponse<List<Ingredient>> getLowStockIngredients() {
    return ApiResponse.ok(ingredientService.getLowStockIngredients());
  }

  @GetMapping()
  public ApiResponse<List<Ingredient>> getAllIngredients() {
    return ApiResponse.ok(ingredientService.getAllIngredients());
  }
}
