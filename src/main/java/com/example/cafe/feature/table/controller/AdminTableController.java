package com.example.cafe.feature.table.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.table.dto.request.AdminTableRequest;
import com.example.cafe.feature.table.dto.request.UpdateTableStatusRequest;
import com.example.cafe.feature.table.dto.response.AdminTableResponse;
import com.example.cafe.feature.table.service.AdminTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tables")
@RequiredArgsConstructor
public class AdminTableController {
  private final AdminTableService adminTableService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<AdminTableResponse>>> list() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(adminTableService.list()));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<AdminTableResponse>> create(
      @Valid @RequestBody AdminTableRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(adminTableService.create(request)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<AdminTableResponse>> rename(
      @PathVariable("id") Long id,
      @Valid @RequestBody AdminTableRequest request
  ) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(adminTableService.rename(id, request)));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<AdminTableResponse>> updateStatus(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateTableStatusRequest request
  ) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(adminTableService.updateStatus(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
    adminTableService.delete(id);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(null));
  }
}
