package com.jaconis.bankflow.account.dto;

import com.jaconis.bankflow.account.entity.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Conta bancária")
public record AccountResponse(
        @Schema(example = "11111111-1111-1111-1111-111111111111")
        UUID id,

        @Schema(example = "22222222-2222-2222-2222-222222222222")
        UUID userId,

        @Schema(example = "ACTIVE")
        AccountStatus status,

        @Schema(example = "BRL")
        String currency,

        Instant createdAt
) {}
