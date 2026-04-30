package com.taipei.iot.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TenantInterceptorTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void singleMode_shouldSetDefaultTenant() throws Exception {
        TenantProperties props = new TenantProperties();
        props.setMode("single");
        props.setDefaultId("DEFAULT");

        TenantInterceptor interceptor = new TenantInterceptor(props);
        interceptor.preHandle(
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class),
                new Object()
        );

        assertEquals("DEFAULT", TenantContext.getCurrentTenantId());
    }

    @Test
    void multiMode_shouldNotSetTenant() throws Exception {
        TenantProperties props = new TenantProperties();
        props.setMode("multi");

        TenantInterceptor interceptor = new TenantInterceptor(props);
        interceptor.preHandle(
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class),
                new Object()
        );

        assertNull(TenantContext.getCurrentTenantId());
    }

    @Test
    void afterCompletion_shouldClearContext() throws Exception {
        TenantProperties props = new TenantProperties();
        TenantContext.setCurrentTenantId("T1");

        TenantInterceptor interceptor = new TenantInterceptor(props);
        interceptor.afterCompletion(
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class),
                new Object(),
                null
        );

        assertNull(TenantContext.getCurrentTenantId());
    }
}
