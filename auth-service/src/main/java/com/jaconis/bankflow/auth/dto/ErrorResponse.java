package com.jaconis.bankflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Erro padrão da API")
public record ErrorResponse(
        Instant timestamp,

        @Schema(example = "401")
        int status,

        @Schema(example = "Não autenticado")
        String message,

        @Schema(example = "/auth/me")
        String path
) {}
