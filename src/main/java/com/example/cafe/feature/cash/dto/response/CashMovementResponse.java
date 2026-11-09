package com.example.cafe.feature.cash.dto.response;

import com.example.cafe.common.enums.CashMovementType;
import java.math.BigDecimal;
import java.time.*;

public record CashMovementResponse(Long id, LocalDate businessDate, CashMovementType type,
    BigDecimal amount, String note, Long performedById, String performedByName, Instant createdAt) {}
