package com.example.cafe.feature.inventory.service.impl;

import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.inventory.service.IngredientService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {
  private final IngredientRepository ingredientRepository;

  @Override
  public List<Ingredient> getLowStockIngredients() {
    return ingredientRepository.findLowStockIngredients();
  }

  @Override
  public List<Ingredient> getAllIngredients() {
    return ingredientRepository.findAll();
  }
}
