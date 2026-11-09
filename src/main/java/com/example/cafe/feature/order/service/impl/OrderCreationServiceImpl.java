package com.example.cafe.feature.order.service.impl;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.catalog.Recipe;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.entity.inventory.StockReservation;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.order.OrderItem;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.OrderPaymentStatus;
import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.enums.OrderType;
import com.example.cafe.common.enums.ProductType;
import com.example.cafe.common.enums.StockReservationStatus;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.catalog.repository.RecipeRepository;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.inventory.repository.StockReservationRepository;
import com.example.cafe.feature.order.dto.request.CreateOrderRequest;
import com.example.cafe.feature.order.dto.response.OrderResponse;
import com.example.cafe.feature.order.mapper.OrderMapper;
import com.example.cafe.feature.order.repository.OrderItemRepository;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.order.service.GuestOrderLines;
import com.example.cafe.feature.order.service.OrderCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderCreationServiceImpl implements OrderCreationService {
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  private final RecipeRepository recipeRepository;
  private final IngredientRepository ingredientRepository;
  private final StockReservationRepository stockReservationRepository;

  private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(3);

  @Override
  public OrderResponse create(
      TableSession session,
      User createdBy,
      CreateOrderRequest request,
      OrderType orderType) {

    // 2. Gộp số lượng và khoá sản phẩm
    Map<Long, Integer> quantities = collectQuantities(request);
    Map<Long, Product> products = lockProducts(quantities);
    var lines = GuestOrderLines.collect(request, products);

    // 3. Tính nhu cầu, khóa nguyên liệu và kiểm tra đủ kho
    Map<Long, Double> requirements = collectIngredientRequirements(products, quantities);
    Map<Long, Ingredient> ingredients = lockIngredients(requirements);

    validatePackagedStock(products, quantities);

    // 4. Tính tiền và chuẩn bị chi tiết đơn
    BigDecimal totalAmount = BigDecimal.ZERO;
    List<OrderItem> items = new ArrayList<>();

    for (var line : lines) {
      Product product = line.product();
      int quantity = line.quantity();
      BigDecimal price = line.price();

      totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));

      OrderItem item = new OrderItem();
      item.setProduct(product);
      item.setQuantity(quantity);
      item.setPrice(price);
      item.setSize(line.size());
      item.setIceLevel(line.iceLevel());
      item.setSweetnessLevel(line.sweetnessLevel());

      items.add(item);

    }

    if (totalAmount.compareTo(
        new BigDecimal("9999999999.99")) > 0) {
      throw new AppException(ErrorCode.INVALID_ORDER_TOTAL);
    }

    // 5. Lưu đơn chưa thanh toán
    Order order = new Order();
    order.setTableSession(session);
    order.setCreatedBy(createdBy);
    order.setOrderType(orderType);
    order.setStatus(OrderStatus.PENDING);
    order.setPaymentStatus(OrderPaymentStatus.UNPAID);
    order.setTotalAmount(totalAmount);
    order.setNote(request.getNote());
    order.setPaymentExpiresAt(
        Instant.now().plus(PAYMENT_TIMEOUT));

    order = orderRepository.save(order);

    // 6. Gắn chi tiết vào đơn đã lưu
    for (OrderItem item : items) {
      item.setOrder(order);
    }

    List<OrderItem> savedItems = orderItemRepository.saveAll(items);

    // 7. Giữ hàng cho đơn chờ thanh toán
    reserveStock(order, products, quantities, ingredients, requirements);

    return OrderMapper.toResponse(order, savedItems);
  }

  private Map<Long, Integer> collectQuantities(CreateOrderRequest request) {
    Map<Long, Integer> quantities = new TreeMap<>();

    try {
      for (CreateOrderRequest.ItemRequest item : request.getItems()) {
        quantities.merge(
            item.getProductId(),
            item.getQuantity(),
            (current, extra) -> Math.addExact(current, extra));
      }
    } catch (ArithmeticException exception) {
      throw new AppException(ErrorCode.INVALID_ORDER_QUANTITY);
    }
    return quantities;
  }

  private Map<Long, Product> lockProducts(Map<Long, Integer> quantities) {
    Map<Long, Product> products = new TreeMap<>();

    for (Long productId : new TreeMap<>(quantities).keySet()) {

      Product product = productRepository.findByIdForUpdate(productId)
          .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

      if (!Boolean.TRUE.equals(product.getIsActive())) {
        throw new AppException(ErrorCode.PRODUCT_INACTIVE);
      }

      products.put(productId, product);
    }

    return products;
  }

  private Map<Long, Double> collectIngredientRequirements(Map<Long, Product> products, Map<Long, Integer> quantities) {
    Map<Long, Double> requirements = new TreeMap<>();
    for (Product product : products.values()) {
      if (product.getProductType() == ProductType.PACKAGED) {
        continue;
      }

      if (product.getProductType() != ProductType.MADE_TO_ORDER) {
        throw new AppException(ErrorCode.PRODUCT_CONFIGURATION_INVALID);
      }

      List<Recipe> recipes = recipeRepository.findByProductId(product.getId());

      if (recipes.isEmpty()) {
        throw new AppException(ErrorCode.PRODUCT_CONFIGURATION_INVALID);
      }

      for (Recipe recipe : recipes) {
        Double amountPerItem = recipe.getQuantity();

        if (amountPerItem == null
            || !Double.isFinite(amountPerItem)
            || amountPerItem <= 0
            || recipe.getIngredient() == null
            || recipe.getIngredient().getId() == null) {
          throw new AppException(
              ErrorCode.PRODUCT_CONFIGURATION_INVALID);
        }

        Long ingredientId = recipe.getIngredient().getId();
        double totalRequired = requirements.getOrDefault(ingredientId, 0.0)
            + amountPerItem * quantities.get(product.getId());

        if (!Double.isFinite(totalRequired)) {
          throw new AppException(
              ErrorCode.PRODUCT_CONFIGURATION_INVALID);
        }

        requirements.put(ingredientId, totalRequired);
      }
    }
    return requirements;
  }

  private Map<Long, Ingredient> lockIngredients(Map<Long, Double> requirements) {
    Map<Long, Ingredient> ingredients = new TreeMap<>();
    for (Long ingredientId : new TreeMap<>(requirements).keySet()) {
      Ingredient ingredient = ingredientRepository.findByIdForUpdate(ingredientId)
          .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));

      Double stock = ingredient.getStockQuantity();
      double required = requirements.get(ingredientId);

      if (stock == null || !Double.isFinite(stock) || stock < required) {
        throw new AppException(ErrorCode.NOT_ENOUGH_INGREDIENT);
      }

      ingredients.put(ingredientId, ingredient);
    }
    return ingredients;
  }

  private void validatePackagedStock(Map<Long, Product> products, Map<Long, Integer> quantities) {
    for (Product product : products.values()) {
      if (product.getProductType() != ProductType.PACKAGED) {
        continue;
      }

      Integer stock = product.getStockQuantity();
      int required = quantities.get(product.getId());

      if (stock == null || stock < required) {
        throw new AppException(ErrorCode.OUT_OF_STOCK);
      }
    }
  }

  private void reserveStock(Order order, Map<Long, Product> products,
                            Map<Long, Integer> quantities,
                            Map<Long, Ingredient> ingredients,
                            Map<Long, Double> requirements) {
    List<StockReservation> reservations = new ArrayList<>();

    // Giữ hàng đóng gói
    for (Product product : products.values()) {
      if (product.getProductType() != ProductType.PACKAGED) {
        continue;
      }

      int quantity = quantities.get(product.getId());

      product.setStockQuantity(product.getStockQuantity() - quantity);

      StockReservation reservation = new StockReservation();
      reservation.setOrder(order);
      reservation.setProduct(product);
      reservation.setQuantity((double) quantity);
      reservation.setStatus(StockReservationStatus.HELD);

      reservations.add(reservation);
    }

    // Giữ nguyên liệu pha chế
    for (Ingredient ingredient : ingredients.values()) {
      double required = requirements.get(ingredient.getId());

      ingredient.setStockQuantity(ingredient.getStockQuantity() - required);

      StockReservation reservation = new StockReservation();
      reservation.setOrder(order);
      reservation.setIngredient(ingredient);
      reservation.setQuantity(required);
      reservation.setStatus(StockReservationStatus.HELD);

      reservations.add(reservation);
    }

    stockReservationRepository.saveAll(reservations);
  }
}
