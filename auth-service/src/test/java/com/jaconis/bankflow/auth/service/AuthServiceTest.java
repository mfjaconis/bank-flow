package com.jaconis.bankflow.auth.service;

import com.jaconis.bankflow.auth.client.AccountClient;
import com.jaconis.bankflow.auth.dto.AuthResponse;
import com.jaconis.bankflow.auth.dto.LoginRequest;
import com.jaconis.bankflow.auth.dto.MeResponse;
import com.jaconis.bankflow.auth.dto.RegisterRequest;
import com.jaconis.bankflow.auth.entity.User;
import com.jaconis.bankflow.auth.exception.AccountProvisioningException;
import com.jaconis.bankflow.auth.exception.EmailAlreadyRegisteredException;
import com.jaconis.bankflow.auth.exception.InvalidCredentialsException;
import com.jaconis.bankflow.auth.exception.UserNotFoundException;
import com.jaconis.bankflow.auth.repository.UserRepository;
import com.jaconis.bankflow.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;
    @Mock
    AccountClient accountClient;
    @InjectMocks
    AuthService authService;

    @Test
    void register_ok_savesUserAndCreatesAccount() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", userId);
            return user;
        });
        when(accountClient.createDefaultAccount(userId)).thenReturn(accountId);

        AuthResponse res = authService.register(new RegisterRequest("a@b.com", "123456"));

        assertEquals("User registered", res.message());
        assertEquals("a@b.com", res.email());
        assertNull(res.token());
        assertEquals(accountId, res.accountId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("a@b.com", saved.getEmail());
        assertEquals("encoded-hash", saved.getPassword());
        assertEquals("USER", saved.getRole());
        verify(accountClient).createDefaultAccount(userId);
    }

    @Test
    void register_whenAccountProvisioningFails_deletesUserAndThrows() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", userId);
            return user;
        });
        when(accountClient.createDefaultAccount(userId)).thenThrow(new AccountProvisioningException());

        assertThrows(AccountProvisioningException.class,
                () -> authService.register(new RegisterRequest("a@b.com", "123456")));

        verify(userRepository).delete(any(User.class));
    }

    @Test
    void register_whenEmailExists_throwsConflict() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> authService.register(new RegisterRequest("a@b.com", "123456")));

        verify(userRepository, never()).save(any());
        verify(accountClient, never()).createDefaultAccount(any());
    }

    @Test
    void login_ok_returnsToken() {
        User user = new User();
        user.setEmail("a@b.com");
        user.setPassword("hash");

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse res = authService.login(new LoginRequest("a@b.com", "123456"));

        assertEquals("Login realizado com sucesso!", res.message());
        assertEquals("a@b.com", res.email());
        assertEquals("jwt-token", res.token());
        assertNull(res.accountId());
    }

    @Test
    void login_whenEmailNotFound_throwsUnauthorized() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("a@b.com", "123456")));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_whenPasswordMismatch_throwsUnauthorized() {
        User user = new User();
        user.setEmail("a@b.com");
        user.setPassword("hash");

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("a@b.com", "123456")));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void me_ok_returnsUserInfo() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setEmail("a@b.com");
        user.setPassword("hash");
        user.setRole("USER");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MeResponse res = authService.me(userId.toString());

        assertEquals(userId, res.id());
        assertEquals("a@b.com", res.email());
        assertEquals("USER", res.role());
        assertEquals(user.getCreatedAt(), res.createdAt());
    }

    @Test
    void me_whenUserNotFound_throwsUnauthorized() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.me(userId.toString()));
    }

    @Test
    void me_whenUserIdInvalid_throwsUnauthorized() {
        assertThrows(UserNotFoundException.class, () -> authService.me("not-a-uuid"));
    }
}
