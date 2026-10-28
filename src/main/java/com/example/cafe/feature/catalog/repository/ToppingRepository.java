package com.example.cafe.feature.catalog.repository;

import com.example.cafe.common.entity.catalog.Topping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, Long> {
}
