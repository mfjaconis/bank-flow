package com.jaconis.bankflow.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Saldo da conta")
public record BalanceResponse(
        @Schema(example = "11111111-1111-1111-1111-111111111111")
        UUID accountId,

        @Schema(example = "0.00")
        BigDecimal balance,

        @Schema(example = "BRL")
        String currency
) {}
