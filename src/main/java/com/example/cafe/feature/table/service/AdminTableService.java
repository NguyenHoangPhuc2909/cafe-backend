package com.example.cafe.feature.table.service;

import com.example.cafe.feature.table.dto.request.AdminTableRequest;
import com.example.cafe.feature.table.dto.request.UpdateTableStatusRequest;
import com.example.cafe.feature.table.dto.response.AdminTableResponse;

import java.util.List;

public interface AdminTableService {
  List<AdminTableResponse> list();

  AdminTableResponse create(AdminTableRequest request);

  AdminTableResponse rename(Long id, AdminTableRequest request);

  AdminTableResponse updateStatus(Long id, UpdateTableStatusRequest request);

  void delete(Long id);
}
