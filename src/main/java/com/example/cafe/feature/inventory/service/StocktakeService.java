package com.example.cafe.feature.inventory.service;

import com.example.cafe.common.entity.inventory.StocktakeItem;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.inventory.dto.request.StocktakeRequest;
import com.example.cafe.feature.inventory.dto.response.StocktakeResponse;
import java.util.List;

public interface StocktakeService {
  List<StocktakeItem> preview();
  StocktakeResponse save(StocktakeRequest request, String username);
  PageResponse<StocktakeResponse> history(int page, int size);
  StocktakeResponse detail(Long id);
}
