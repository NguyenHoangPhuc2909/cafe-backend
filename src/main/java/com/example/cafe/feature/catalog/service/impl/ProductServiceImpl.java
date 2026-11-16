package com.example.cafe.feature.catalog.service.impl;

import com.example.cafe.common.entity.catalog.Category;
import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.catalog.Recipe;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.enums.DrinkSize;
import com.example.cafe.common.enums.ProductType;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.catalog.dto.request.ProductRequest;
import com.example.cafe.feature.catalog.dto.request.AdminProductRequest;
import com.example.cafe.feature.catalog.dto.response.AdminProductDetailResponse;
import com.example.cafe.feature.catalog.dto.response.ProductResponse;
import com.example.cafe.feature.catalog.mapper.ProductMapper;
import com.example.cafe.feature.catalog.repository.CategoryRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.catalog.repository.RecipeRepository;
import com.example.cafe.feature.catalog.service.ProductService;
import com.example.cafe.feature.inventory.repository.IngredientRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final RecipeRepository recipeRepository;
  private final IngredientRepository ingredientRepository;

  @Override
  @Transactional(readOnly = true)
  public List<ProductResponse> getAllProducts() {
    return productRepository.findAll().stream()
        .map(ProductMapper::toResponse).collect(Collectors.toList());
  }

  @Override
  public ProductResponse getProductById(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    return ProductMapper.toResponse(product);
  }

  @Override
  @Transactional
  public ProductResponse createProduct(ProductRequest request) {
    return ProductMapper.toResponse(createProductEntity(request));
  }

  private Product createProductEntity(ProductRequest request) {
    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

    Product product = ProductMapper.toEntity(request, category);
    product = productRepository.save(product);

    return product;
  }

  @Override
  @Transactional
  public ProductResponse updateProduct(Long id, ProductRequest request) {
    return ProductMapper.toResponse(updateProductEntity(id, request));
  }

  private Product updateProductEntity(Long id, ProductRequest request) {
    Product product = productRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

    if (request.getProductType() != product.getProductType()) {
      throw new AppException(ErrorCode.PRODUCT_CONFIGURATION_INVALID);
    }

    product.setName(request.getName());
    product.setDescription(request.getDescription());
    product.setPrice(request.getPrice());
    product.setImageUrl(request.getImageUrl());
    product.setCategory(category);
    if (product.getProductType() == ProductType.PACKAGED) {
      product.setSizeEnabled(false);
    } else if (request.getSizeEnabled() != null) {
      product.setSizeEnabled(request.getSizeEnabled());
    }
    if (request.getUnit() != null) {
      product.setUnit(
          request.getUnit().isBlank()
              ? null
              : request.getUnit().strip()
      );
    }

    product = productRepository.save(product);
    return product;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminProductDetailResponse getAdminProduct(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    return toAdminResponse(product, recipeRepository.findByProductId(id));
  }

  @Override
  @Transactional
  public AdminProductDetailResponse createAdminProduct(AdminProductRequest request) {
    Product product = createProductEntity(request.product());
    return saveRecipes(product, request.recipes());
  }

  @Override
  @Transactional
  public AdminProductDetailResponse updateAdminProduct(Long id, AdminProductRequest request) {
    Product product = updateProductEntity(id, request.product());
    return saveRecipes(product, request.recipes());
  }

  private record RecipeIdentity(Long ingredientId, DrinkSize size) {}

  private AdminProductDetailResponse saveRecipes(Product product, List<AdminProductRequest.RecipeItem> items) {
    var sizes = EnumSet.noneOf(DrinkSize.class);
    var identities = new HashSet<RecipeIdentity>();
    if (product.getProductType() == ProductType.PACKAGED && !items.isEmpty()) {
      throw new AppException(ErrorCode.PRODUCT_CONFIGURATION_INVALID);
    }

    for (var item : items) {
      if (item.quantity() == null || !Double.isFinite(item.quantity()) || item.quantity() <= 0
          || !identities.add(new RecipeIdentity(item.ingredientId(), item.size()))) {
        throw new AppException(ErrorCode.PRODUCT_CONFIGURATION_INVALID);
      }
      sizes.add(item.size());
    }

    if (product.getProductType() == ProductType.MADE_TO_ORDER) {
      var expected = Boolean.TRUE.equals(product.getSizeEnabled())
          ? EnumSet.of(DrinkSize.M, DrinkSize.L) : EnumSet.of(DrinkSize.M);
      if (!sizes.equals(expected)) {
        throw new AppException(ErrorCode.RECIPE_SIZE_NOT_CONFIGURED);
      }
    }

    var ingredientIds = items.stream().map(AdminProductRequest.RecipeItem::ingredientId).distinct().toList();
    Map<Long, Ingredient> ingredients = ingredientRepository.findAllById(ingredientIds).stream()
        .collect(Collectors.toMap(Ingredient::getId, ingredient -> ingredient));
    if (ingredients.size() != ingredientIds.size()) {
      throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
    }

    List<Recipe> recipes = new ArrayList<>();
    for (var item : items) {
      Ingredient ingredient = ingredients.get(item.ingredientId());
      if ("CUP_M".equals(ingredient.getCode()) || "CUP_L".equals(ingredient.getCode())) {
        throw new AppException(ErrorCode.PRODUCT_CONFIGURATION_INVALID);
      }
      Recipe recipe = new Recipe();
      recipe.setProduct(product);
      recipe.setIngredient(ingredient);
      recipe.setSize(item.size());
      recipe.setQuantity(item.quantity());
      recipes.add(recipe);
    }

    recipeRepository.deleteAll(recipeRepository.findByProductId(product.getId()));
    return toAdminResponse(product, recipeRepository.saveAll(recipes));
  }

  private AdminProductDetailResponse toAdminResponse(Product product, List<Recipe> recipes) {
    var items = recipes.stream()
        .sorted(java.util.Comparator.comparing(Recipe::getSize).thenComparing(recipe -> recipe.getIngredient().getId()))
        .map(recipe -> new AdminProductDetailResponse.RecipeItem(
            recipe.getIngredient().getId(), recipe.getIngredient().getName(),
            recipe.getIngredient().getUnit(), recipe.getQuantity(), recipe.getSize()))
        .toList();
    return new AdminProductDetailResponse(ProductMapper.toResponse(product), items);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductResponse> getPublicMenu() {
    return productRepository.findByIsActiveTrueOrderByIdAsc()
        .stream().map(ProductMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public ProductResponse updateProductActive(Long id, boolean active) {
    Product product = productRepository.findByIdForUpdate(id)
        .orElseThrow(() ->
            new AppException(ErrorCode.PRODUCT_NOT_FOUND));

    product.setIsActive(active);
    return ProductMapper.toResponse(product);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ProductResponse> getAdminProducts(String keyword, Long categoryId, Boolean active, int page, int size) {
    String search = keyword == null ? "" : keyword.strip();

    var pageable = PageRequest.of(
        Math.max(0, page),
        Math.min(100, Math.max(1, size)),
        Sort.by("name", "id")
    );

    var products = productRepository.findForAdmin(
        search, categoryId, active, pageable);

    return PageResponse.from(products.map(ProductMapper::toResponse));
  }
}
