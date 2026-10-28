package com.example.cafe.feature.table.service;

import com.example.cafe.feature.table.dto.response.TableSessionAccessResponse;
import com.example.cafe.feature.table.dto.response.TableSessionResponse;


public interface TableSessionService {
  TableSessionResponse openSession(Long tableId);
  TableSessionResponse getOpenSession(Long tableId);
  TableSessionAccessResponse grantGuestAccess(String qrCode);
  TableSessionResponse getGuestSession(String token);
}
