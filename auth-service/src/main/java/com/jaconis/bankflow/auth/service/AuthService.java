package com.jaconis.bankflow.auth.service;

import com.jaconis.bankflow.auth.dto.AuthResponse;
import com.jaconis.bankflow.auth.dto.RegisterRequest;
import com.jaconis.bankflow.auth.entity.User;
import com.jaconis.bankflow.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("Email already registered");
        });


        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");

        userRepository.save(user);

        return new AuthResponse("User registered", user.getEmail());
    }
}