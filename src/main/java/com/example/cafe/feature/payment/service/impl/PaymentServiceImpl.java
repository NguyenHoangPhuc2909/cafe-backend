package com.example.cafe.feature.payment.service.impl;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.payment.Payment;
import com.example.cafe.common.enums.OrderPaymentStatus;
import com.example.cafe.common.enums.PaymentMethod;
import com.example.cafe.common.enums.StockReservationStatus;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.inventory.repository.StockReservationRepository;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.payment.dto.response.PaymentResponse;
import com.example.cafe.feature.payment.repository.PaymentRepository;
import com.example.cafe.feature.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final StockReservationRepository stockReservationRepository;

  @Override
  @Transactional
  public PaymentResponse confirmPayment(Long orderId, PaymentMethod method, String username) {
    if (orderId == null || orderId <= 0 || method == null) {
      throw new AppException(ErrorCode.FIELD_REQUIRED);
    }

    if (username == null || username.isBlank()) {
      throw new AppException(ErrorCode.INVALID_CREDENTIALS);
    }

    User staff = userRepository.findByUsername(username)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    if (!"STAFF".equals(staff.getRole()) && !"ADMIN".equals(staff.getRole())) {
      throw new AppException(ErrorCode.PAYMENT_FORBIDDEN);
    }

    // Khoá đơn
    Order order = orderRepository.findByIdForUpdate(orderId)
        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    // Request lặp: trả lại kqua cũ nếu nhất quán
    var existing = paymentRepository.findByOrder_Id(orderId);

    if (existing.isPresent()) {
      Payment payment = existing.get();

      if (order.getPaymentStatus() != OrderPaymentStatus.PAID
          || payment.getMethod() != method
          || payment.getAmount() == null
          || order.getTotalAmount() == null
          || payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
        throw new AppException(ErrorCode.PAYMENT_CONFLICT);
      }
      return toResponse(payment);
    }

    if ("EXPIRED".equals(order.getStatus())) {
      throw new AppException(ErrorCode.PAYMENT_EXPIRED);
    }

    // Chỉ nhận đơn đang chờ trả và còn hạn thanh toán
    if (!"PENDING".equals(order.getStatus()) || order.getPaymentStatus() != OrderPaymentStatus.UNPAID
        || order.getPaymentExpiresAt() == null) {
      throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
    }

    if (order.getTotalAmount() == null || order.getTotalAmount().signum() < 0) {
      throw new AppException(ErrorCode.PAYMENT_DATA_INVALID);
    }

    // Các bản ghi giữ hàng phải còn HELD
    var reservations = stockReservationRepository.findByOrder_IdOrderByIdAsc(orderId);
    if (reservations.isEmpty() || reservations.stream()
        .anyMatch(reservation -> reservation.getStatus() != StockReservationStatus.HELD)) {
      throw new AppException(ErrorCode.PAYMENT_DATA_INVALID);
    }

    // Lấy thời điểm sau khi đã khóa và đọc dữ liệu cần kiểm tra
    Instant now = Instant.now();
    if (!order.getPaymentExpiresAt().isAfter(now)) {
      throw new AppException(ErrorCode.PAYMENT_EXPIRED);
    }

    // Ghi nhận tiền theo tổng BE, không lấy số tiền từ FE
    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setMethod(method);
    payment.setAmount(order.getTotalAmount());
    payment.setPaidAt(now);
    payment.setConfirmedBy(staff);

    order.setPaymentStatus(OrderPaymentStatus.PAID);

    for (var reservation : reservations) {
      reservation.setStatus(StockReservationStatus.CONSUMED);
    }

    Payment saved = paymentRepository.save(payment);
    return toResponse(saved);
  }

  private PaymentResponse toResponse(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getOrder().getId(),
        payment.getMethod(),
        payment.getAmount(),
        payment.getPaidAt(),
        payment.getConfirmedBy().getId()
    );
  }
}
