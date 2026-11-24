package com.example.cafe.feature.order.repository;

import com.example.cafe.common.entity.order.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  List<OrderItem> findByOrderId(Long orderId);

  interface TopProductAggregate {
    Long getProductId();
    String getProductName();
    Long getQuantitySold();
    BigDecimal getRevenue();
  }

  @Query("""
      SELECT oi.product.id AS productId,
             oi.product.name AS productName,
             SUM(oi.quantity) AS quantitySold,
             SUM(oi.price * oi.quantity) AS revenue
      FROM OrderItem oi
      JOIN oi.order o
      JOIN Payment p ON p.order = o
      WHERE o.status = com.example.cafe.common.enums.OrderStatus.COMPLETED
        AND o.paymentStatus = com.example.cafe.common.enums.OrderPaymentStatus.PAID
        AND p.paidAt >= :from
        AND p.paidAt < :until
      GROUP BY oi.product.id, oi.product.name
      ORDER BY SUM(oi.quantity) DESC, SUM(oi.price * oi.quantity) DESC, oi.product.id ASC
      """)
  List<TopProductAggregate> findTopProductsCompletedInRange(
      @Param("from") Instant from,
      @Param("until") Instant until,
      Pageable pageable
  );
}
