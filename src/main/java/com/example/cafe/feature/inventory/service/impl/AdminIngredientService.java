package com.example.cafe.feature.inventory.service.impl;

import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.inventory.dto.request.IngredientRequest;
import com.example.cafe.feature.inventory.dto.response.AdminIngredientResponse;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminIngredientService {
  private final IngredientRepository ingredients;

  @Transactional(readOnly = true)
  public List<AdminIngredientResponse> list() {
    var referencedIds = new HashSet<>(ingredients.findReferencedIds());
    return ingredients.findAll(Sort.by("name", "id")).stream()
        .map(item -> response(item, referencedIds.contains(item.getId()))).toList();
  }

  @Transactional
  public AdminIngredientResponse save(Long id, IngredientRequest request) {
    if (request.minThreshold() == null || !Double.isFinite(request.minThreshold()) || request.minThreshold() < 0)
      throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
    Ingredient item = id == null ? new Ingredient() : find(id);
    String name = request.name().strip();
    String unit = request.unit().strip();
    if (ingredients.existsByNameIgnoreCaseAndIdNot(name, id == null ? -1L : id))
      throw new AppException(ErrorCode.INGREDIENT_NAME_EXISTS);
    boolean referenced = id != null && ingredients.isReferenced(id);
    if (id != null && !unit.equals(item.getUnit())) {
      if (system(item)) throw new AppException(ErrorCode.SYSTEM_INGREDIENT_LOCKED);
      if (hasStock(item) || referenced) throw new AppException(ErrorCode.INGREDIENT_UNIT_LOCKED);
    }
    item.setName(name);
    item.setUnit(unit);
    item.setMinThreshold(request.minThreshold());
    return response(ingredients.saveAndFlush(item), referenced);
  }

  @Transactional
  public void delete(Long id) {
    Ingredient item = find(id);
    if (system(item)) throw new AppException(ErrorCode.SYSTEM_INGREDIENT_LOCKED);
    if (hasStock(item) || ingredients.isReferenced(id)) throw new AppException(ErrorCode.INGREDIENT_IN_USE);
    try {
      ingredients.delete(item);
      ingredients.flush();
    } catch (DataIntegrityViolationException e) {
      throw new AppException(ErrorCode.INGREDIENT_IN_USE);
    }
  }

  private Ingredient find(Long id) {
    return ingredients.findByIdForUpdate(id).orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));
  }
  private boolean system(Ingredient item) {
    return "CUP_M".equals(item.getCode()) || "CUP_L".equals(item.getCode());
  }
  private boolean hasStock(Ingredient item) {
    return item.getStockQuantity() == null || item.getStockQuantity() != 0;
  }
  private AdminIngredientResponse response(Ingredient item, boolean referenced) {
    boolean editable = !system(item) && !hasStock(item) && !referenced;
    return new AdminIngredientResponse(item.getId(), item.getName(), item.getUnit(), item.getStockQuantity(),
        item.getMinThreshold(), system(item), editable, editable);
  }
}
