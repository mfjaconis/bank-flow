package com.jaconis.bankflow.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Erro padrão da API")
public record ErrorResponse(
        Instant timestamp,

        @Schema(example = "404")
        int status,

        @Schema(example = "Conta não encontrada")
        String message,

        @Schema(example = "/accounts/11111111-1111-1111-1111-111111111111")
        String path
) {}
