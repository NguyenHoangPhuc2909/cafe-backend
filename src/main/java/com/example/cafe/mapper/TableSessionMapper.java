package com.example.cafe.mapper;

import com.example.cafe.dto.response.TableSessionResponse;
import com.example.cafe.entity.CafeTable;
import com.example.cafe.entity.TableSession;

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
