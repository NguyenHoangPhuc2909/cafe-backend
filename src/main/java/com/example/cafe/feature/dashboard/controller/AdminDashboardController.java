package com.example.cafe.feature.dashboard.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.dashboard.dto.response.AdminDashboardResponse;
import com.example.cafe.feature.dashboard.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
  private final AdminDashboardService adminDashboardService;

  @GetMapping
  public ResponseEntity<ApiResponse<AdminDashboardResponse>> getSummary() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(adminDashboardService.getSummary()));
  }
}
