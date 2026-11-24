package com.example.cafe.feature.payment.service;

import com.example.cafe.feature.payment.dto.response.AdminRevenueReportResponse;

import java.time.LocalDate;

public interface AdminReportService {
  AdminRevenueReportResponse getRevenueReport(
      LocalDate fromDate,
      LocalDate toDate,
      int topLimit
  );
}
