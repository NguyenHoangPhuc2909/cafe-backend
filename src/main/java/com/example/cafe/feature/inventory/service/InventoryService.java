package com.example.cafe.feature.inventory.service;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.inventory.dto.request.StockReceiptRequest;
import com.example.cafe.feature.inventory.dto.response.PackagedStockResponse;
import com.example.cafe.feature.inventory.dto.response.StockMovementResponse;

import java.time.LocalDate;
import java.util.List;


public interface InventoryService {
  public List<PackagedStockResponse> getPackagedStock();

  StockMovementResponse receiveStock(StockReceiptRequest request, String username);
  PageResponse<StockMovementResponse> getReceiptHistory(
      int page,
      int size,
      LocalDate fromDate,
      LocalDate toDate,
      Long ingredientId,
      Long productId
  );
}