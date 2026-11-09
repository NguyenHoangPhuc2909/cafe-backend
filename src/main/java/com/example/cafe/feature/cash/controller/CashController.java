package com.example.cafe.feature.cash.controller;

import com.example.cafe.common.response.*;
import com.example.cafe.feature.cash.dto.request.*;
import com.example.cafe.feature.cash.dto.response.*;
import com.example.cafe.feature.cash.service.CashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/staff/cash") @RequiredArgsConstructor
public class CashController {
  private final CashService service;

  @GetMapping("/today")
  public ResponseEntity<ApiResponse<CashDayResponse>> today(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) { return ok(service.today(page, size)); }

  @PostMapping("/movements")
  public ResponseEntity<ApiResponse<CashMovementResponse>> movement(
      @Valid @RequestBody CashMovementRequest request, Authentication authentication) {
    return ok(service.recordMovement(request, authentication.getName()));
  }

  @PostMapping("/reconciliations")
  public ResponseEntity<ApiResponse<CashReconciliationResponse>> reconcile(
      @Valid @RequestBody CashReconciliationRequest request, Authentication authentication) {
    return ok(service.reconcile(request, authentication.getName()));
  }

  @GetMapping("/reconciliations")
  public ResponseEntity<ApiResponse<PageResponse<CashReconciliationResponse>>> history(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ok(service.history(page, size));
  }

  private <T> ResponseEntity<ApiResponse<T>> ok(T data) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(data));
  }
}
