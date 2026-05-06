package com.taipei.iot.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantEntityListenerTest {

    private final TenantEntityListener listener = new TenantEntityListener();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void prePersist_shouldSetTenantId_whenNull() {
        TenantContext.setCurrentTenantId("T1");

        TestTenantAwareEntity entity = new TestTenantAwareEntity();
        listener.prePersist(entity);

        assertEquals("T1", entity.getTenantId());
    }

    @Test
    void prePersist_shouldNotOverride_whenAlreadySet() {
        TenantContext.setCurrentTenantId("T1");

        TestTenantAwareEntity entity = new TestTenantAwareEntity();
        entity.setTenantId("T2");
        listener.prePersist(entity);

        assertEquals("T2", entity.getTenantId());
    }
}
