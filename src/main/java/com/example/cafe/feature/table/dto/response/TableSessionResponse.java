package com.example.cafe.feature.table.dto.response;

import com.example.cafe.common.enums.TableSessionStatus;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class TableSessionResponse {
  private Long id;
  private Long tableId;
  private String tableName;
  private TableSessionStatus status;
  private LocalDateTime openedAt;
  private LocalDateTime closedAt;
}
