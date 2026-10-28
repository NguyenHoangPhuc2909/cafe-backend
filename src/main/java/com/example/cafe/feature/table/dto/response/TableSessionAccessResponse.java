package com.example.cafe.feature.table.dto.response;

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
