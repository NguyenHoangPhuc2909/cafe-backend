package com.example.cafe.feature.inventory.service.impl;

import com.example.cafe.common.entity.inventory.Stocktake;
import com.example.cafe.common.entity.inventory.StocktakeItem;
import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.inventory.dto.request.StocktakeRequest;
import com.example.cafe.feature.inventory.dto.response.StocktakeResponse;
import com.example.cafe.feature.inventory.repository.*;
import com.example.cafe.feature.inventory.service.StocktakeService;
import com.example.cafe.feature.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StocktakeServiceImpl implements StocktakeService {
  private final StocktakeRepository stocktakes;
  private final IngredientRepository ingredients;
  private final ProductRepository products;
  private final OrderRepository orders;
  private final UserRepository users;

  private void requireClosedOrders() {
    if (orders.existsByStatusIn(List.of(OrderStatus.PENDING, OrderStatus.PREPARING)))
      throw new AppException(ErrorCode.STOCKTAKE_ACTIVE_ORDERS);
  }

  @Transactional
  public List<StocktakeItem> preview() {
    var items = currentItems();
    requireClosedOrders();
    return items;
  }

  // Follow the same product-then-ingredient lock order as order creation.
  private List<StocktakeItem> currentItems() {
    var result = new ArrayList<StocktakeItem>();
    for (var product : products.findAllForStocktake()) {
      result.add(item(null, product.getId(), product.getName(), product.getUnit(),
          product.getStockQuantity() == null ? null : product.getStockQuantity().doubleValue()));
    }
    for (var ingredient : ingredients.findAllForStocktake()) {
      result.add(item(ingredient.getId(), null, ingredient.getName(), ingredient.getUnit(), ingredient.getStockQuantity()));
    }
    return result;
  }

  private StocktakeItem item(Long ingredientId, Long productId, String name, String unit, Double quantity) {
    if (quantity == null || !Double.isFinite(quantity) || quantity < 0)
      throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
    var item = new StocktakeItem();
    item.setIngredientId(ingredientId);
    item.setProductId(productId);
    item.setItemName(name);
    item.setUnit(unit);
    item.setExpectedQuantity(quantity);
    item.setNote("");
    return item;
  }

  @Transactional
  public StocktakeResponse save(StocktakeRequest request, String username) {
    validate(request);
    var staff = users.findByUsernameForUpdate(username)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    var existing = stocktakes.findByPerformedBy_IdAndRequestId(staff.getId(), request.requestId());
    if (existing.isPresent()) {
      var saved = existing.get();
      var lines = new HashMap<String, StocktakeRequest.Line>();
      request.items().forEach(line -> lines.put(key(line.ingredientId(), line.productId()), line));
      boolean same = saved.getNote().equals(request.note().strip())
          && saved.getItems().size() == lines.size()
          && saved.getItems().stream().allMatch(item -> {
            var line = lines.get(key(item.getIngredientId(), item.getProductId()));
            return line != null && Objects.equals(item.getExpectedQuantity(), line.expectedQuantity())
                && Objects.equals(item.getActualQuantity(), line.actualQuantity())
                && item.getNote().equals(line.note().strip());
          });
      if (!same) throw new AppException(ErrorCode.STOCKTAKE_CONFLICT);
      return response(saved, true);
    }
    var current = currentItems();
    requireClosedOrders();
    var byKey = new HashMap<String, StocktakeItem>();
    current.forEach(item -> byKey.put(key(item.getIngredientId(), item.getProductId()), item));
    if (current.size() != request.items().size()) throw new AppException(ErrorCode.STOCKTAKE_STALE);
    for (var line : request.items()) {
      var item = byKey.get(key(line.ingredientId(), line.productId()));
      if (item == null || Double.compare(item.getExpectedQuantity(), line.expectedQuantity()) != 0)
        throw new AppException(ErrorCode.STOCKTAKE_STALE);
      item.setActualQuantity(line.actualQuantity());
      item.setNote(line.note().strip());
      if (line.actualQuantity() != null) item.setDifference(BigDecimal.valueOf(line.actualQuantity())
          .subtract(BigDecimal.valueOf(item.getExpectedQuantity())).doubleValue());
    }
    var report = new Stocktake();
    report.setRequestId(request.requestId());
    report.setPerformedBy(staff);
    report.setPerformedByName(staff.getFullName() == null || staff.getFullName().isBlank()
        ? staff.getUsername() : staff.getFullName());
    report.setCreatedAt(Instant.now());
    report.setNote(request.note().strip());
    report.setItems(current);
    // Reporting only: never mutate stock quantities or create stock movements.
    return response(stocktakes.saveAndFlush(report), true);
  }

  private void validate(StocktakeRequest request) {
    if (request == null || request.requestId() == null || request.note() == null
        || request.note().length() > 500 || request.items() == null
        || request.items().isEmpty() || request.items().size() > 2000)
      throw new AppException(ErrorCode.INVALID_STOCKTAKE);
    var keys = new HashSet<String>();
    boolean checked = false;
    for (var line : request.items()) {
      if (line == null || (line.ingredientId() == null) == (line.productId() == null)
          || (line.ingredientId() != null && line.ingredientId() <= 0)
          || (line.productId() != null && line.productId() <= 0)
          || line.expectedQuantity() == null || !Double.isFinite(line.expectedQuantity())
          || line.expectedQuantity() < 0 || line.note() == null || line.note().length() > 500
          || !keys.add(key(line.ingredientId(), line.productId())))
        throw new AppException(ErrorCode.INVALID_STOCKTAKE);
      if (line.actualQuantity() != null) {
        double quantity = line.actualQuantity();
        if (!Double.isFinite(quantity) || quantity < 0 || (line.productId() != null
            && (quantity != Math.rint(quantity) || quantity > Integer.MAX_VALUE)))
          throw new AppException(ErrorCode.INVALID_STOCKTAKE);
        checked = true;
      }
    }
    if (!checked) throw new AppException(ErrorCode.INVALID_STOCKTAKE);
  }

  private String key(Long ingredientId, Long productId) {
    return ingredientId == null ? "p:" + productId : "i:" + ingredientId;
  }

  @Transactional(readOnly = true)
  public PageResponse<StocktakeResponse> history(int page, int size) {
    return PageResponse.from(stocktakes.findAll(PageRequest.of(Math.max(0, page),
        Math.min(100, Math.max(1, size)), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
        .map(report -> response(report, false)));
  }

  @Transactional(readOnly = true)
  public StocktakeResponse detail(Long id) {
    return response(stocktakes.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.STOCKTAKE_NOT_FOUND)), true);
  }

  private StocktakeResponse response(Stocktake report, boolean detail) {
    return new StocktakeResponse(report.getId(), report.getPerformedBy().getId(),
        report.getPerformedByName(), report.getCreatedAt(), report.getNote(), report.getItems().size(),
        report.getItems().stream().filter(item -> item.getActualQuantity() != null).count(),
        report.getItems().stream().filter(item -> item.getDifference() != null && item.getDifference() != 0).count(),
        detail ? report.getItems() : null);
  }
}
