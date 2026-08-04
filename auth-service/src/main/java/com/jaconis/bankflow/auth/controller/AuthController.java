package com.jaconis.bankflow.auth.controller;

import com.jaconis.bankflow.auth.dto.AuthResponse;
import com.jaconis.bankflow.auth.dto.RegisterRequest;
import com.jaconis.bankflow.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}