package com.example.cafe.feature.inventory.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.inventory.dto.response.IngredientOptionResponse;
import com.example.cafe.feature.inventory.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import com.example.cafe.feature.inventory.dto.request.IngredientRequest;
import com.example.cafe.feature.inventory.dto.response.AdminIngredientResponse;
import com.example.cafe.feature.inventory.service.impl.AdminIngredientService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ingredients")
@RequiredArgsConstructor
public class AdminIngredientController {
  private final IngredientService ingredientService;
  private final AdminIngredientService management;

  @GetMapping("/management")
  public ApiResponse<List<AdminIngredientResponse>> list() {
    return ApiResponse.ok(management.list());
  }

  @PostMapping
  public ResponseEntity<ApiResponse<AdminIngredientResponse>> create(@Valid @RequestBody IngredientRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(management.save(null, request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<AdminIngredientResponse> update(@PathVariable("id") Long id, @Valid @RequestBody IngredientRequest request) {
    return ApiResponse.ok(management.save(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable("id") Long id) {
    management.delete(id);
    return ApiResponse.ok(null);
  }

  @GetMapping
  public ApiResponse<List<IngredientOptionResponse>> getRecipeIngredients() {
    return ApiResponse.ok(ingredientService.getAllIngredients().stream()
        .filter(ingredient -> !"CUP_M".equals(ingredient.getCode()) && !"CUP_L".equals(ingredient.getCode()))
        .map(ingredient -> new IngredientOptionResponse(ingredient.getId(), ingredient.getName(), ingredient.getUnit()))
        .toList());
  }
}
