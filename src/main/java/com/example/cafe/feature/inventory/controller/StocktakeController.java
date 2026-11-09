package com.example.cafe.feature.inventory.controller;

import com.example.cafe.common.entity.inventory.StocktakeItem;
import com.example.cafe.common.response.*;
import com.example.cafe.feature.inventory.dto.request.StocktakeRequest;
import com.example.cafe.feature.inventory.dto.response.StocktakeResponse;
import com.example.cafe.feature.inventory.service.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/staff/inventory/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {
  private final StocktakeService service;

  @GetMapping("/preview")
  public ResponseEntity<ApiResponse<List<StocktakeItem>>> preview() {
    return ok(service.preview());
  }
  @PostMapping
  public ResponseEntity<ApiResponse<StocktakeResponse>> save(
      @Valid @RequestBody StocktakeRequest request, Authentication authentication) {
    return ok(service.save(request, authentication.getName()));
  }
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<StocktakeResponse>>> history(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ok(service.history(page, size));
  }
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<StocktakeResponse>> detail(@PathVariable Long id) {
    return ok(service.detail(id));
  }
  private <T> ResponseEntity<ApiResponse<T>> ok(T data) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(data));
  }
}
