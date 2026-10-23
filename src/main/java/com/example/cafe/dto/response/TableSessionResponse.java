package com.example.cafe.dto.response;

import com.example.cafe.entity.TableSessionStatus;
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
