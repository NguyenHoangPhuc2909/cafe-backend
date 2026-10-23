package com.example.cafe.controller;

import com.example.cafe.dto.response.ApiResponse;
import com.example.cafe.dto.response.TableSessionResponse;
import com.example.cafe.service.TableSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestSessionController {
  private final TableSessionService tableSessionService;

  @GetMapping("/session")
  public ResponseEntity<ApiResponse<TableSessionResponse>> getSession(
      @RequestHeader(value = "X-Table-Session-Token", required = false) String token) {
    TableSessionResponse response = tableSessionService.getGuestSession(token);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(response));
  }
}
