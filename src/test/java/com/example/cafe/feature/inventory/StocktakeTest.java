package com.example.cafe.feature.inventory;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.inventory.*;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.inventory.dto.request.StocktakeRequest;
import com.example.cafe.feature.inventory.repository.*;
import com.example.cafe.feature.inventory.service.impl.StocktakeServiceImpl;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.support.RepositoryStub;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StocktakeTest {
  private final RepositoryStub<StocktakeRepository> reports = new RepositoryStub<>(StocktakeRepository.class);
  private final RepositoryStub<IngredientRepository> ingredients = new RepositoryStub<>(IngredientRepository.class);
  private final RepositoryStub<ProductRepository> products = new RepositoryStub<>(ProductRepository.class);
  private final RepositoryStub<OrderRepository> orders = new RepositoryStub<>(OrderRepository.class);
  private final RepositoryStub<UserRepository> users = new RepositoryStub<>(UserRepository.class);
  private final StocktakeServiceImpl service = new StocktakeServiceImpl(reports.value, ingredients.value, products.value, orders.value, users.value);
  private final Ingredient coffee = new Ingredient();
  private final Product bottle = new Product();
  private Stocktake saved;

  @BeforeEach void setup() {
    coffee.setId(3L); coffee.setName("Cà phê"); coffee.setUnit("g"); coffee.setStockQuantity(100.0);
    bottle.setId(2L); bottle.setName("Nước chai"); bottle.setUnit("chai"); bottle.setStockQuantity(10);
    var staff = new User(); staff.setId(7L); staff.setUsername("cashier"); staff.setFullName("Nhân viên");
    users.on("findByUsernameForUpdate", args -> { assertEquals("cashier", args[0]); return Optional.of(staff); });
    products.on("findAllForStocktake", args -> List.of(bottle));
    ingredients.on("findAllForStocktake", args -> List.of(coffee));
    orders.on("existsByStatusIn", args -> false);
    reports.on("findByPerformedBy_IdAndRequestId", args -> { assertEquals(7L, args[0]); return Optional.ofNullable(saved); });
    reports.on("saveAndFlush", args -> { saved = (Stocktake) args[0]; saved.setId(1L); return saved; });
  }

  private StocktakeRequest request(Double coffeeCount, Double bottleCount) {
    return new StocktakeRequest(UUID.randomUUID(), " Cuối buổi ", List.of(
        new StocktakeRequest.Line(3L, null, 100.0, coffeeCount, " Hao hụt "),
        new StocktakeRequest.Line(null, 2L, 10.0, bottleCount, "")));
  }

  @Test void recordsZeroAndUncheckedSeparatelyWithoutChangingStock() {
    var result = service.save(request(0.0, null), "cashier");
    assertEquals(1, result.checkedItems()); assertEquals(1, result.differenceItems());
    var line = result.items().stream().filter(item -> item.getIngredientId() != null).findFirst().orElseThrow();
    assertEquals(0.0, line.getActualQuantity()); assertEquals(-100.0, line.getDifference());
    assertEquals("Hao hụt", line.getNote()); assertEquals("Cuối buổi", result.note());
    assertNull(result.items().getFirst().getActualQuantity());
    assertEquals(100.0, coffee.getStockQuantity()); assertEquals(10, bottle.getStockQuantity());
    assertEquals("Nhân viên", result.performedByName());
  }

  @Test void sameRequestCanBeRetriedEvenWhenNewOrdersExist() {
    var request = request(99.5, 10.0);
    var first = service.save(request, "cashier");
    orders.on("existsByStatusIn", args -> true);
    var second = service.save(request, "cashier");
    assertEquals(first.id(), second.id()); assertEquals(1, reports.calls.get("saveAndFlush"));
  }

  @Test void changedContentCannotReuseAnExistingRequestId() {
    var request = request(99.5, 10.0); service.save(request, "cashier");
    var changed = new StocktakeRequest(request.requestId(), "Khác", request.items());
    assertError(ErrorCode.STOCKTAKE_CONFLICT, () -> service.save(changed, "cashier"));
  }

  @Test void unfinishedOrdersBlockPreviewAndSaving() {
    orders.on("existsByStatusIn", args -> true);
    assertError(ErrorCode.STOCKTAKE_ACTIVE_ORDERS, () -> service.preview());
    assertError(ErrorCode.STOCKTAKE_ACTIVE_ORDERS, () -> service.save(request(100.0, 10.0), "cashier"));
    assertNull(saved);
  }

  @Test void changedInventoryRequiresReconciliation() {
    var request = request(100.0, 10.0); coffee.setStockQuantity(110.0);
    assertError(ErrorCode.STOCKTAKE_STALE, () -> service.save(request, "cashier")); assertNull(saved);
  }

  @Test void catalogChangesCannotSilentlyDropUncheckedItems() {
    var request = request(100.0, 10.0);
    products.on("findAllForStocktake", args -> List.of());
    assertError(ErrorCode.STOCKTAKE_STALE, () -> service.save(request, "cashier"));
  }

  @Test void rejectsEmptyCountsNegativeNonFiniteAndFractionalPackagedCounts() {
    for (var request : List.of(request(null, null), request(-1.0, null), request(Double.NaN, null),
        request(Double.POSITIVE_INFINITY, null), request(null, 1.5), request(null, 2147483648.0)))
      assertError(ErrorCode.INVALID_STOCKTAKE, () -> service.save(request, "cashier"));
    assertFalse(reports.calls.containsKey("saveAndFlush"));
  }

  @Test void rejectsDuplicateTargetsAndAmbiguousTargets() {
    var line = request(100.0, null).items().getFirst();
    assertError(ErrorCode.INVALID_STOCKTAKE, () -> service.save(
        new StocktakeRequest(UUID.randomUUID(), "", List.of(line, line)), "cashier"));
    assertError(ErrorCode.INVALID_STOCKTAKE, () -> service.save(new StocktakeRequest(UUID.randomUUID(), "", List.of(
        new StocktakeRequest.Line(3L, 2L, 100.0, 100.0, ""))), "cashier"));
  }

  @Test void historicalNamesAreIndependentOfCatalogChanges() {
    service.save(request(99.5, 10.0), "cashier");
    coffee.setName("Tên mới"); coffee.setUnit("kg");
    reports.on("findById", args -> Optional.of(saved));
    var line = service.detail(1L).items().stream().filter(item -> item.getIngredientId() != null).findFirst().orElseThrow();
    assertEquals("Cà phê", line.getItemName()); assertEquals("g", line.getUnit());
  }

  private void assertError(ErrorCode expected, Runnable action) {
    assertEquals(expected, assertThrows(AppException.class, action::run).getErrorCode());
  }
}
