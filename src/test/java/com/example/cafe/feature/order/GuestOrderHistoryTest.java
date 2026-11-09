package com.example.cafe.feature.order;

import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.order.repository.OrderItemRepository;
import com.example.cafe.feature.order.service.impl.OrderServiceImpl;
import com.example.cafe.feature.table.dto.response.TableSessionResponse;
import com.example.cafe.feature.table.service.TableSessionService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;

class GuestOrderHistoryTest {
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }

  @Test void historyUsesTheSessionFromTheTokenAndPreservesPagination() {
    var table = new CafeTable();
    table.setId(1L);
    table.setName("Bàn 1");
    var session = new TableSession();
    session.setId(7L);
    session.setTable(table);
    var order = new Order();
    order.setId(101L);
    order.setTableSession(session);
    var tables = proxy(TableSessionService.class, (p, method, args) -> {
      assertEquals("getGuestSession", method.getName());
      assertEquals("guest-token", args[0]);
      return TableSessionResponse.builder().id(7L).tableId(1L).build();
    });
    var orders = proxy(OrderRepository.class, (p, method, args) -> {
      assertEquals("findByTableSession_IdOrderByCreatedAtDescIdDesc", method.getName());
      assertEquals(7L, args[0]);
      Pageable pageable = (Pageable) args[1];
      assertEquals(1, pageable.getPageNumber());
      assertEquals(20, pageable.getPageSize());
      return new PageImpl<>(List.of(order), pageable, 21);
    });
    var items = proxy(OrderItemRepository.class, (p, method, args) -> {
      assertEquals("findByOrderId", method.getName());
      assertEquals(101L, args[0]);
      return List.of();
    });
    var service = new OrderServiceImpl(orders, items, null, null, null, null, null, null, tables, null);
    var result = service.getGuestOrders("guest-token", 1, 20);
    assertEquals(1, result.getPage());
    assertEquals(21, result.getTotalElements());
    assertEquals(2, result.getTotalPages());
    assertEquals(7L, result.getContent().getFirst().getSessionId());
  }

  @Test void invalidTokenCannotReadOrderHistory() {
    var tables = proxy(TableSessionService.class, (p, method, args) -> {
      throw new AppException(ErrorCode.INVALID_TABLE_SESSION_TOKEN);
    });
    var orders = proxy(OrderRepository.class, (p, method, args) -> {
      throw new AssertionError("History must not be queried before session validation");
    });
    var service = new OrderServiceImpl(orders, null, null, null, null, null, null, null, tables, null);
    var error = assertThrows(AppException.class, () -> service.getGuestOrders("bad-token", 0, 20));
    assertEquals(ErrorCode.INVALID_TABLE_SESSION_TOKEN, error.getErrorCode());
    assertThrows(AppException.class, () -> service.getGuestOrders("bad-token", -1, 20));
    assertThrows(AppException.class, () -> service.getGuestOrders("bad-token", 0, 101));
  }
}
