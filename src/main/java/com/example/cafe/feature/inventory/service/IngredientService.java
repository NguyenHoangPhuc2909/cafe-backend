package com.example.cafe.feature.inventory.service;

import com.example.cafe.common.entity.inventory.Ingredient;


import java.util.List;

public interface IngredientService {
  List<Ingredient> getLowStockIngredients();
  List<Ingredient> getAllIngredients();
}
