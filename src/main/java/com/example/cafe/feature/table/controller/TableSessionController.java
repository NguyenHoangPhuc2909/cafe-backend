package com.example.cafe.feature.table.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.table.dto.response.TableSessionResponse;
import com.example.cafe.feature.table.service.TableSessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/tables")
@RequiredArgsConstructor
public class TableSessionController {
  private final TableSessionService tableSessionService;

  @PostMapping("/{tableId}/sessions")
  public ResponseEntity<ApiResponse<TableSessionResponse>> openSession(@PathVariable("tableId") Long tableId) {
    TableSessionResponse response = tableSessionService.openSession(tableId);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @GetMapping("/{tableId}/sessions/current")
  public ApiResponse<TableSessionResponse> getOpenSession(@PathVariable("tableId") Long tableId){
    return ApiResponse.ok(tableSessionService.getOpenSession(tableId));
  }
}
