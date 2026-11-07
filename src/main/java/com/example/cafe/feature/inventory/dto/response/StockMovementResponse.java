package com.example.cafe.feature.inventory.dto.response;

import com.example.cafe.common.enums.StockMovementType;
import java.time.Instant;

public record StockMovementResponse(
    Long id,
    StockMovementType type,
    Long ingredientId,
    Long productId,
    Double quantityDelta,
    Double availableBefore,
    Double availableAfter,
    Long performedById,
    String note,
    Instant createdAt
) {}