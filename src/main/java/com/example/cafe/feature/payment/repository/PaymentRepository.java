package com.example.cafe.feature.payment.repository;


import com.example.cafe.common.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByOrder_Id(Long orderId);
}
