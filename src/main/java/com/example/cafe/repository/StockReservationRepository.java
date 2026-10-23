package com.example.cafe.repository;

import com.example.cafe.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
  List<StockReservation> findByOrder_IdOrderByIdAsc(Long orderId);
}
