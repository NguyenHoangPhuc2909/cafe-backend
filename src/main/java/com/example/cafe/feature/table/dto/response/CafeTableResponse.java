package com.example.cafe.feature.table.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CafeTableResponse {
  private Long id;
  private String name;
  private String qrCode;
  private boolean isActive;
}
