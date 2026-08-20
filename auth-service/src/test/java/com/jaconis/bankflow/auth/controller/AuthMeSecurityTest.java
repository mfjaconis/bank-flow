package com.jaconis.bankflow.auth.controller;

import com.jaconis.bankflow.auth.config.SecurityConfig;
import com.jaconis.bankflow.auth.dto.MeResponse;
import com.jaconis.bankflow.auth.exception.ApiExceptionHandler;
import com.jaconis.bankflow.auth.security.JwtAuthenticationEntryPoint;
import com.jaconis.bankflow.auth.security.JwtAuthenticationFilter;
import com.jaconis.bankflow.auth.security.JwtService;
import com.jaconis.bankflow.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class
})
@AutoConfigureMockMvc
class AuthMeSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtService jwtService;

    @Test
    void me_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Não autenticado"))
                .andExpect(jsonPath("$.path").value("/auth/me"));
    }

    @Test
    void me_withValidJwt_returns200() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant createdAt = Instant.parse("2026-01-15T12:00:00Z");
        String token = "valid.jwt.token";

        when(jwtService.isValid(token)).thenReturn(true);
        when(jwtService.extractSubject(token)).thenReturn(userId.toString());
        when(jwtService.extractRole(token)).thenReturn("USER");
        when(authService.me(userId.toString()))
                .thenReturn(new MeResponse(userId, "a@b.com", "USER", createdAt));

        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("a@b.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void me_withInvalidJwt_returns401() throws Exception {
        String token = "invalid.jwt.token";
        when(jwtService.isValid(token)).thenReturn(false);

        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Não autenticado"))
                .andExpect(jsonPath("$.path").value("/auth/me"));
    }

    @Test
    void me_withBlankBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Não autenticado"))
                .andExpect(jsonPath("$.path").value("/auth/me"));
    }

    @Test
    void me_withNonBearerAuthorization_returns401() throws Exception {
        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Não autenticado"))
                .andExpect(jsonPath("$.path").value("/auth/me"));
    }
}
