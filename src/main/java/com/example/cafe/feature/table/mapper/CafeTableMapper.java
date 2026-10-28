package com.example.cafe.feature.table.mapper;

import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.feature.table.dto.response.CafeTableResponse;


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
