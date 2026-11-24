package com.example.cafe.feature.order.service.impl;

import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.enums.OrderStatus;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.order.dto.response.OrderResponse;
import com.example.cafe.feature.order.mapper.OrderMapper;
import com.example.cafe.feature.order.repository.OrderItemRepository;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.order.service.AdminOrderService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<OrderResponse> getCompletedOrders(
      LocalDate fromDate,
      LocalDate toDate,
      Long sessionId,
      int page,
      int size
  ) {
    if (page < 0
        || size < 1
        || size > 100
        || (sessionId != null && sessionId <= 0)
        || (fromDate != null && toDate != null && fromDate.isAfter(toDate))
        || LocalDate.MAX.equals(toDate)) {
      throw new AppException(ErrorCode.INVALID_ORDER_QUERY);
    }

    LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
    LocalDateTime toExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

    Specification<Order> spec = (root, query, cb) -> {
      var conditions = new ArrayList<Predicate>();
      conditions.add(cb.equal(root.get("status"), OrderStatus.COMPLETED));

      if (fromDateTime != null) {
        conditions.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
      }
      if (toExclusive != null) {
        conditions.add(cb.lessThan(root.get("createdAt"), toExclusive));
      }
      if (sessionId != null) {
        conditions.add(cb.equal(root.get("tableSession").get("id"), sessionId));
      }

      return cb.and(conditions.toArray(new Predicate[0]));
    };

    var pageable = PageRequest.of(
        page,
        size,
        Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
        )
    );

    var orders = orderRepository.findAll(spec, pageable);
    var responses = orders.map(order -> OrderMapper.toResponse(
        order,
        orderItemRepository.findByOrderId(order.getId())
    ));

    return PageResponse.from(responses);
  }
}
