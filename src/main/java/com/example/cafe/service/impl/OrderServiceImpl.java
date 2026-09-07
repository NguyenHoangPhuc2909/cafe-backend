package com.example.cafe.service.impl;

import com.example.cafe.dto.request.OrderRequest;
import com.example.cafe.dto.response.OrderResponse;
import com.example.cafe.entity.Order;
import com.example.cafe.entity.OrderItem;
import com.example.cafe.entity.Product;
import com.example.cafe.entity.User;
import com.example.cafe.exception.AppException;
import com.example.cafe.exception.ErrorCode;
import com.example.cafe.mapper.OrderMapper;
import com.example.cafe.repository.OrderItemRepository;
import com.example.cafe.repository.OrderRepository;
import com.example.cafe.repository.ProductRepository;
import com.example.cafe.repository.UserRepository;
import com.example.cafe.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;

  @Override
  public OrderResponse createOrder(OrderRequest request) {
    User user = userRepository.findById(request.getUserId())
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    Order order = new Order();
    order.setUser(user);
    order.setStatus("PENDING");

    order.setTotalAmount(BigDecimal.ZERO);
    order = orderRepository.save(order);

    BigDecimal totalAmount = BigDecimal.ZERO;
    List<OrderItem> saveItems = new ArrayList<>();

    for(OrderRequest.OrderItemRequest itemReq : request.getItems()){
      Product product = productRepository.findById(itemReq.getProductId())
          .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

      OrderItem orderItem = new OrderItem();
      orderItem.setOrder(order);
      orderItem.setProduct(product);
      orderItem.setQuantity(itemReq.getQuantity());
      orderItem.setPrice(product.getPrice());

      BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemReq.getQuantity()));
      totalAmount = totalAmount.add(itemTotal);

      saveItems.add(orderItemRepository.save(orderItem));
    }
      order.setTotalAmount(totalAmount);
      order.setNote(request.getNote());

      order = orderRepository.save(order);
      return OrderMapper.toResponse(order, saveItems);
  }

  @Override
  public OrderResponse getOrderById(Long id) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    List<OrderItem> orderItems = orderItemRepository.findByOrderId(id);
    return OrderMapper.toResponse(order, orderItems);
  }
}
