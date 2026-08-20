package com.jaconis.bankflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Perfil do usuário autenticado")
public record MeResponse(
        @Schema(example = "11111111-1111-1111-1111-111111111111")
        UUID id,

        @Schema(example = "user@example.com")
        String email,

        @Schema(example = "USER")
        String role,

        @Schema(example = "2026-01-15T12:00:00Z")
        Instant createdAt
) {}
