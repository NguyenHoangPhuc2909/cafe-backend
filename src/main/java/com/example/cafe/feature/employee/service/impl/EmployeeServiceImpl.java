package com.example.cafe.feature.employee.service.impl;

import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.employee.dto.request.UpdateEmployeeRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.employee.dto.response.EmployeeResponse;
import com.example.cafe.feature.employee.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<EmployeeResponse> getEmployees(int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(100, Math.max(1, size));

    var pageable = PageRequest.of(safePage, safeSize,
        Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")));

    var employees = userRepository.findByRole("STAFF", pageable);
    var responses = employees.map(this::toResponse);

    return PageResponse.from(responses);
  }

  @Override
  @Transactional
  public EmployeeResponse updateEmployeeStatus(Long id, boolean active) {
    User user = userRepository.findByIdAndRole(id, "STAFF")
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    user.setActive(active);
    return toResponse(user);
  }

  @Override
  @Transactional
  public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
    User user = userRepository.findByIdAndRole(id, "STAFF")
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    if(userRepository.existsByEmailAndIdNot(request.email(), id)) {
      throw new AppException(ErrorCode.EMAIL_EXISTED);
    }

    user.setFullName(request.fullName());
    user.setEmail(request.email());
    return toResponse(user);
  }

  private EmployeeResponse toResponse(User user) {
    return new EmployeeResponse(
        user.getId(),
        user.getUsername(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getCreatedAt(),
        user.isActive());
  }
}