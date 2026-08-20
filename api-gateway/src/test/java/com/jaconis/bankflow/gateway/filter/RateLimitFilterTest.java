package com.jaconis.bankflow.gateway.filter;

import com.jaconis.bankflow.gateway.config.RateLimitProperties;
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
        RateLimitFilter filter = filter();
        when(proxyManager.getProxy(eq("gateway-rate-limit:10.0.0.1"), any())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        MockHttpServletRequest request = request("/accounts", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void returns429_whenLimitExceeded() throws Exception {
        RateLimitFilter filter = filter();
        when(proxyManager.getProxy(eq("gateway-rate-limit:10.0.0.1"), any())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        MockHttpServletRequest request = request("/transfers", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader(HttpHeaders.RETRY_AFTER));
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":429"));
        assertTrue(body.contains("\"path\":\"/transfers\""));
        assertTrue(body.contains("Muitas tentativas. Tente novamente em instantes."));
    }

    @Test
    void skipsFilter_forHealth() throws Exception {
        RateLimitFilter filter = filter();
        MockHttpServletRequest request = request("/actuator/health", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(proxyManager, never()).getProxy(any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsFilter_forSwagger() throws Exception {
        RateLimitFilter filter = filter();
        MockHttpServletRequest request = request("/v3/api-docs", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(proxyManager, never()).getProxy(any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usesFirstForwardedIp_inBucketKey() throws Exception {
        RateLimitFilter filter = filter();
        when(proxyManager.getProxy(eq("gateway-rate-limit:203.0.113.10"), any())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        MockHttpServletRequest request = request("/auth/me", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.1.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(proxyManager).getProxy(eq("gateway-rate-limit:203.0.113.10"), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void retryAfter_isAtLeastOneSecond() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                proxyManager,
                new RateLimitProperties(true, 5, Duration.ofMillis(500))
        );
        when(proxyManager.getProxy(any(), any())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/accounts", "10.0.0.1"), response, filterChain);

        assertEquals("1", response.getHeader(HttpHeaders.RETRY_AFTER));
    }

    private RateLimitFilter filter() {
        return new RateLimitFilter(
                proxyManager,
                new RateLimitProperties(true, 100, Duration.ofMinutes(1))
        );
    }

    private static MockHttpServletRequest request(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
