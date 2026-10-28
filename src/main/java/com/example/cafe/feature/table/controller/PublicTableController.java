package com.example.cafe.feature.table.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.table.dto.response.CafeTableResponse;
import com.example.cafe.feature.table.dto.response.TableSessionAccessResponse;
import com.example.cafe.feature.table.service.CafeTableService;
import com.example.cafe.feature.table.service.TableSessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/tables")
@RequiredArgsConstructor
public class PublicTableController {
  private final CafeTableService cafeTableService;
  private final TableSessionService tableSessionService;

  @GetMapping("/{qrCode}")
  public ApiResponse<CafeTableResponse> getTableByQrCode(@PathVariable("qrCode") String qrCode){
    return ApiResponse.ok(cafeTableService.getTableByQrCode(qrCode));
  }

  @PostMapping("{qrCode}/access")
  public ResponseEntity<ApiResponse<TableSessionAccessResponse>> grantGuestAccess(@PathVariable("qrCode") String qrCode){
    TableSessionAccessResponse response = tableSessionService.grantGuestAccess(qrCode);

    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }
}
