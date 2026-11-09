package com.example.cafe.feature.cash.service;

import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.cash.dto.request.*;
import com.example.cafe.feature.cash.dto.response.*;

public interface CashService {
  CashDayResponse today(int page, int size);
  CashMovementResponse recordMovement(CashMovementRequest request, String username);
  CashReconciliationResponse reconcile(CashReconciliationRequest request, String username);
  PageResponse<CashReconciliationResponse> history(int page, int size);
}
