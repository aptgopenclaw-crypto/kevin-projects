package com.taipei.iot.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.common.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HandlerMethod handlerMethod;

    @InjectMocks
    private RateLimitInterceptor interceptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(redisTemplate, objectMapper);
    }

    // ─── Helper: stub @RateLimit annotation on handler ───────────────────────

    private RateLimit mockRateLimit(String key, int limit, int period) {
        RateLimit rateLimit = mock(RateLimit.class);
        when(rateLimit.key()).thenReturn(key);
        lenient().when(rateLimit.limit()).thenReturn(limit);
        lenient().when(rateLimit.period()).thenReturn(period);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        return rateLimit;
    }

    @SuppressWarnings("unchecked")
    private void mockLuaScript(Long returnValue) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(String.class)))
                .thenReturn(returnValue);
    }

    @SuppressWarnings("unchecked")
    private void mockLuaScriptThrows(Exception ex) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(String.class)))
                .thenThrow(ex);
    }

    // ─── 1. No annotation → pass through ────────────────────────────────────

    @Test
    void preHandle_noAnnotation_shouldPassThrough() throws Exception {
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verifyNoInteractions(redisTemplate);
    }

    // ─── 2. Not HandlerMethod → pass through ────────────────────────────────

    @Test
    void preHandle_notHandlerMethod_shouldPassThrough() throws Exception {
        Object staticHandler = new Object();

        boolean result = interceptor.preHandle(request, response, staticHandler);

        assertTrue(result);
        verifyNoInteractions(redisTemplate);
    }

    // ─── 3. Under limit → allow ─────────────────────────────────────────────

    @Test
    void preHandle_underLimit_shouldAllow() throws Exception {
        mockRateLimit("login", 10, 60);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        mockLuaScript(5L);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verify(response, never()).setStatus(anyInt());
    }

    // ─── 4. First request → Lua script handles EXPIRE atomically ────────────

    @Test
    @SuppressWarnings("unchecked")
    void preHandle_firstRequest_luaScriptCalledWithCorrectArgs() throws Exception {
        mockRateLimit("captcha", 20, 60);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        mockLuaScript(1L);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("rate_limit:captcha:10.0.0.1")),
                eq("60"));
    }

    // ─── 5. Subsequent request → still allowed ──────────────────────────────

    @Test
    void preHandle_subsequentRequest_shouldAllow() throws Exception {
        mockRateLimit("login", 10, 60);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        mockLuaScript(3L);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    // ─── 6. Exceed limit → 429 ─────────────────────────────────────────────

    @Test
    void preHandle_exceedLimit_shouldReturn429() throws Exception {
        mockRateLimit("login", 10, 60);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        mockLuaScript(11L);
        when(redisTemplate.getExpire("rate_limit:login:192.168.1.100", TimeUnit.SECONDS))
                .thenReturn(45L);

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "45");
        verify(response).setContentType("application/json");
        assertTrue(body.toString().contains("10030"));   // ErrorCode.RATE_LIMIT_EXCEEDED
    }

    // ─── 7. At exact limit → still allowed ─────────────────────────────────

    @Test
    void preHandle_atExactLimit_shouldStillAllow() throws Exception {
        mockRateLimit("login", 10, 60);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        mockLuaScript(10L);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    // ─── 8. Redis failure → fail-open ───────────────────────────────────────

    @Test
    void preHandle_redisUnavailable_shouldFailOpen() throws Exception {
        mockRateLimit("login", 10, 60);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        mockLuaScriptThrows(new RedisConnectionFailureException("Connection refused"));

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verify(response, never()).setStatus(anyInt());
    }

    // ─── 9. Script returns null → pass through ──────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void preHandle_scriptReturnsNull_shouldPassThrough() throws Exception {
        mockRateLimit("login", 10, 60);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(String.class)))
                .thenReturn(null);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    // ─── 10. Uses remoteAddr (not X-Forwarded-For) ──────────────────────────

    @Test
    void preHandle_shouldUseRemoteAddr_notForwardedHeader() throws Exception {
        mockRateLimit("login", 10, 60);
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");
        mockLuaScript(1L);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verify(request).getRemoteAddr();
        verify(request, never()).getHeader(anyString());
    }

    // ─── 11. Retry-After with null TTL ──────────────────────────────────────

    @Test
    void preHandle_exceedLimit_nullTtl_shouldNotSetRetryAfter() throws Exception {
        mockRateLimit("captcha", 20, 60);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        mockLuaScript(21L);
        when(redisTemplate.getExpire("rate_limit:captcha:10.0.0.1", TimeUnit.SECONDS))
                .thenReturn(null);

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(429);
        verify(response, never()).setHeader(eq("Retry-After"), anyString());
    }

    // ─── 12. Different keys produce different Redis keys ────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void preHandle_differentKeys_shouldUseDifferentRedisKeys() throws Exception {
        mockRateLimit("forgot-pwd", 5, 300);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        mockLuaScript(1L);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("rate_limit:forgot-pwd:10.0.0.1")),
                eq("300"));
    }
}
