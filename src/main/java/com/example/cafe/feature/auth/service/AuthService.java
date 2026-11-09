package com.example.cafe.feature.auth.service;

import com.example.cafe.feature.auth.dto.request.LoginRequest;
import com.example.cafe.feature.auth.dto.request.RegisterRequest;
import com.example.cafe.feature.auth.dto.response.AuthResponse;
import com.example.cafe.feature.auth.dto.response.StaffAccountResponse;


public interface AuthService {
  StaffAccountResponse register(RegisterRequest request);
  AuthResponse login(LoginRequest request);
}
