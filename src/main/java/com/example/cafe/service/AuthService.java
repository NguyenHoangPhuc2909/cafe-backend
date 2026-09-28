package com.example.cafe.service;

import com.example.cafe.dto.request.LoginRequest;
import com.example.cafe.dto.request.RegisterRequest;
import com.example.cafe.dto.response.AuthResponse;

public interface AuthService {
  AuthResponse register(RegisterRequest request);
  AuthResponse login(LoginRequest request);
}
