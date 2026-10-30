package com.example.cafe.feature.order.repository;

import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.enums.OrderPaymentStatus;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.id = :id")
  Optional<Order> findByIdForUpdate(@Param("id") Long id);

  @Query("""
      SELECT o.id
      FROM Order o
      WHERE o.tableSession IS NOT NULL
        AND o.status = 'PENDING'
        AND o.paymentStatus = :paymentStatus
        AND o.paymentExpiresAt <= :now
      ORDER BY o.id ASC
      """)
  List<Long> findExpiredOrderIds(
      @Param("now") Instant now,
      @Param("paymentStatus") OrderPaymentStatus paymentStatus,
      Pageable pageable
  );

  // Khách xem một đơn thuộc phiên bàn của mình
  Optional<Order> findByIdAndTableSession_Id(Long orderId, Long sessionId);

  // Danh sách đơn
  @Query("""
          SELECT o FROM Order o
          WHERE (:status IS NULL OR o.status = :status)
            AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
            AND (:sessionId IS NULL OR o.tableSession.id = :sessionId)
      """)
  Page<Order> findForStaff(
      @Param("status") String status,
      @Param("paymentStatus") OrderPaymentStatus paymentStatus,
      @Param("sessionId") Long sessionId,
      Pageable pageable
  );
}
