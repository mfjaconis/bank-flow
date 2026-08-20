package com.jaconis.bankflow.gateway.security;

import com.jaconis.bankflow.gateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET));
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void isValid_returnsTrueForSignedToken() {
        String token = token("11111111-1111-1111-1111-111111111111", "USER", Instant.now().plusSeconds(600));

        assertTrue(jwtService.isValid(token));
        assertEquals("11111111-1111-1111-1111-111111111111", jwtService.extractSubject(token));
        assertEquals("USER", jwtService.extractRole(token));
    }

    @Test
    void isValid_returnsFalseForMalformedToken() {
        assertFalse(jwtService.isValid("not-a-jwt"));
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        String token = token(UUID.randomUUID().toString(), "USER", Instant.now().plusSeconds(600));
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertFalse(jwtService.isValid(tampered));
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        String token = token(UUID.randomUUID().toString(), "USER", Instant.now().minusSeconds(60));

        assertFalse(jwtService.isValid(token));
    }

    @Test
    void isValid_returnsFalseForTokenSignedWithDifferentSecret() {
        String token = token(UUID.randomUUID().toString(), "ADMIN", Instant.now().plusSeconds(600));
        JwtService other = new JwtService(new JwtProperties("fedcba9876543210fedcba9876543210"));

        assertFalse(other.isValid(token));
    }

    private String token(String subject, String role, Instant expiration) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }
}
