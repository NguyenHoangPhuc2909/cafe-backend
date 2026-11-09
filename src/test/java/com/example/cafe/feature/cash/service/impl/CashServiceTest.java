package com.example.cafe.feature.cash.service.impl;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.cash.*;
import com.example.cafe.common.enums.CashMovementType;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.cash.dto.request.*;
import com.example.cafe.feature.cash.repository.*;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.support.RepositoryStub;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CashServiceTest {
  private final RepositoryStub<CashDayRepository> days = new RepositoryStub<>(CashDayRepository.class);
  private final RepositoryStub<CashMovementRepository> movements = new RepositoryStub<>(CashMovementRepository.class);
  private final RepositoryStub<CashReconciliationRepository> reports = new RepositoryStub<>(CashReconciliationRepository.class);
  private final RepositoryStub<PaymentRepository> payments = new RepositoryStub<>(PaymentRepository.class);
  private final RepositoryStub<UserRepository> users = new RepositoryStub<>(UserRepository.class);
  private final RepositoryStub<OrderRepository> orders = new RepositoryStub<>(OrderRepository.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);
  private final CashServiceImpl service = new CashServiceImpl(days.value, movements.value, reports.value, payments.value, users.value, orders.value, clock);
  private final LocalDate date = LocalDate.of(2026, 11, 9);
  private final CashDay day = new CashDay();
  private CashMovement savedMovement;
  private CashReconciliation savedReport;

  @BeforeEach void setup() {
    day.setBusinessDate(date); day.setRevision(2);
    var staff = new User(); staff.setId(7L); staff.setUsername("cashier"); staff.setFullName("Nhân viên");
    users.on("findByUsernameForUpdate", args -> { assertEquals("cashier", args[0]); return Optional.of(staff); });
    days.on("findById", args -> Optional.of(day));
    days.on("ensureDay", args -> null); days.on("findForUpdate", args -> Optional.of(day)); days.on("save", args -> args[0]);
    payments.on("summarizeSharedCashDay", args -> {
      assertEquals(Instant.parse("2026-11-08T17:00:00Z"), args[0]);
      assertEquals(Instant.parse("2026-11-09T17:00:00Z"), args[1]);
      return sales("800000", 5, 100);
    });
    movements.on("summarizeDay", args -> List.of(total(CashMovementType.PAYOUT, "100000")));
    movements.on("findByBusinessDateOrderByCreatedAtDescIdDesc", args -> Page.empty((Pageable) args[1]));
    movements.on("findByPerformedBy_IdAndRequestId", args -> Optional.ofNullable(savedMovement));
    movements.on("saveAndFlush", args -> { savedMovement = (CashMovement) args[0]; savedMovement.setId(11L); return savedMovement; });
    reports.on("findByPerformedBy_IdAndRequestId", args -> Optional.ofNullable(savedReport));
    reports.on("saveAndFlush", args -> { savedReport = (CashReconciliation) args[0]; savedReport.setId(21L); return savedReport; });
    orders.on("existsByStatusIn", args -> false);
  }

  private CashReconciliationRequest request(String actual, String note) {
    return new CashReconciliationRequest(UUID.randomUUID(), date, 2L, 5L, 100L, new BigDecimal(actual), note);
  }
  @Test void sharedTillUsesOpeningPlusAllCashSalesMinusPayoutsIndependentOfPagination() {
    var result = service.today(3, 10);
    assertMoney("500000", result.openingAmount()); assertMoney("800000", result.cashSales());
    assertMoney("1200000", result.expectedAmount()); assertEquals(5, result.cashPaymentCount());
    assertEquals(3, result.movements().getPage()); assertEquals("Asia/Ho_Chi_Minh", result.timeZone());
  }
  @Test void newDayDefaultsTo500kWithoutWritingDuringRead() {
    days.on("findById", args -> Optional.empty());
    payments.on("summarizeSharedCashDay", args -> sales("0", 0, 0)); movements.on("summarizeDay", args -> List.of());
    var result = service.today(0, 10);
    assertMoney("500000", result.expectedAmount()); assertEquals(0, result.revision());
    assertFalse(days.calls.containsKey("ensureDay"));
  }
  @Test void topUpsAreAddedAndSnapshotRecordsExactDifferenceWithoutAdjustingTheTill() {
    movements.on("summarizeDay", args -> List.of(total(CashMovementType.TOP_UP, "200000"), total(CashMovementType.PAYOUT, "100000")));
    var result = service.reconcile(request("1390000", "Thiếu 10k, kiểm tra bàn giao"), "cashier");
    assertMoney("1400000", result.expectedAmount()); assertMoney("-10000", result.difference());
    assertEquals("Nhân viên", result.performedByName()); assertEquals(2, day.getRevision());
    assertFalse(movements.calls.containsKey("saveAndFlush"));
  }
  @Test void matchingCountCanHaveAnEmptyNoteAndZeroCashIsNotAnEmptyInput() {
    var result = service.reconcile(request("1200000", ""), "cashier"); assertMoney("0", result.difference());
    savedReport = null;
    var zero = service.reconcile(request("0", "Đã bàn giao toàn bộ tiền, cần ghi lại khoản rút"), "cashier");
    assertMoney("0", zero.actualAmount()); assertMoney("-1200000", zero.difference());
  }
  @Test void discrepancyRequiresAnExplanation() {
    assertError(ErrorCode.CASH_NOTE_REQUIRED, () -> service.reconcile(request("1190000", " "), "cashier"));
    assertNull(savedReport);
  }
  @Test void newCashPaymentsOrMovementsRequireRefreshingThePreview() {
    payments.on("summarizeSharedCashDay", args -> sales("815000", 6, 101));
    assertError(ErrorCode.CASH_DATA_CHANGED, () -> service.reconcile(request("1200000", ""), "cashier"));
    payments.on("summarizeSharedCashDay", args -> sales("800000", 5, 100)); day.setRevision(3);
    assertError(ErrorCode.CASH_DATA_CHANGED, () -> service.reconcile(request("1200000", ""), "cashier"));
    assertNull(savedReport);
  }
  @Test void pendingOrPreparingOrdersBlockReconciliation() {
    orders.on("existsByStatusIn", args -> true);
    assertError(ErrorCode.CASH_ACTIVE_ORDERS, () -> service.reconcile(request("1200000", ""), "cashier"));
  }
  @Test void movementRetriesAreIdempotentAndCaptureActorAndReason() {
    var request = new CashMovementRequest(UUID.randomUUID(), date, CashMovementType.PAYOUT, new BigDecimal("100000"), " Mua ly ");
    var first = service.recordMovement(request, "cashier"); var retry = service.recordMovement(request, "cashier");
    assertEquals(first.id(), retry.id()); assertEquals("Mua ly", first.note()); assertEquals(7L, first.performedById());
    assertEquals(3, day.getRevision()); assertEquals(1, movements.calls.get("saveAndFlush"));
    var conflict = new CashMovementRequest(request.requestId(), date, CashMovementType.TOP_UP, request.amount(), request.note());
    assertError(ErrorCode.CASH_REQUEST_CONFLICT, () -> service.recordMovement(conflict, "cashier"));
  }
  @Test void savedReconciliationRetriesReturnTheOriginalSnapshotEvenAfterNewActivity() {
    var request = request("1200000", ""); var first = service.reconcile(request, "cashier");
    day.setRevision(99); orders.on("existsByStatusIn", args -> true);
    var retry = service.reconcile(request, "cashier");
    assertEquals(first.id(), retry.id()); assertEquals(1, reports.calls.get("saveAndFlush"));
    assertError(ErrorCode.CASH_REQUEST_CONFLICT, () -> service.reconcile(new CashReconciliationRequest(request.requestId(), date,
        2L, 5L, 100L, new BigDecimal("1100000"), "Khác"), "cashier"));
  }
  @Test void rejectsBackdatedEntriesAndFractionalNegativeOrOversizedCashInputs() {
    assertError(ErrorCode.CASH_DAY_CHANGED, () -> service.recordMovement(new CashMovementRequest(UUID.randomUUID(), date.minusDays(1),
        CashMovementType.PAYOUT, BigDecimal.ONE, "Mua hàng"), "cashier"));
    for (String amount : List.of("-1", "1.5", "1000000000000")) {
      assertError(ErrorCode.INVALID_CASH_ENTRY, () -> service.reconcile(request(amount, "Lệch"), "cashier"));
      assertError(ErrorCode.INVALID_CASH_ENTRY, () -> service.recordMovement(new CashMovementRequest(UUID.randomUUID(), date,
          CashMovementType.TOP_UP, new BigDecimal(amount), "Bổ sung"), "cashier"));
    }
  }
  @Test void vietnamMidnightChangesTheBusinessDateAndSavedRequestsRemainRetryable() {
    var movement = new CashMovementRequest(UUID.randomUUID(), date, CashMovementType.TOP_UP, BigDecimal.TEN, "Bổ sung");
    service.recordMovement(movement, "cashier");
    var tomorrow = new CashServiceImpl(days.value, movements.value, reports.value, payments.value, users.value, orders.value,
        Clock.fixed(Instant.parse("2026-11-09T17:00:00Z"), ZoneOffset.UTC));
    assertEquals(savedMovement.getId(), tomorrow.recordMovement(movement, "cashier").id());
    assertError(ErrorCode.CASH_DAY_CHANGED, () -> tomorrow.reconcile(request("1200000", ""), "cashier"));
  }
  private void assertMoney(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
  private void assertError(ErrorCode expected, Runnable action) { assertEquals(expected, assertThrows(AppException.class, action::run).getErrorCode()); }
  private PaymentRepository.CashTotal sales(String amount, long count, long lastId) {
    return new PaymentRepository.CashTotal() {
      public BigDecimal getAmount() { return new BigDecimal(amount); }
      public long getPaymentCount() { return count; }
      public long getLastPaymentId() { return lastId; }
    };
  }
  private CashMovementRepository.MovementTotal total(CashMovementType type, String amount) {
    return new CashMovementRepository.MovementTotal() {
      public CashMovementType getType() { return type; }
      public BigDecimal getAmount() { return new BigDecimal(amount); }
    };
  }
}
