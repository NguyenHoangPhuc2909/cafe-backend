package com.example.cafe.feature.inventory.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.inventory.dto.request.StockReceiptRequest;
import com.example.cafe.feature.inventory.dto.response.PackagedStockResponse;
import com.example.cafe.feature.inventory.dto.response.StockMovementResponse;
import com.example.cafe.feature.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/inventory")
@RequiredArgsConstructor
public class InventoryController {
  private final InventoryService inventoryService;

  @GetMapping("/packaged")
  public ResponseEntity<ApiResponse<List<PackagedStockResponse>>> getPackagedStock() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(inventoryService.getPackagedStock()));
  }

  @PostMapping("/receipts")
  public ResponseEntity<ApiResponse<StockMovementResponse>> receiveStock(
      @Valid @RequestBody StockReceiptRequest request,
      Authentication authentication
  ) {
    var response = inventoryService.receiveStock(
        request,
        authentication.getName()
    );

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }

  @GetMapping("/receipts")
  public ResponseEntity<ApiResponse<PageResponse<StockMovementResponse>>>
  getReceiptHistory(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(
            inventoryService.getReceiptHistory(page, size)
        ));
  }
}