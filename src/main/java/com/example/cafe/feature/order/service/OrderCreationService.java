package com.example.cafe.feature.order.service;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.feature.order.dto.request.CreateOrderRequest;
import com.example.cafe.feature.order.dto.response.OrderResponse;

public interface OrderCreationService {
  OrderResponse create(TableSession session, User createdBy, CreateOrderRequest request);
}