package com.example.cafe.feature.employee.service;

import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.employee.dto.request.UpdateEmployeeRequest;
import com.example.cafe.feature.employee.dto.response.EmployeeResponse;

public interface EmployeeService {
  PageResponse<EmployeeResponse> getEmployees(int page, int size);
  EmployeeResponse updateEmployeeStatus(Long id, boolean active);
  EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request);
} 