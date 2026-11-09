package com.example.cafe.feature.payment.service;

import com.example.cafe.feature.payment.dto.response.PersonalRevenueResponse;

public interface PersonalRevenueService {
  PersonalRevenueResponse today(String username, int page, int size);
}
