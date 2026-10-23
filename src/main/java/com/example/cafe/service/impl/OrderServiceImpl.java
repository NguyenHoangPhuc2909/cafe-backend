package com.example.cafe.service.impl;

import com.example.cafe.dto.request.GuestOrderRequest;
import com.example.cafe.dto.request.OrderRequest;
import com.example.cafe.dto.response.OrderResponse;
import com.example.cafe.entity.*;
import com.example.cafe.exception.AppException;
import com.example.cafe.exception.ErrorCode;
import com.example.cafe.mapper.OrderMapper;
import com.example.cafe.repository.*;
import com.example.cafe.security.TableSessionTokenProvider;
import com.example.cafe.service.OrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
public class OrderServiceImpl implements OrderService {
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final TableSessionRepository tableSessionRepository;
  private final TableSessionTokenProvider tableSessionTokenProvider;
  private final StockReservationRepository stockReservationRepository;

  private final RecipeRepository recipeRepository;
  private final IngredientRepository ingredientRepository;

  private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(5);

  private TableSession resolveGuestSessionForOrder(String token) {

    if (token == null || token.isBlank()) {
      throw new AppException(ErrorCode.INVALID_TABLE_SESSION_TOKEN);
    }

    Long sessionId;
    Long tableId;

    try {
      Claims claims = tableSessionTokenProvider.parseToken(token);

      sessionId = Long.valueOf(claims.getSubject());
      tableId = Long.valueOf(claims.get("tableId").toString());

      if (sessionId <= 0 || tableId <= 0) {
        throw new IllegalArgumentException("ID không hợp lệ");
      }

    } catch (JwtException | IllegalArgumentException exception) {
      throw new AppException(
          ErrorCode.INVALID_TABLE_SESSION_TOKEN);
    }

    TableSession session = tableSessionRepository.findByIdForUpdate(sessionId)
        .orElseThrow(() -> new AppException(ErrorCode.INVALID_TABLE_SESSION_TOKEN));

    CafeTable table = session.getTable();

    if (!table.getId().equals(tableId)) {
      throw new AppException(ErrorCode.INVALID_TABLE_SESSION_TOKEN);
    }

    if (session.getStatus() != TableSessionStatus.OPEN) {
      throw new AppException(ErrorCode.TABLE_SESSION_CLOSED);
    }

    if (!Boolean.TRUE.equals(table.getIsActive())) {
      throw new AppException(ErrorCode.TABLE_INACTIVE);
    }

    return session;
  }

  @Override
  public OrderResponse createOrder(OrderRequest request) {
    User user = userRepository.findById(request.getUserId())
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    Order order = new Order();
    order.setUser(user);
    order.setStatus("PENDING");

    order.setTotalAmount(BigDecimal.ZERO);
    order = orderRepository.save(order);

    BigDecimal totalAmount = BigDecimal.ZERO;
    List<OrderItem> saveItems = new ArrayList<>();

    for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
      Product product = productRepository.findById(itemReq.getProductId())
          .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

      if (product.getProductType() == ProductType.PACKAGED) {
        if (product.getStockQuantity() < itemReq.getQuantity()) {
          throw new AppException(ErrorCode.OUT_OF_STOCK);
        }
        product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
        productRepository.save(product);
      } else if (product.getProductType() == ProductType.MADE_TO_ORDER) {
        List<Recipe> recipes = recipeRepository.findByProductId(product.getId());
        for (Recipe recipe : recipes) {
          Ingredient ingredient = recipe.getIngredient();
          double requiredAmount = recipe.getQuantity() * itemReq.getQuantity();
          if (ingredient.getStockQuantity() < requiredAmount) {
            throw new AppException(ErrorCode.NOT_ENOUGH_INGREDIENT);
          }
          ingredient.setStockQuantity(ingredient.getStockQuantity() - requiredAmount);
          ingredientRepository.save(ingredient);
        }
      }
      OrderItem orderItem = new OrderItem();
      orderItem.setOrder(order);
      orderItem.setProduct(product);
      orderItem.setQuantity(itemReq.getQuantity());
      orderItem.setPrice(product.getPrice());

      BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemReq.getQuantity()));
      totalAmount = totalAmount.add(itemTotal);

      saveItems.add(orderItemRepository.save(orderItem));
    }
    order.setTotalAmount(totalAmount);
    order.setNote(request.getNote());

    order = orderRepository.save(order);
    return OrderMapper.toResponse(order, saveItems);
  }

  @Override
  public OrderResponse createGuestOrder(String token, GuestOrderRequest request) {
    // 1. Xác minh token và khoá phiên
    TableSession session = resolveGuestSessionForOrder(token);

    // 2. Gộp số lượng và khoá sản phẩm
    Map<Long, Integer> quantities = collectQuantities(request);
    Map<Long, Product> products = lockProducts(quantities);

    // 3. Tính nhu cầu, khóa nguyên liệu và kiểm tra đủ kho
    Map<Long, Double> requirements = collectIngredientRequirements(products, quantities);
    Map<Long, Ingredient> ingredients = lockIngredients(requirements);

    validatePackagedStock(products, quantities);

    // 4. Tính tiền và chuẩn bị chi tiết đơn
    BigDecimal totalAmount = BigDecimal.ZERO;
    List<OrderItem> items = new ArrayList<>();

    for (Product product : products.values()) {
      int quantity = quantities.get(product.getId());
      BigDecimal price = product.getPrice();

      if (price == null || price.signum() < 0) {
        throw new AppException(
            ErrorCode.PRODUCT_CONFIGURATION_INVALID
        );
      }

      totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));

      OrderItem item = new OrderItem();
      item.setProduct(product);
      item.setQuantity(quantity);
      item.setPrice(price);

      items.add(item);

    }

    if (totalAmount.compareTo(
        new BigDecimal("9999999999.99")
    ) > 0) {
      throw new AppException(ErrorCode.INVALID_ORDER_TOTAL);
    }

    // 5. Lưu đơn chưa thanh toán
    Order order = new Order();
    order.setTableSession(session);
    order.setStatus("PENDING");
    order.setPaymentStatus(OrderPaymentStatus.UNPAID);
    order.setTotalAmount(totalAmount);
    order.setNote(request.getNote());
    order.setPaymentExpiresAt(
        Instant.now().plus(PAYMENT_TIMEOUT)
    );

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

@Override
public OrderResponse getOrderById(Long id) {
  Order order = orderRepository.findById(id)
      .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

  List<OrderItem> orderItems = orderItemRepository.findByOrderId(id);
  return OrderMapper.toResponse(order, orderItems);
}

private Map<Long, Integer> collectQuantities(GuestOrderRequest request) {
  Map<Long, Integer> quantities = new TreeMap<>();

  try {
    for (GuestOrderRequest.ItemRequest item : request.getItems()) {
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
