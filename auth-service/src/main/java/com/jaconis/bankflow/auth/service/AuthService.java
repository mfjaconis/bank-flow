package com.jaconis.bankflow.auth.service;

import com.jaconis.bankflow.auth.dto.AuthResponse;
import com.jaconis.bankflow.auth.dto.LoginRequest;
import com.jaconis.bankflow.auth.dto.MeResponse;
import com.jaconis.bankflow.auth.dto.RegisterRequest;
import com.jaconis.bankflow.auth.entity.User;
import com.jaconis.bankflow.auth.exception.EmailAlreadyRegisteredException;
import com.jaconis.bankflow.auth.exception.InvalidCredentialsException;
import com.jaconis.bankflow.auth.exception.UserNotFoundException;
import com.jaconis.bankflow.auth.repository.UserRepository;
import com.jaconis.bankflow.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new EmailAlreadyRegisteredException();
        });

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");

        userRepository.save(user);

        return new AuthResponse("User registered", user.getEmail(), null);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse("Login realizado com sucesso!", user.getEmail(), token);
    }

    public MeResponse me(String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new UserNotFoundException();
        }

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        return new MeResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
