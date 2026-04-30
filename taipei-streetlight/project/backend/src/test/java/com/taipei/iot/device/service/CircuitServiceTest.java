package com.taipei.iot.device.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.device.dto.CircuitRequest;
import com.taipei.iot.device.dto.CircuitResponse;
import com.taipei.iot.device.entity.Circuit;
import com.taipei.iot.device.repository.CircuitRepository;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircuitServiceTest {

    @InjectMocks private CircuitService circuitService;
    @Mock private CircuitRepository circuitRepository;
    @Mock private DeviceRepository deviceRepository;

    private Circuit circuit;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("TENANT_A");
        circuit = Circuit.builder().id(1L).circuitNumber("CKT-N-A")
                .circuitName("北區A迴路").status("ACTIVE").build();
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void getById_found() {
        when(circuitRepository.findById(1L)).thenReturn(Optional.of(circuit));
        CircuitResponse res = circuitService.getById(1L);
        assertEquals("CKT-N-A", res.getCircuitNumber());
    }

    @Test
    void getById_notFound_throws() {
        when(circuitRepository.findById(99L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> circuitService.getById(99L));
        assertEquals(ErrorCode.CIRCUIT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_duplicateNumber_throws() {
        CircuitRequest req = new CircuitRequest();
        req.setCircuitNumber("CKT-N-A");
        when(circuitRepository.findByTenantIdAndCircuitNumber("TENANT_A", "CKT-N-A"))
                .thenReturn(Optional.of(circuit));

        BusinessException ex = assertThrows(BusinessException.class, () -> circuitService.create(req));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void create_success() {
        CircuitRequest req = new CircuitRequest();
        req.setCircuitNumber("CKT-NEW");
        req.setCircuitName("New Circuit");

        when(circuitRepository.findByTenantIdAndCircuitNumber(any(), any())).thenReturn(Optional.empty());
        when(circuitRepository.save(any(Circuit.class))).thenAnswer(inv -> {
            Circuit c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CircuitResponse res = circuitService.create(req);
        assertEquals("CKT-NEW", res.getCircuitNumber());
        assertEquals("ACTIVE", res.getStatus());
    }

    @Test
    void delete_withDevices_throwsCIRCUIT_HAS_DEVICES() {
        when(circuitRepository.findById(1L)).thenReturn(Optional.of(circuit));
        when(deviceRepository.countByCircuitId(1L)).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class, () -> circuitService.delete(1L));
        assertEquals(ErrorCode.CIRCUIT_HAS_DEVICES, ex.getErrorCode());
    }

    @Test
    void delete_noDevices_succeeds() {
        when(circuitRepository.findById(1L)).thenReturn(Optional.of(circuit));
        when(deviceRepository.countByCircuitId(1L)).thenReturn(0L);

        circuitService.delete(1L);
        verify(circuitRepository).delete(circuit);
    }

    @Test
    void update_success() {
        CircuitRequest req = new CircuitRequest();
        req.setCircuitNumber("CKT-UPD");
        req.setCircuitName("Updated");

        when(circuitRepository.findById(1L)).thenReturn(Optional.of(circuit));
        when(circuitRepository.save(any())).thenReturn(circuit);

        CircuitResponse res = circuitService.update(1L, req);
        assertEquals("CKT-UPD", circuit.getCircuitNumber());
    }
}
