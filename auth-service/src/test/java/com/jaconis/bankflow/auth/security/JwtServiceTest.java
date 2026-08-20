package com.jaconis.bankflow.auth.security;

import com.jaconis.bankflow.auth.entity.User;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final long EXPIRATION_MS = 900_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
    }

    @Test
    void generateToken_containsSubjectEmailAndRole() {
        User user = userWithId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        String token = jwtService.generateToken(user);

        assertEquals(user.getId().toString(), jwtService.extractSubject(token));
        assertEquals("a@b.com", jwtService.extractEmail(token));
        assertEquals("USER", jwtService.extractRole(token));
        assertTrue(jwtService.isValid(token));
    }

    @Test
    void isValid_returnsFalseForMalformedToken() {
        assertFalse(jwtService.isValid("not-a-jwt"));
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        User user = userWithId(UUID.randomUUID());
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertFalse(jwtService.isValid(tampered));
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1_000L);
        User user = userWithId(UUID.randomUUID());

        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isValid(token));
    }

    @Test
    void isValid_returnsFalseForTokenSignedWithDifferentSecret() {
        User user = userWithId(UUID.randomUUID());
        String token = jwtService.generateToken(user);

        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret", "fedcba9876543210fedcba9876543210");
        ReflectionTestUtils.setField(otherService, "expirationMs", EXPIRATION_MS);

        assertFalse(otherService.isValid(token));
    }

    private static @NonNull User userWithId(UUID id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail("a@b.com");
        user.setPassword("hash");
        user.setRole("USER");
        return user;
    }
}
