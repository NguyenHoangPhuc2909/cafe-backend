package com.example.cafe.feature.table;

import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.enums.TableSessionStatus;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.table.repository.CafeTableRepository;
import com.example.cafe.feature.table.repository.TableSessionRepository;
import com.example.cafe.feature.table.service.impl.TableSessionServiceImpl;
import com.example.cafe.security.TableSessionTokenProvider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableSessionClosureTest {
  private final CafeTable table = new CafeTable();
  private final TableSession original = new TableSession();
  private final Map<Long, TableSession> sessions = new HashMap<>();
  private final List<OrderStatus> orderStatuses = new ArrayList<>();
  private final List<String> locks = new ArrayList<>();
  private TableSessionServiceImpl service;
  private long nextId = 8;

  @BeforeEach void setup() {
    table.setId(1L);
    table.setName("Bàn 1");
    table.setQrCode("table-qr");
    original.setId(7L);
    original.setTable(table);
    original.setOpenedAt(LocalDateTime.now());
    sessions.put(7L, original);
    var tables = proxy(CafeTableRepository.class, (p, method, args) -> switch (method.getName()) {
      case "findByIdForUpdate" -> {
        locks.add("table");
        yield args[0].equals(1L) ? Optional.of(table) : Optional.empty();
      }
      case "findByQrCodeForUpdate" -> {
        locks.add("table");
        yield "table-qr".equals(args[0]) ? Optional.of(table) : Optional.empty();
      }
      default -> throw new AssertionError(method.getName());
    });
    var repository = proxy(TableSessionRepository.class, (p, method, args) -> switch (method.getName()) {
      case "findByIdForUpdate" -> { locks.add("session"); yield Optional.ofNullable(sessions.get(args[0])); }
      case "findById" -> Optional.ofNullable(sessions.get(args[0]));
      case "findByTable_IdAndStatus" -> sessions.values().stream()
          .filter(value -> value.getTable().getId().equals(args[0]) && value.getStatus().equals(args[1])).findFirst();
      case "save" -> {
        TableSession value = (TableSession) args[0];
        value.setId(nextId++);
        value.setOpenedAt(LocalDateTime.now());
        sessions.put(value.getId(), value);
        yield value;
      }
      default -> throw new AssertionError(method.getName());
    });
    var orders = proxy(OrderRepository.class, (p, method, args) -> {
      assertEquals("existsByTableSession_IdAndStatusIn", method.getName());
      assertEquals(7L, args[0]);
      var blocked = (Collection<?>) args[1];
      return orderStatuses.stream().anyMatch(blocked::contains);
    });
    String secret = Base64.getEncoder().encodeToString(new byte[32]);
    service = new TableSessionServiceImpl(tables, repository, new TableSessionTokenProvider(secret), orders);
  }

  @Test void unpaidAndPreparingOrdersPreventClosureWithoutChangingTheSession() {
    for (var status : List.of(OrderStatus.PENDING, OrderStatus.PREPARING)) {
      orderStatuses.clear();
      orderStatuses.add(status);
      var error = assertThrows(AppException.class, () -> service.closeSession(1L, 7L));
      assertEquals(ErrorCode.TABLE_SESSION_HAS_ACTIVE_ORDERS, error.getErrorCode());
      assertEquals(TableSessionStatus.OPEN, original.getStatus());
      assertNull(original.getClosedAt());
    }
  }

  @Test void completedAndExpiredOrdersAllowExplicitClosureAndRejectTheOldGuestToken() {
    orderStatuses.addAll(List.of(OrderStatus.COMPLETED, OrderStatus.EXPIRED));
    var access = service.grantGuestAccess("table-qr");
    assertEquals(7L, service.getGuestSession(access.getAccessToken()).getId());
    locks.clear();
    var result = service.closeSession(1L, 7L);
    assertEquals(TableSessionStatus.CLOSE, result.getStatus());
    assertNotNull(result.getClosedAt());
    assertEquals(List.of("table", "session"), locks);
    var error = assertThrows(AppException.class, () -> service.getGuestSession(access.getAccessToken()));
    assertEquals(ErrorCode.TABLE_SESSION_CLOSED, error.getErrorCode());
    var nextAccess = service.grantGuestAccess("table-qr");
    assertNotEquals(access.getSessionId(), nextAccess.getSessionId());
    assertEquals(nextAccess.getSessionId(), service.getGuestSession(nextAccess.getAccessToken()).getId());
    assertThrows(AppException.class, () -> service.getGuestSession(access.getAccessToken()));
  }

  @Test void emptySessionCanCloseAndStaleCloseCannotCloseTheNextVisit() {
    var closed = service.closeSession(1L, 7L);
    var closedAt = closed.getClosedAt();
    var next = service.grantGuestAccess("table-qr");
    assertNotEquals(7L, next.getSessionId());
    assertEquals(closedAt, service.closeSession(1L, 7L).getClosedAt());
    assertEquals(TableSessionStatus.OPEN, sessions.get(next.getSessionId()).getStatus());
    assertEquals(next.getSessionId(), service.getGuestSession(service.grantGuestAccess("table-qr").getAccessToken()).getId());
  }

  @Test void firstQrConfirmationOpensOneSessionAndLaterConfirmationsReuseIt() {
    sessions.clear();
    var first = service.grantGuestAccess("table-qr");
    var second = service.grantGuestAccess("table-qr");
    assertEquals(first.getSessionId(), second.getSessionId());
    assertEquals(1, sessions.size());
    assertEquals(List.of("table", "table"), locks);
    assertEquals(TableSessionStatus.OPEN, service.getGuestSession(first.getAccessToken()).getStatus());
  }

  @Test void disabledOrUnknownQrCannotOpenASession() {
    sessions.clear();
    table.setIsActive(false);
    var disabled = assertThrows(AppException.class, () -> service.grantGuestAccess("table-qr"));
    assertEquals(ErrorCode.TABLE_INACTIVE, disabled.getErrorCode());
    var unknown = assertThrows(AppException.class, () -> service.grantGuestAccess("unknown-qr"));
    assertEquals(ErrorCode.TABLE_NOT_FOUND, unknown.getErrorCode());
    assertTrue(sessions.isEmpty());
  }

  @Test void cannotCloseASessionOfAnotherTable() {
    var otherTable = new CafeTable();
    otherTable.setId(2L);
    var other = new TableSession();
    other.setId(9L);
    other.setTable(otherTable);
    sessions.put(9L, other);
    var error = assertThrows(AppException.class, () -> service.closeSession(1L, 9L));
    assertEquals(ErrorCode.TABLE_SESSION_NOT_FOUND, error.getErrorCode());
    assertEquals(TableSessionStatus.OPEN, other.getStatus());
  }

  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }
}
