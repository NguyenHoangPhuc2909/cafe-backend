package com.example.cafe.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
  private String accessToken;
  @Builder.Default
  private String tokenType = "Bearer";
  private String username;
  private String role;
}
