package com.example.cafe.controller;

import com.example.cafe.dto.request.CafeTableRequest;
import com.example.cafe.dto.response.ApiResponse;
import com.example.cafe.dto.response.CafeTableResponse;
import com.example.cafe.service.CafeTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/tables")
@RequiredArgsConstructor
public class CafeTableController {
  private final CafeTableService cafeTableService;

  @PostMapping
  public ResponseEntity<ApiResponse<CafeTableResponse>> createTable(@Valid @RequestBody CafeTableRequest request) {
    CafeTableResponse response = cafeTableService.createTable(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response));
  }

  @GetMapping
  public ApiResponse<List<CafeTableResponse>> getAllTables(){
    return ApiResponse.ok(cafeTableService.getAllTables());
  }
}
