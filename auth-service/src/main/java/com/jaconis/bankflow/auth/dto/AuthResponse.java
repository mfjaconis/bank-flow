package com.jaconis.bankflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de registro ou login")
public record AuthResponse(
        @Schema(example = "Login realizado com sucesso!")
        String message,

        @Schema(example = "user@example.com")
        String email,

        @Schema(description = "JWT (apenas no login)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token
) {}
