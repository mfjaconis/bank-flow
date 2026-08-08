package com.jaconis.bankflow.auth.dto;

public record AuthResponse(
        String message,
        String email,
        String token
) {}