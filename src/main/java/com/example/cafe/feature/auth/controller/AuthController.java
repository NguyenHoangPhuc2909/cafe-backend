package com.example.cafe.feature.auth.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.auth.dto.request.LoginRequest;
import com.example.cafe.feature.auth.dto.request.RegisterRequest;
import com.example.cafe.feature.auth.dto.response.AuthResponse;
import com.example.cafe.feature.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ApiResponse.<AuthResponse>builder()
        .success(true)
        .message("Đăng ký tài khoản thành công")
        .data(response)
        .build();
  }

  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ApiResponse.<AuthResponse>builder()
        .success(true)
        .message("Đăng nhập thành công")
        .data(response)
        .build();
  }
}
