package com.example.cafe.service;

import com.example.cafe.entity.Ingredient;

import java.util.List;

public interface IngredientService {
  List<Ingredient> getLowStockIngredients();
  List<Ingredient> getAllIngredients();
}
