package com.example.cafe.feature.cash.dto.request;

import com.example.cafe.common.enums.CashMovementType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashMovementRequest(@NotNull UUID requestId, @NotNull LocalDate businessDate,
    @NotNull CashMovementType type, @NotNull @Positive @Digits(integer = 12, fraction = 0) BigDecimal amount,
    @NotBlank @Size(max = 500) String note) {}
