package com.example.cafe.feature.order.scheduler;

import com.example.cafe.common.enums.OrderPaymentStatus;
import com.example.cafe.feature.order.repository.OrderRepository;
import com.example.cafe.feature.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationScheduler {
  private final OrderRepository orderRepository;
  private final OrderService orderService;

  @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
  public void expireUnpaidOrders(){
    List<Long> orderIds = orderRepository.findExpiredOrderIds(Instant.now(), OrderPaymentStatus.UNPAID, PageRequest.of(0, 100));

    for(Long orderId : orderIds) {
      try {
        boolean expired = orderService.expireUnpaidOrder(orderId);

        if(expired) {
          log.info("Đã xử lý hết hạn đơn {}", orderId);
        }
      } catch (RuntimeException exception) {
        log.error("Không thể xử lý hết hạn đơn {}", orderId, exception);
      }
    }
  }
}
