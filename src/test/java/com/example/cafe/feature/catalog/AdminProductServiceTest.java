package com.example.cafe.feature.catalog;

import com.example.cafe.common.entity.catalog.Category;
import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.catalog.Recipe;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.enums.DrinkSize;
import com.example.cafe.common.enums.ProductType;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.catalog.dto.request.AdminProductRequest;
import com.example.cafe.feature.catalog.dto.request.ProductRequest;
import com.example.cafe.feature.catalog.repository.CategoryRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.catalog.repository.RecipeRepository;
import com.example.cafe.feature.catalog.service.impl.ProductServiceImpl;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdminProductServiceTest {
  private final RepositoryStub<ProductRepository> products = new RepositoryStub<>(ProductRepository.class);
  private final RepositoryStub<CategoryRepository> categories = new RepositoryStub<>(CategoryRepository.class);
  private final RepositoryStub<RecipeRepository> recipes = new RepositoryStub<>(RecipeRepository.class);
  private final RepositoryStub<IngredientRepository> ingredients = new RepositoryStub<>(IngredientRepository.class);
  private final ProductServiceImpl service = new ProductServiceImpl(products.value, categories.value, recipes.value, ingredients.value);
  private final Product existing = new Product();
  private final Ingredient coffee = new Ingredient();
  private final List<Recipe> storedRecipes = new ArrayList<>();

  @BeforeEach void setup() {
    Category category = new Category();
    category.setId(1L);
    category.setName("Cà phê");
    categories.on("findById", args -> Optional.of(category));
    products.on("save", args -> { Product p = (Product) args[0]; p.setId(10L); return p; });
    products.on("findByIdForUpdate", args -> Optional.of(existing));
    products.on("findById", args -> Optional.of(existing));
    existing.setId(10L);
    existing.setName("Cà phê");
    existing.setPrice(new BigDecimal("30000"));
    existing.setUnit("ly");
    existing.setCategory(category);
    existing.setProductType(ProductType.MADE_TO_ORDER);
    coffee.setId(2L);
    coffee.setName("Cà phê bột");
    coffee.setUnit("gram");
    coffee.setStockQuantity(100.0);
    ingredients.on("findAllById", args -> ((List<?>) args[0]).isEmpty() ? List.of() : List.of(coffee));
    recipes.on("findByProductId", args -> new ArrayList<>(storedRecipes));
    recipes.on("deleteAll", args -> { storedRecipes.clear(); return null; });
    recipes.on("saveAll", args -> {
      for (Object item : (Iterable<?>) args[0]) storedRecipes.add((Recipe) item);
      return new ArrayList<>(storedRecipes);
    });
  }

  private ProductRequest product(boolean sizes, ProductType type) {
    ProductRequest request = new ProductRequest();
    request.setName("Cà phê mới");
    request.setPrice(new BigDecimal("35000"));
    request.setCategoryId(1L);
    request.setUnit("ly");
    request.setSizeEnabled(sizes);
    request.setProductType(type);
    return request;
  }

  private AdminProductRequest.RecipeItem item(DrinkSize size, double amount) {
    return new AdminProductRequest.RecipeItem(2L, amount, size);
  }

  @Test void createsSeparateRecipesForEachSizeWithoutChangingStock() {
    var response = service.createAdminProduct(new AdminProductRequest(product(true, ProductType.MADE_TO_ORDER),
        List.of(item(DrinkSize.L, 30), item(DrinkSize.M, 20))));
    assertEquals("Cà phê mới", response.product().getName());
    assertTrue(response.product().getSizeEnabled());
    assertEquals(List.of(DrinkSize.M, DrinkSize.L), response.recipes().stream().map(r -> r.size()).toList());
    assertEquals(List.of(20.0, 30.0), response.recipes().stream().map(r -> r.quantity()).toList());
    assertEquals("gram", response.recipes().getFirst().unit());
    assertEquals(100.0, coffee.getStockQuantity());
    assertTrue(storedRecipes.stream().allMatch(recipe -> recipe.getProduct().getId().equals(10L)));
  }

  @Test void omittedSizeDefaultsToMForUnsizedProduct() {
    var response = service.createAdminProduct(new AdminProductRequest(product(false, ProductType.MADE_TO_ORDER), List.of(item(null, 20))));
    assertEquals(DrinkSize.M, response.recipes().getFirst().size());
    assertFalse(response.product().getSizeEnabled());
  }

  @Test void updateReplacesPreviousRecipesAndPreservesProductStockAndStatus() {
    Recipe old = new Recipe(); old.setIngredient(coffee); old.setQuantity(10.0);
    storedRecipes.add(old);
    existing.setIsActive(false);
    existing.setStockQuantity(12);
    var response = service.updateAdminProduct(10L, new AdminProductRequest(product(false, ProductType.MADE_TO_ORDER), List.of(item(null, 25))));
    assertEquals(1, storedRecipes.size());
    assertEquals(25.0, storedRecipes.getFirst().getQuantity());
    assertFalse(response.product().getIsActive());
    assertEquals(12, existing.getStockQuantity());
    assertEquals(1, products.calls.get("findByIdForUpdate"));
  }

  @Test void rejectsIncompleteSizedRecipeBeforeDeletingOldRecipes() {
    AppException error = assertThrows(AppException.class, () -> service.updateAdminProduct(10L,
        new AdminProductRequest(product(true, ProductType.MADE_TO_ORDER), List.of(item(DrinkSize.M, 20)))));
    assertEquals(ErrorCode.RECIPE_SIZE_NOT_CONFIGURED, error.getErrorCode());
    assertFalse(recipes.calls.containsKey("deleteAll"));
  }

  @Test void rejectsDuplicateIngredientInSameSizeAndNonPositiveOrNonFiniteQuantities() {
    List<List<AdminProductRequest.RecipeItem>> invalid = List.of(
        List.of(item(null, 20), item(DrinkSize.M, 30)),
        List.of(item(null, 0)), List.of(item(null, -1)),
        List.of(item(null, Double.POSITIVE_INFINITY)), List.of(item(null, Double.NaN)));
    for (var rows : invalid) {
      AppException error = assertThrows(AppException.class, () -> service.createAdminProduct(
          new AdminProductRequest(product(false, ProductType.MADE_TO_ORDER), rows)));
      assertEquals(ErrorCode.PRODUCT_CONFIGURATION_INVALID, error.getErrorCode());
    }
    assertFalse(recipes.calls.containsKey("deleteAll"));
  }

  @Test void rejectsUnknownIngredientsAndAutomaticTakeawayCups() {
    ingredients.on("findAllById", args -> List.of());
    var request = new AdminProductRequest(product(false, ProductType.MADE_TO_ORDER), List.of(item(null, 20)));
    assertEquals(ErrorCode.INGREDIENT_NOT_FOUND, assertThrows(AppException.class,
        () -> service.createAdminProduct(request)).getErrorCode());
    ingredients.on("findAllById", args -> List.of(coffee));
    for (String code : List.of("CUP_M", "CUP_L")) {
      coffee.setCode(code);
      assertEquals(ErrorCode.PRODUCT_CONFIGURATION_INVALID, assertThrows(AppException.class,
          () -> service.createAdminProduct(request)).getErrorCode());
    }
    assertFalse(recipes.calls.containsKey("deleteAll"));
  }

  @Test void packagedProductsHaveNoRecipesAndNoSizes() {
    var response = service.createAdminProduct(new AdminProductRequest(product(true, ProductType.PACKAGED), List.of()));
    assertTrue(response.recipes().isEmpty());
    assertFalse(response.product().getSizeEnabled());
    assertEquals(ErrorCode.PRODUCT_CONFIGURATION_INVALID, assertThrows(AppException.class,
        () -> service.createAdminProduct(new AdminProductRequest(product(false, ProductType.PACKAGED), List.of(item(null, 1))))).getErrorCode());
  }

  @Test void cannotChangeProductTypeAfterCreation() {
    assertEquals(ErrorCode.PRODUCT_CONFIGURATION_INVALID, assertThrows(AppException.class,
        () -> service.updateAdminProduct(10L, new AdminProductRequest(product(false, ProductType.PACKAGED), List.of()))).getErrorCode());
    assertFalse(recipes.calls.containsKey("deleteAll"));
  }
}
