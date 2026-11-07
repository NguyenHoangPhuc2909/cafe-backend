package com.example.cafe.feature.inventory.service;
import com.example.cafe.feature.inventory.dto.request.StockReceiptRequest;
import com.example.cafe.feature.inventory.dto.response.PackagedStockResponse;
import com.example.cafe.feature.inventory.dto.response.StockMovementResponse;

import java.util.List;


public interface InventoryService {
  public List<PackagedStockResponse> getPackagedStock();

  StockMovementResponse receiveStock(StockReceiptRequest request, String username);
}