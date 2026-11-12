package com.example.cafe.feature.employee.dto.response;

import java.time.LocalDateTime;

public record EmployeeResponse(
  Long id,
  String username,
  String fullName,
  String email,
  String role,
  LocalDateTime createdAt,
  boolean isActive
) {} 
  
