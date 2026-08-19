package com.jaconis.bankflow.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String role,
        Instant createdAt
) {}
