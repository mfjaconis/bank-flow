package com.jaconis.bankflow.auth.security;

import com.jaconis.bankflow.auth.config.RateLimitProperties;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    ProxyManager<String> proxyManager;

    @Mock
    BucketProxy bucket;

    @Mock
    FilterChain filterChain;

    @Test
    void allowsRequest_whenTokenConsumed() throws Exception {
        RateLimitFilter filter = filterFor(List.of("/auth/login", "/auth/register"));
        when(proxyManager.getProxy(eq("auth-rate-limit:/auth/login:10.0.0.1"), any()))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        MockHttpServletRequest request = request("/auth/login", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void returns429_whenLimitExceeded() throws Exception {
        RateLimitFilter filter = filterFor(List.of("/auth/login"));
        when(proxyManager.getProxy(eq("auth-rate-limit:/auth/login:10.0.0.1"), any()))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        MockHttpServletRequest request = request("/auth/login", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader(HttpHeaders.RETRY_AFTER));
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":429"));
        assertTrue(body.contains("\"path\":\"/auth/login\""));
        assertTrue(body.contains("Muitas tentativas. Tente novamente em instantes."));
    }

    @Test
    void skipsFilter_whenPathIsNotConfigured() throws Exception {
        RateLimitFilter filter = filterFor(List.of("/auth/login"));

        MockHttpServletRequest request = request("/auth/register", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(proxyManager, never()).getProxy(any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsFilter_whenPathsAreEmpty() throws Exception {
        RateLimitFilter filter = filterFor(List.of());

        MockHttpServletRequest request = request("/auth/login", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(proxyManager, never()).getProxy(any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usesFirstForwardedIp_inBucketKey() throws Exception {
        RateLimitFilter filter = filterFor(List.of("/auth/register"));
        when(proxyManager.getProxy(eq("auth-rate-limit:/auth/register:203.0.113.10"), any()))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        MockHttpServletRequest request = request("/auth/register", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.1.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(proxyManager).getProxy(eq("auth-rate-limit:/auth/register:203.0.113.10"), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usesRemoteAddr_whenForwardedHeaderIsBlank() throws Exception {
        RateLimitFilter filter = filterFor(List.of("/auth/login"));
        when(proxyManager.getProxy(eq("auth-rate-limit:/auth/login:192.168.0.20"), any()))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        MockHttpServletRequest request = request("/auth/login", "192.168.0.20");
        request.addHeader("X-Forwarded-For", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(proxyManager).getProxy(eq("auth-rate-limit:/auth/login:192.168.0.20"), any());
    }

    @Test
    void retryAfter_isAtLeastOneSecond() throws Exception {
        RateLimitProperties properties = new RateLimitProperties(
                true,
                5,
                Duration.ofMillis(500),
                List.of("/auth/login")
        );
        RateLimitFilter filter = new RateLimitFilter(proxyManager, properties);
        when(proxyManager.getProxy(any(), any())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/auth/login", "10.0.0.1"), response, filterChain);

        assertEquals("1", response.getHeader(HttpHeaders.RETRY_AFTER));
    }

    private RateLimitFilter filterFor(List<String> paths) {
        return new RateLimitFilter(
                proxyManager,
                new RateLimitProperties(true, 10, Duration.ofMinutes(1), paths)
        );
    }

    private static MockHttpServletRequest request(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
