package com.example.cafe.service.impl;

import com.example.cafe.dto.request.LoginRequest;
import com.example.cafe.dto.request.RegisterRequest;
import com.example.cafe.dto.response.AuthResponse;
import com.example.cafe.entity.User;
import com.example.cafe.exception.AppException;
import com.example.cafe.exception.ErrorCode;
import com.example.cafe.repository.UserRepository;
import com.example.cafe.security.JwtTokenProvider;
import com.example.cafe.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new AppException(ErrorCode.USERNAME_EXISTED);
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new AppException(ErrorCode.EMAIL_EXISTED);
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setEmail(request.getEmail());
    user.setFullName(request.getFullName());
    user.setRole("CUSTOMER");
    userRepository.save(user);

    String token = jwtTokenProvider.generateToken(user.getUsername());

    return AuthResponse.builder().accessToken(token).username(user.getUsername()).role(user.getRole()).build();
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    try {
      // 1. Xác thực username và password với Spring Security
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
      );

      // 2. Tìm thông tin User trong DB
      User user = userRepository.findByUsername(request.getUsername())
          .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

      // 3. Sinh JWT Token và trả về response
      String token = jwtTokenProvider.generateToken(user.getUsername());
      return AuthResponse.builder()
          .accessToken(token)
          .username(user.getUsername())
          .role(user.getRole())
          .build();
    } catch (Exception e) {
      throw new AppException(ErrorCode.INVALID_CREDENTIALS);
    }
  }
}
