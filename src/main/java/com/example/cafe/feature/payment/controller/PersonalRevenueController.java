package com.example.cafe.feature.payment.controller;

import com.example.cafe.common.response.ApiResponse;
import com.example.cafe.feature.payment.dto.response.PersonalRevenueResponse;
import com.example.cafe.feature.payment.service.PersonalRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/revenue")
@RequiredArgsConstructor
public class PersonalRevenueController {
  private final PersonalRevenueService service;

  @GetMapping("/today")
  public ResponseEntity<ApiResponse<PersonalRevenueResponse>> today(Authentication authentication,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(ApiResponse.ok(service.today(authentication.getName(), page, size)));
  }
}
