package com.example.cafe.mapper;

import com.example.cafe.dto.response.CafeTableResponse;
import com.example.cafe.entity.CafeTable;

public class CafeTableMapper {
  public static CafeTableResponse toResponse(CafeTable cafeTable){
    return CafeTableResponse.builder()
        .id(cafeTable.getId())
        .name(cafeTable.getName())
        .qrCode(cafeTable.getQrCode())
        .isActive(cafeTable.getIsActive())
        .build();
  }
}
