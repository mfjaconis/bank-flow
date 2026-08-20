package com.jaconis.bankflow.gateway.filter;

import com.jaconis.bankflow.gateway.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    JwtService jwtService;

    @Mock
    FilterChain filterChain;

    @Test
    void skipsFilter_forPublicLogin() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(jwtService, never()).isValid(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsFilter_forSwaggerUi() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(jwtService, never()).isValid(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void returns401_whenTokenMissing() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":401"));
        assertTrue(body.contains("Token ausente"));
        assertTrue(body.contains("\"path\":\"/auth/me\""));
    }

    @Test
    void returns401_whenTokenInvalid() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        when(jwtService.isValid("bad.token")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/accounts");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token inválido ou expirado"));
    }

    @Test
    void continues_andAddsUserHeaders_whenTokenValid() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        when(jwtService.isValid("good.token")).thenReturn(true);
        when(jwtService.extractSubject("good.token")).thenReturn("user-1");
        when(jwtService.extractRole("good.token")).thenReturn("USER");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), any());
        HttpServletRequest forwarded = requestCaptor.getValue();
        assertEquals("user-1", forwarded.getHeader("X-User-Id"));
        assertEquals("USER", forwarded.getHeader("X-User-Role"));
        assertEquals("Bearer good.token", forwarded.getHeader(HttpHeaders.AUTHORIZATION));
    }
}
