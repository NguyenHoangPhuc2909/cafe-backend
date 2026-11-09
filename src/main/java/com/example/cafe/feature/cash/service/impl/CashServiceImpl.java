package com.example.cafe.feature.cash.service.impl;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.cash.*;
import com.example.cafe.common.enums.CashMovementType;
import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.*;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.cash.dto.request.*;
import com.example.cafe.feature.cash.dto.response.*;
import com.example.cafe.feature.cash.repository.*;
import com.example.cafe.feature.cash.service.CashService;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.*;

@Service
public class CashServiceImpl implements CashService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final BigDecimal OPENING = new BigDecimal("500000");
  private static final BigDecimal MAX_INPUT = new BigDecimal("999999999999");
  private final CashDayRepository days;
  private final CashMovementRepository movements;
  private final CashReconciliationRepository reconciliations;
  private final PaymentRepository payments;
  private final UserRepository users;
  private final OrderRepository orders;
  private final Clock clock;

  @Autowired
  public CashServiceImpl(CashDayRepository days, CashMovementRepository movements,
      CashReconciliationRepository reconciliations, PaymentRepository payments,
      UserRepository users, OrderRepository orders) {
    this(days, movements, reconciliations, payments, users, orders, Clock.systemUTC());
  }
  CashServiceImpl(CashDayRepository days, CashMovementRepository movements,
      CashReconciliationRepository reconciliations, PaymentRepository payments,
      UserRepository users, OrderRepository orders, Clock clock) {
    this.days = days; this.movements = movements; this.reconciliations = reconciliations;
    this.payments = payments; this.users = users; this.orders = orders; this.clock = clock;
  }

  private LocalDate date() { return clock.instant().atZone(ZONE).toLocalDate(); }
  private User staff(String username) {
    return users.findByUsernameForUpdate(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }
  private String staffName(User staff) {
    return staff.getFullName() == null || staff.getFullName().isBlank() ? staff.getUsername() : staff.getFullName();
  }
  private void requireToday(LocalDate date) {
    if (!date().equals(date)) throw new AppException(ErrorCode.CASH_DAY_CHANGED);
  }
  private CashDay lockDay(LocalDate date) {
    days.ensureDay(date);
    return days.findForUpdate(date).orElseThrow(() -> new AppException(ErrorCode.CASH_DATA_CHANGED));
  }
  private boolean validMoney(BigDecimal value, boolean positive) {
    return value != null && value.signum() >= (positive ? 1 : 0)
        && value.compareTo(MAX_INPUT) <= 0 && value.stripTrailingZeros().scale() <= 0;
  }
  private boolean validNote(String note) { return note != null && note.length() <= 500; }
  private boolean sameMoney(BigDecimal first, BigDecimal second) { return first.compareTo(second) == 0; }
  private Pageable page(int page, int size) {
    return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
  }
  private record Balance(BigDecimal opening, BigDecimal sales, long paymentCount, long lastPaymentId,
      long revision, BigDecimal topUps, BigDecimal payouts, BigDecimal expected) {}

  private Balance balance(LocalDate date, CashDay day) {
    var sales = payments.summarizeSharedCashDay(date.atStartOfDay(ZONE).toInstant(),
        date.plusDays(1).atStartOfDay(ZONE).toInstant());
    BigDecimal topUps = BigDecimal.ZERO, payouts = BigDecimal.ZERO;
    for (var total : movements.summarizeDay(date)) {
      if (total.getType() == CashMovementType.TOP_UP) topUps = total.getAmount();
      else if (total.getType() == CashMovementType.PAYOUT) payouts = total.getAmount();
    }
    BigDecimal opening = day == null ? OPENING : day.getOpeningAmount();
    return new Balance(opening, sales.getAmount(), sales.getPaymentCount(), sales.getLastPaymentId(),
        day == null ? 0 : day.getRevision(), topUps, payouts, opening.add(sales.getAmount()).add(topUps).subtract(payouts));
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public CashDayResponse today(int page, int size) {
    var date = date();
    var balance = balance(date, days.findById(date).orElse(null));
    var entries = movements.findByBusinessDateOrderByCreatedAtDescIdDesc(date, page(page, size));
    return new CashDayResponse(date, ZONE.getId(), clock.instant(), balance.opening(), balance.sales(),
        balance.paymentCount(), balance.lastPaymentId(), balance.revision(), balance.topUps(), balance.payouts(),
        balance.expected(), PageResponse.from(entries.map(this::response)));
  }

  @Override
  @Transactional
  public CashMovementResponse recordMovement(CashMovementRequest request, String username) {
    if (request == null || request.requestId() == null || request.businessDate() == null || request.type() == null
        || !validMoney(request.amount(), true) || !validNote(request.note()) || request.note().isBlank())
      throw new AppException(ErrorCode.INVALID_CASH_ENTRY);
    var staff = staff(username);
    var existing = movements.findByPerformedBy_IdAndRequestId(staff.getId(), request.requestId());
    if (existing.isPresent()) {
      var saved = existing.get();
      if (!saved.getBusinessDate().equals(request.businessDate()) || saved.getType() != request.type()
          || !sameMoney(saved.getAmount(), request.amount()) || !saved.getNote().equals(request.note().strip()))
        throw new AppException(ErrorCode.CASH_REQUEST_CONFLICT);
      return response(saved);
    }
    requireToday(request.businessDate());
    var day = lockDay(request.businessDate());
    requireToday(request.businessDate());
    var entry = new CashMovement();
    entry.setBusinessDate(request.businessDate()); entry.setRequestId(request.requestId());
    entry.setType(request.type()); entry.setAmount(request.amount()); entry.setNote(request.note().strip());
    entry.setPerformedBy(staff); entry.setPerformedByName(staffName(staff)); entry.setCreatedAt(clock.instant());
    day.setRevision(Math.addExact(day.getRevision(), 1));
    days.save(day);
    return response(movements.saveAndFlush(entry));
  }

  @Override
  @Transactional
  public CashReconciliationResponse reconcile(CashReconciliationRequest request, String username) {
    if (request == null || request.requestId() == null || request.businessDate() == null
        || request.expectedRevision() == null || request.expectedRevision() < 0
        || request.expectedPaymentCount() == null || request.expectedPaymentCount() < 0
        || request.expectedLastPaymentId() == null || request.expectedLastPaymentId() < 0
        || !validMoney(request.actualAmount(), false) || !validNote(request.note()))
      throw new AppException(ErrorCode.INVALID_CASH_ENTRY);
    var staff = staff(username);
    var existing = reconciliations.findByPerformedBy_IdAndRequestId(staff.getId(), request.requestId());
    if (existing.isPresent()) {
      var saved = existing.get();
      if (!saved.getBusinessDate().equals(request.businessDate()) || !sameMoney(saved.getActualAmount(), request.actualAmount())
          || saved.getRevision() != request.expectedRevision() || saved.getCashPaymentCount() != request.expectedPaymentCount()
          || saved.getLastPaymentId() != request.expectedLastPaymentId() || !saved.getNote().equals(request.note().strip()))
        throw new AppException(ErrorCode.CASH_REQUEST_CONFLICT);
      return response(saved);
    }
    requireToday(request.businessDate());
    var day = lockDay(request.businessDate());
    requireToday(request.businessDate());
    if (orders.existsByStatusIn(java.util.List.of(OrderStatus.PENDING, OrderStatus.PREPARING)))
      throw new AppException(ErrorCode.CASH_ACTIVE_ORDERS);
    var balance = balance(request.businessDate(), day);
    if (balance.revision() != request.expectedRevision() || balance.paymentCount() != request.expectedPaymentCount()
        || balance.lastPaymentId() != request.expectedLastPaymentId()) throw new AppException(ErrorCode.CASH_DATA_CHANGED);
    BigDecimal difference = request.actualAmount().subtract(balance.expected());
    if (difference.signum() != 0 && request.note().isBlank()) throw new AppException(ErrorCode.CASH_NOTE_REQUIRED);
    var report = new CashReconciliation();
    report.setBusinessDate(request.businessDate()); report.setRequestId(request.requestId());
    report.setPerformedBy(staff); report.setPerformedByName(staffName(staff)); report.setCreatedAt(clock.instant());
    report.setOpeningAmount(balance.opening()); report.setCashSales(balance.sales());
    report.setCashPaymentCount(balance.paymentCount()); report.setLastPaymentId(balance.lastPaymentId());
    report.setRevision(balance.revision()); report.setTopUps(balance.topUps()); report.setPayouts(balance.payouts());
    report.setExpectedAmount(balance.expected()); report.setActualAmount(request.actualAmount());
    report.setDifference(difference); report.setNote(request.note().strip());
    // An immutable observation, not a cash adjustment or a closure of the business day.
    return response(reconciliations.saveAndFlush(report));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<CashReconciliationResponse> history(int page, int size) {
    var pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)),
        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    return PageResponse.from(reconciliations.findAll(pageable).map(this::response));
  }

  private CashMovementResponse response(CashMovement entry) {
    return new CashMovementResponse(entry.getId(), entry.getBusinessDate(), entry.getType(), entry.getAmount(),
        entry.getNote(), entry.getPerformedBy().getId(), entry.getPerformedByName(), entry.getCreatedAt());
  }
  private CashReconciliationResponse response(CashReconciliation report) {
    return new CashReconciliationResponse(report.getId(), report.getBusinessDate(), report.getPerformedBy().getId(),
        report.getPerformedByName(), report.getCreatedAt(), report.getOpeningAmount(), report.getCashSales(),
        report.getCashPaymentCount(), report.getLastPaymentId(), report.getRevision(), report.getTopUps(), report.getPayouts(),
        report.getExpectedAmount(), report.getActualAmount(), report.getDifference(), report.getNote());
  }
}
