package com.jaconis.bankflow.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta de registro ou login")
public record AuthResponse(
        @Schema(example = "Login realizado com sucesso!")
        String message,

        @Schema(example = "user@example.com")
        String email,

        @Schema(description = "JWT (apenas no login)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Conta criada no registro", example = "11111111-1111-1111-1111-111111111111")
        UUID accountId
) {}
