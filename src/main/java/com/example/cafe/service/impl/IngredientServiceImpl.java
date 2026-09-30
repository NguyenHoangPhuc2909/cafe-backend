package com.example.cafe.service.impl;

import com.example.cafe.entity.Ingredient;
import com.example.cafe.repository.IngredientRepository;
import com.example.cafe.service.IngredientService;
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
