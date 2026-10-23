package com.example.cafe.service;

import com.example.cafe.dto.response.TableSessionAccessResponse;
import com.example.cafe.dto.response.TableSessionResponse;

public interface TableSessionService {
  TableSessionResponse openSession(Long tableId);
  TableSessionResponse getOpenSession(Long tableId);
  TableSessionAccessResponse grantGuestAccess(String qrCode);
  TableSessionResponse getGuestSession(String token);
}
