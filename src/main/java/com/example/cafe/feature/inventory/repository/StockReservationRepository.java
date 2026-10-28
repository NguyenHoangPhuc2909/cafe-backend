package com.example.cafe.feature.inventory.repository;

import com.example.cafe.common.entity.inventory.StockReservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
  List<StockReservation> findByOrder_IdOrderByIdAsc(Long orderId);
}
