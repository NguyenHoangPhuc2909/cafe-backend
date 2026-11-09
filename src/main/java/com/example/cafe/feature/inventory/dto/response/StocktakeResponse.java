package com.example.cafe.feature.inventory.dto.response;

import com.example.cafe.common.entity.inventory.StocktakeItem;
import java.time.Instant;
import java.util.List;

public record StocktakeResponse(Long id, Long performedById, String performedByName,
    Instant createdAt, String note, int totalItems, long checkedItems,
    long differenceItems, List<StocktakeItem> items) {}
