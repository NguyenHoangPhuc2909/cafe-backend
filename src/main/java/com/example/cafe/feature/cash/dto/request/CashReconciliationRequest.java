package com.example.cafe.feature.cash.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashReconciliationRequest(@NotNull UUID requestId, @NotNull LocalDate businessDate,
    @NotNull @PositiveOrZero Long expectedRevision,
    @NotNull @PositiveOrZero Long expectedPaymentCount,
    @NotNull @PositiveOrZero Long expectedLastPaymentId,
    @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 0) BigDecimal actualAmount,
    @NotNull @Size(max = 500) String note) {}
