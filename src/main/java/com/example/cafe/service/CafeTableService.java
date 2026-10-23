package com.example.cafe.service;

import com.example.cafe.dto.request.CafeTableRequest;
import com.example.cafe.dto.response.CafeTableResponse;

import java.util.List;

public interface CafeTableService {
  CafeTableResponse createTable(CafeTableRequest request);

  List<CafeTableResponse> getAllTables();

  CafeTableResponse getTableByQrCode(String qrCode);
}
