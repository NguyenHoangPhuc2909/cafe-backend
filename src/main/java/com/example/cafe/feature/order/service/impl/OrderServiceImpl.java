package com.example.cafe.feature.order.service.impl;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.inventory.Ingredient;
import com.example.cafe.common.entity.inventory.StockReservation;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.order.OrderItem;
import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.*;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.inventory.repository.StockReservationRepository;
import com.example.cafe.feature.order.dto.request.CreateOrderRequest;
import com.example.cafe.feature.order.dto.request.StaffOrderRequest;
import com.example.cafe.feature.order.dto.response.OrderResponse;
import com.example.cafe.feature.order.mapper.OrderMapper;
import com.example.cafe.feature.order.repository.OrderItemRepository;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.order.service.OrderCreationService;
import com.example.cafe.feature.order.service.OrderService;
import com.example.cafe.feature.table.repository.TableSessionRepository;
import com.example.cafe.feature.table.service.TableSessionService;
import com.example.cafe.security.TableSessionTokenProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final TableSessionRepository tableSessionRepository;
  private final TableSessionTokenProvider tableSessionTokenProvider;
  private final ProductRepository productRepository;
  private final IngredientRepository ingredientRepository;
  private final StockReservationRepository stockReservationRepository;
  private final UserRepository userRepository;

  private final TableSessionService tableSessionService;
  private final OrderCreationService orderCreationService;


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
  public OrderResponse createGuestOrder(String token, CreateOrderRequest request) {
    // 1. Xác minh token và khoá phiên
    TableSession session = resolveGuestSessionForOrder(token);

    return orderCreationService.create(session, null, request, OrderType.DINE_IN);
  }

  @Override
  @Transactional
  public boolean expireUnpaidOrder(Long orderId) {
    // Khóa đơn trước khi kiểm tra trạng thái
    Order order = orderRepository.findByIdForUpdate(orderId)
        .orElse(null);

    if (order == null) {
      return false;
    }

    // Chỉ xử lý đơn đang chờ thanh toán và có hạn thanh toán
    if (order.getStatus() != OrderStatus.PENDING
        || order.getPaymentStatus() != OrderPaymentStatus.UNPAID
        || order.getPaymentExpiresAt() == null) {
      return false;
    }

    // Chưa tới hạn thì chưa hoàn kho
    if (order.getPaymentExpiresAt().isAfter(Instant.now())) {
      return false;
    }

    releaseStock(order);
    order.setStatus(OrderStatus.EXPIRED);
    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<OrderResponse> getStaffOrders(String status, OrderPaymentStatus paymentStatus, Long sessionId, int page, int size) {
    if (page < 0 || size < 1 || size > 100 || (sessionId != null && sessionId <= 0)) {
      throw new AppException(ErrorCode.INVALID_ORDER_QUERY);
    }

    OrderStatus normalizedStatus = null;

    if (status != null && !status.isBlank()) {
      try {
        normalizedStatus = OrderStatus.valueOf(
            status.trim().toUpperCase(Locale.ROOT)
        );
      } catch (IllegalArgumentException exception) {
        throw new AppException(ErrorCode.INVALID_ORDER_QUERY);
      }
    }

    var pageable = PageRequest.of(page, size,
        Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
        ));

    var orders = orderRepository.findForStaff(normalizedStatus, paymentStatus, sessionId, pageable);

    var responses = orders.map(order ->
        OrderMapper.toResponse(order,
            orderItemRepository.findByOrderId(order.getId())));

    return PageResponse.from(responses);
  }

  @Override
  @Transactional(readOnly = true)
  public OrderResponse getGuestOrder(String token, Long orderId) {
    var session = tableSessionService.getGuestSession(token);

    if (orderId == null || orderId <= 0) {
      throw new AppException(ErrorCode.ORDER_NOT_FOUND);
    }

    Order order = orderRepository.findByIdAndTableSession_Id(orderId, session.getId())
        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

    return OrderMapper.toResponse(order, items);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<OrderResponse> getGuestOrders(String token, int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_ORDER_QUERY);
    }
    var session = tableSessionService.getGuestSession(token);
    var orders = orderRepository.findByTableSession_IdOrderByCreatedAtDescIdDesc(
        session.getId(), PageRequest.of(page, size));
    return PageResponse.from(orders.map(order -> OrderMapper.toResponse(
        order, orderItemRepository.findByOrderId(order.getId()))));
  }

  @Override
  @Transactional
  public OrderResponse completeOrder(Long orderId) {
    if (orderId == null || orderId <= 0) {
      throw new AppException(ErrorCode.ORDER_NOT_FOUND);
    }

    Order order = orderRepository.findByIdForUpdate(orderId)
        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    if (order.getPaymentStatus() != OrderPaymentStatus.PAID) {
      throw new AppException(ErrorCode.ORDER_NOT_COMPLETABLE);
    }

    if (order.getStatus() == OrderStatus.COMPLETED) {
      return OrderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    if (order.getStatus() != OrderStatus.PREPARING) {
      throw new AppException(ErrorCode.ORDER_NOT_COMPLETABLE);
    }

    order.setStatus(OrderStatus.COMPLETED);
    return OrderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
  }

  @Override
  public OrderResponse createStaffOrder(String username, StaffOrderRequest request) {
    User staff = userRepository.findByUsername(username)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    TableSession session = null;

    if (request.getSessionId() != null) {
      session = tableSessionRepository
          .findByIdForUpdate(request.getSessionId())
          .orElseThrow(() ->
              new AppException(ErrorCode.TABLE_SESSION_NOT_FOUND));

      if (session.getStatus() != TableSessionStatus.OPEN) {
        throw new AppException(ErrorCode.TABLE_SESSION_CLOSED);
      }

      if (!Boolean.TRUE.equals(session.getTable().getIsActive())) {
        throw new AppException(ErrorCode.TABLE_INACTIVE);
      }
    }
    return orderCreationService.create(session, staff, request, request.getOrderType());
  }

  @Override
  public OrderResponse getOrderById(Long id) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    List<OrderItem> orderItems = orderItemRepository.findByOrderId(id);
    return OrderMapper.toResponse(order, orderItems);
  }

  private void releaseStock(Order order) {
    List<StockReservation> heldReservations = stockReservationRepository.findByOrder_IdOrderByIdAsc(order.getId())
        .stream().filter(r -> r.getStatus() == StockReservationStatus.HELD)
        .toList();

    Map<Long, StockReservation> productReservations = new TreeMap<>();
    Map<Long, StockReservation> ingredientReservations = new TreeMap<>();

    // Kiểm tra và chia bản ghi theo loại hàng cần hoàn
    for (StockReservation reservation : heldReservations) {
      Double quantity = reservation.getQuantity();

      if (quantity == null || !Double.isFinite(quantity) || quantity <= 0
          || (reservation.getProduct() == null) == (reservation.getIngredient() == null)) {
        throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
      }

      if (reservation.getProduct() != null) {
        if (quantity != Math.rint(quantity) || quantity > Integer.MAX_VALUE) {
          throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
        }

        Long productId = reservation.getProduct().getId();

        if (productReservations.putIfAbsent(productId, reservation) != null) {
          throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
        }
      } else {
        Long ingredientId = reservation.getIngredient().getId();

        if (ingredientReservations.putIfAbsent(ingredientId, reservation) != null) {
          throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
        }
      }
    }

    // Khóa và hoàn sản phẩm theo ID tăng dần
    for (Long productId : productReservations.keySet()) {
      Product product = productRepository.findByIdForUpdate(productId)
          .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

      Integer stock = product.getStockQuantity();
      StockReservation reservation = productReservations.get(productId);

      if (stock == null || stock < 0) {
        throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
      }

      long restoredStock = (long) stock + reservation.getQuantity().longValue();

      if (restoredStock > Integer.MAX_VALUE) {
        throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
      }

      product.setStockQuantity((int) restoredStock);
    }

    // Sau đó khóa và hoàn nguyên liệu theo ID tăng dần
    for (Long ingredientId : ingredientReservations.keySet()) {
      Ingredient ingredient = ingredientRepository.findByIdForUpdate(ingredientId)
          .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));

      Double stock = ingredient.getStockQuantity();
      StockReservation reservation = ingredientReservations.get(ingredientId);

      if (stock == null || !Double.isFinite(stock) || stock < 0) {
        throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
      }

      double restoredStock = stock + reservation.getQuantity();

      if (!Double.isFinite(restoredStock)) {
        throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
      }

      ingredient.setStockQuantity(restoredStock);
    }

    for (StockReservation reservation : heldReservations) {
      reservation.setStatus(StockReservationStatus.RELEASED);
    }
  }
}
