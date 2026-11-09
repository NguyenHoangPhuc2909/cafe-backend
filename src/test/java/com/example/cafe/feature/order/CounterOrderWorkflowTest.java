package com.example.cafe.feature.order;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.catalog.Recipe;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.entity.inventory.StockReservation;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.order.OrderItem;
import com.example.cafe.common.entity.payment.Payment;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.enums.*;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.catalog.repository.RecipeRepository;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.inventory.repository.StockReservationRepository;
import com.example.cafe.feature.order.dto.request.CreateOrderRequest;
import com.example.cafe.feature.order.dto.request.StaffOrderRequest;
import com.example.cafe.feature.order.repository.OrderItemRepository;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.order.service.impl.OrderCreationServiceImpl;
import com.example.cafe.feature.order.service.impl.OrderServiceImpl;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.feature.payment.service.impl.PaymentServiceImpl;
import com.example.cafe.feature.table.repository.TableSessionRepository;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Exercise real services with in-memory repository boundaries, without a database.
class CounterOrderWorkflowTest {
  private final Stub<OrderRepository> orders = new Stub<>(OrderRepository.class);
  private final Stub<OrderItemRepository> items = new Stub<>(OrderItemRepository.class);
  private final Stub<ProductRepository> products = new Stub<>(ProductRepository.class);
  private final Stub<RecipeRepository> recipes = new Stub<>(RecipeRepository.class);
  private final Stub<IngredientRepository> ingredients = new Stub<>(IngredientRepository.class);
  private final Stub<StockReservationRepository> reservations = new Stub<>(StockReservationRepository.class);
  private final Stub<UserRepository> users = new Stub<>(UserRepository.class);
  private final Stub<TableSessionRepository> sessions = new Stub<>(TableSessionRepository.class);
  private final Stub<PaymentRepository> payments = new Stub<>(PaymentRepository.class);
  private final OrderCreationServiceImpl creation = new OrderCreationServiceImpl(
      orders.value, items.value, products.value, recipes.value, ingredients.value, reservations.value);
  private final OrderServiceImpl service = new OrderServiceImpl(
      orders.value, items.value, sessions.value, null, products.value, ingredients.value,
      reservations.value, users.value, null, creation);
  private final PaymentServiceImpl paymentService = new PaymentServiceImpl(
      payments.value, orders.value, users.value, reservations.value);
  private final Product bottle = new Product();
  private final Ingredient coffee = new Ingredient();
  private final User staff = new User();
  private Order savedOrder;
  private Payment savedPayment;
  private List<OrderItem> savedItems;
  private List<StockReservation> held;

  @BeforeEach void setup() {
    staff.setId(9L);
    staff.setUsername("cashier");
    staff.setRole("STAFF");
    users.on("findByUsername", args -> Optional.of(staff));
    bottle.setId(1L);
    bottle.setName("Nước chai");
    bottle.setPrice(new BigDecimal("15000"));
    bottle.setProductType(ProductType.PACKAGED);
    bottle.setStockQuantity(10);
    Product drink = new Product();
    drink.setId(2L);
    drink.setName("Cà phê");
    drink.setPrice(new BigDecimal("30000"));
    drink.setProductType(ProductType.MADE_TO_ORDER);
    coffee.setId(3L);
    coffee.setStockQuantity(100.0);
    Recipe recipe = new Recipe();
    recipe.setIngredient(coffee);
    recipe.setQuantity(5.0);
    products.on("findByIdForUpdate", args -> Optional.of(args[0].equals(1L) ? bottle : drink));
    recipes.on("findByProductId", args -> List.of(recipe));
    ingredients.on("findByIdForUpdate", args -> Optional.of(coffee));
    orders.on("save", args -> {
      savedOrder = (Order) args[0];
      savedOrder.setId(10L);
      return savedOrder;
    });
    items.on("saveAll", args -> {
      savedItems = listOf(args[0], OrderItem.class);
      for (int i = 0; i < savedItems.size(); i++) savedItems.get(i).setId((long) i + 1);
      return savedItems;
    });
    items.on("findByOrderId", args -> savedItems);
    reservations.on("saveAll", args -> {
      held = listOf(args[0], StockReservation.class);
      return held;
    });
    orders.on("findByIdForUpdate", args -> Optional.ofNullable(savedOrder));
    reservations.on("findByOrder_IdOrderByIdAsc", args -> held);
    payments.on("findByOrder_Id", args -> Optional.ofNullable(savedPayment));
    payments.on("save", args -> {
      savedPayment = (Payment) args[0];
      savedPayment.setId(20L);
      return savedPayment;
    });
  }

  private StaffOrderRequest request(OrderType type) {
    var packaged = new CreateOrderRequest.ItemRequest();
    packaged.setProductId(1L);
    packaged.setQuantity(2);
    var drink = new CreateOrderRequest.ItemRequest();
    drink.setProductId(2L);
    drink.setQuantity(1);
    var request = new StaffOrderRequest();
    request.setOrderType(type);
    request.setItems(List.of(packaged, drink));
    return request;
  }

  @Test void takeawayUsesOrderNumberAndReservesBothStockTypesWithoutATable() {
    var result = service.createStaffOrder("cashier", request(OrderType.TAKEAWAY));
    assertNull(result.getSessionId());
    assertNull(result.getTableId());
    assertEquals(OrderType.TAKEAWAY, result.getOrderType());
    assertSame(staff, savedOrder.getCreatedBy());
    assertEquals(new BigDecimal("60000"), result.getTotalAmount());
    assertEquals(8, bottle.getStockQuantity());
    assertEquals(95.0, coffee.getStockQuantity());
    assertEquals(2, held.size());
    assertTrue(held.stream().allMatch(r -> r.getStatus() == StockReservationStatus.HELD));
    assertTrue(sessions.calls.isEmpty());
  }

  @Test void dineInCounterPaymentAndCompletionDoNotDeductOrReleaseStockAgain() {
    var created = service.createStaffOrder("cashier", request(OrderType.DINE_IN));
    assertEquals(OrderType.DINE_IN, created.getOrderType());
    assertNull(created.getSessionId());
    assertThrows(AppException.class, () -> service.completeOrder(10L));
    var payment = paymentService.confirmPayment(10L, PaymentMethod.CASH, "cashier");
    assertEquals(new BigDecimal("60000"), payment.amount());
    assertEquals(OrderStatus.PREPARING, savedOrder.getStatus());
    assertTrue(held.stream().allMatch(r -> r.getStatus() == StockReservationStatus.CONSUMED));
    assertEquals(payment.id(), paymentService.confirmPayment(10L, PaymentMethod.CASH, "cashier").id());
    assertEquals(OrderStatus.COMPLETED, service.completeOrder(10L).getStatus());
    assertEquals(OrderStatus.COMPLETED, service.completeOrder(10L).getStatus());
    savedOrder.setPaymentExpiresAt(Instant.now().minusSeconds(1));
    assertFalse(service.expireUnpaidOrder(10L));
    assertEquals(8, bottle.getStockQuantity());
    assertEquals(95.0, coffee.getStockQuantity());
    assertEquals(1, payments.calls.get("save"));
    assertTrue(sessions.calls.isEmpty());
  }

  @Test void expiredCounterOrderRestoresBothStockTypesExactlyOnce() {
    service.createStaffOrder("cashier", request(OrderType.TAKEAWAY));
    assertFalse(service.expireUnpaidOrder(10L));
    savedOrder.setPaymentExpiresAt(Instant.now().minusSeconds(1));
    var error = assertThrows(AppException.class,
        () -> paymentService.confirmPayment(10L, PaymentMethod.CASH, "cashier"));
    assertEquals(ErrorCode.PAYMENT_EXPIRED, error.getErrorCode());
    assertTrue(service.expireUnpaidOrder(10L));
    assertEquals(OrderStatus.EXPIRED, savedOrder.getStatus());
    assertEquals(10, bottle.getStockQuantity());
    assertEquals(100.0, coffee.getStockQuantity());
    assertTrue(held.stream().allMatch(r -> r.getStatus() == StockReservationStatus.RELEASED));
    assertFalse(service.expireUnpaidOrder(10L));
    assertEquals(10, bottle.getStockQuantity());
    assertEquals(100.0, coffee.getStockQuantity());
  }

  @Test void insufficientIngredientDoesNotCreateOrderOrHoldPackagedStock() {
    coffee.setStockQuantity(1.0);
    var error = assertThrows(AppException.class,
        () -> service.createStaffOrder("cashier", request(OrderType.DINE_IN)));
    assertEquals(ErrorCode.NOT_ENOUGH_INGREDIENT, error.getErrorCode());
    assertEquals(10, bottle.getStockQuantity());
    assertFalse(orders.calls.containsKey("save"));
    assertFalse(reservations.calls.containsKey("saveAll"));
  }

  @Test void sameTableCanOrderAgainBeforePayingWithoutChangingTheFirstOrder() {
    var createdOrders = new ArrayList<Order>();
    orders.on("save", args -> {
      Order value = (Order) args[0];
      value.setId(10L + createdOrders.size());
      createdOrders.add(value);
      return value;
    });
    var table = new CafeTable();
    table.setId(1L);
    table.setName("Bàn 1");
    var session = new TableSession();
    session.setId(7L);
    session.setTable(table);
    var first = creation.create(session, null, request(OrderType.DINE_IN), OrderType.DINE_IN);
    var firstDeadline = first.getPaymentExpiresAt();
    var firstReservations = List.copyOf(held);
    var second = creation.create(session, null, request(OrderType.DINE_IN), OrderType.DINE_IN);
    assertNotEquals(first.getId(), second.getId());
    assertEquals(7L, first.getSessionId());
    assertEquals(first.getSessionId(), second.getSessionId());
    assertEquals(firstDeadline, createdOrders.getFirst().getPaymentExpiresAt());
    assertEquals(OrderPaymentStatus.UNPAID, createdOrders.getFirst().getPaymentStatus());
    assertEquals(6, bottle.getStockQuantity());
    assertEquals(90.0, coffee.getStockQuantity());
    assertTrue(firstReservations.stream().allMatch(r -> r.getStatus() == StockReservationStatus.HELD));
    assertTrue(held.stream().allMatch(r -> r.getStatus() == StockReservationStatus.HELD));
    assertNotSame(firstReservations.getFirst().getOrder(), held.getFirst().getOrder());
  }

  private static <T> List<T> listOf(Object values, Class<T> type) {
    var result = new ArrayList<T>();
    for (Object value : (Iterable<?>) values) result.add(type.cast(value));
    return result;
  }

  private static final class Stub<T> {
    final T value;
    final Map<String, Function<Object[], Object>> handlers = new HashMap<>();
    final Map<String, Integer> calls = new HashMap<>();

    Stub(Class<T> type) {
      value = type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
          (proxy, method, args) -> {
            calls.merge(method.getName(), 1, Integer::sum);
            var handler = handlers.get(method.getName());
            if (handler == null) throw new AssertionError("Unexpected repository call: " + method.getName());
            return handler.apply(args);
          }));
    }

    void on(String method, Function<Object[], Object> handler) { handlers.put(method, handler); }
  }
}
