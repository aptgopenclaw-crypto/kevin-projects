package com.taipei.iot.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setAndGet_shouldReturnSameValue() {
        TenantContext.setCurrentTenantId("T1");
        assertEquals("T1", TenantContext.getCurrentTenantId());
    }

    @Test
    void clear_shouldRemoveValue() {
        TenantContext.setCurrentTenantId("T1");
        TenantContext.clear();
        assertNull(TenantContext.getCurrentTenantId());
    }

    @Test
    void setSystemContext_shouldReturnTrue() {
        TenantContext.setSystemContext();
        assertTrue(TenantContext.isSystemContext());
    }
}
