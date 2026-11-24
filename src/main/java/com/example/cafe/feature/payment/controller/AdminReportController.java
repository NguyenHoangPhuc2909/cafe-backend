package com.example.cafe.feature.payment.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.payment.dto.response.AdminRevenueReportResponse;
import com.example.cafe.feature.payment.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {
  private final AdminReportService adminReportService;

  @GetMapping("/revenue")
  public ResponseEntity<ApiResponse<AdminRevenueReportResponse>> getRevenueReport(
      @RequestParam(name = "fromDate", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate fromDate,
      @RequestParam(name = "toDate", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate toDate,
      @RequestParam(name = "topLimit", defaultValue = "10") int topLimit
  ) {
    AdminRevenueReportResponse report = adminReportService.getRevenueReport(fromDate, toDate, topLimit);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(report));
  }
}
