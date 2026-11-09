package com.example.cafe.feature.cash.dto.response;

import com.example.cafe.common.response.PageResponse;
import java.math.BigDecimal;
import java.time.*;

public record CashDayResponse(LocalDate businessDate, String timeZone, Instant updatedAt,
    BigDecimal openingAmount, BigDecimal cashSales, long cashPaymentCount, long lastPaymentId,
    long revision, BigDecimal topUps, BigDecimal payouts, BigDecimal expectedAmount,
    PageResponse<CashMovementResponse> movements) {}
