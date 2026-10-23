package com.example.cafe.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TableSessionAccessResponse {
  private Long sessionId;
  private Long tableId;
  private String tableName;
  private String accessToken;
  private Instant expiresAt;
}
