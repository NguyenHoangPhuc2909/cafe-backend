package com.example.cafe.feature.cash.dto.response;

import java.math.BigDecimal;
import java.time.*;

public record CashReconciliationResponse(Long id, LocalDate businessDate, Long performedById,
    String performedByName, Instant createdAt, BigDecimal openingAmount, BigDecimal cashSales,
    long cashPaymentCount, long lastPaymentId, long revision, BigDecimal topUps, BigDecimal payouts,
    BigDecimal expectedAmount, BigDecimal actualAmount, BigDecimal difference, String note) {}
