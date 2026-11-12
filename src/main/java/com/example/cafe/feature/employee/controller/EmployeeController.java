package com.example.cafe.feature.employee.controller;

import com.example.cafe.feature.employee.dto.request.UpdateEmployeeRequest;
import com.example.cafe.feature.employee.dto.request.UpdateEmployeeStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.employee.dto.response.EmployeeResponse;
import com.example.cafe.feature.employee.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
public class EmployeeController {
  private final EmployeeService employeeService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getEmployees(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    var response = employeeService.getEmployees(page, size);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<EmployeeResponse> updateEmployeeStatus(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateEmployeeStatusRequest request
  ) {
    return ApiResponse.ok(
        employeeService.updateEmployeeStatus(id, request.isActive()));
  }

  @PatchMapping("/{id}")
  public ApiResponse<EmployeeResponse> updateEmployee(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateEmployeeRequest request
  ) {
    return ApiResponse.ok(employeeService.updateEmployee(id, request));
  }
}
