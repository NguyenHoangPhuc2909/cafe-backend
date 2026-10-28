package com.example.cafe.feature.table.mapper;

import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.feature.table.dto.response.TableSessionResponse;


public class TableSessionMapper {
  public static TableSessionResponse toResponse(TableSession session) {
    CafeTable table = session.getTable();

    return TableSessionResponse.builder()
        .id(session.getId())
        .tableId(table.getId())
        .tableName(table.getName())
        .status(session.getStatus())
        .openedAt(session.getOpenedAt())
        .closedAt(session.getClosedAt())
        .build();
  }
}
