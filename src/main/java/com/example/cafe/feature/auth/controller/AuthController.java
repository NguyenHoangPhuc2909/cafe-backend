package com.example.cafe.feature.auth.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.auth.dto.request.LoginRequest;
import com.example.cafe.feature.auth.dto.request.RegisterRequest;
import com.example.cafe.feature.auth.dto.response.AuthResponse;
import com.example.cafe.feature.auth.dto.response.StaffAccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<ApiResponse<StaffAccountResponse>> register(@Valid @RequestBody RegisterRequest request) {
    StaffAccountResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.<StaffAccountResponse>builder()
            .success(true).message("Đã tạo tài khoản nhân viên").data(response).build());
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
