package com.taipei.iot.fault.service;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.ConnectivityType;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.fault.entity.FaultCorrelation;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.enums.FaultTicketStatus;
import com.taipei.iot.fault.enums.RootCauseType;
import com.taipei.iot.fault.repository.FaultCorrelationRepository;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaultCorrelationServiceTest {

    @InjectMocks private FaultCorrelationService faultCorrelationService;
    @Mock private FaultTicketRepository faultTicketRepository;
    @Mock private FaultCorrelationRepository faultCorrelationRepository;
    @Mock private DeviceRepository deviceRepository;

    // ── Circuit dimension ──

    @Test
    void detectOnNewTicket_circuitThresholdMet_createsCorrelation() {
        FaultTicket ticket = FaultTicket.builder().id(1L)
                .circuitId(5L).deviceId(10L).status(FaultTicketStatus.OPEN).build();

        when(faultTicketRepository.countRecentByCircuit(eq(5L), any()))
                .thenReturn(3L); // Meets threshold (≥3)
        when(faultCorrelationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // deviceId check (dimension 2) — non-GATEWAY connectivity
        Device device = Device.builder().id(10L).deviceType(DeviceType.LUMINAIRE).build();
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));

        faultCorrelationService.detectOnNewTicket(ticket);

        ArgumentCaptor<FaultCorrelation> captor = ArgumentCaptor.forClass(FaultCorrelation.class);
        verify(faultCorrelationRepository).save(captor.capture());

        FaultCorrelation saved = captor.getValue();
        assertEquals(RootCauseType.CIRCUIT, saved.getRootCauseType());
        assertEquals(5L, saved.getRootCauseId());
        assertEquals("DETECTED", saved.getStatus());
    }

    @Test
    void detectOnNewTicket_circuitBelowThreshold_noCorrelation() {
        FaultTicket ticket = FaultTicket.builder().id(1L)
                .circuitId(5L).deviceId(10L).status(FaultTicketStatus.OPEN).build();

        when(faultTicketRepository.countRecentByCircuit(eq(5L), any()))
                .thenReturn(1L); // Below threshold
        // Gateway dimension — non-GATEWAY connectivity
        Device device = Device.builder().id(10L).deviceType(DeviceType.LUMINAIRE).build();
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));

        faultCorrelationService.detectOnNewTicket(ticket);

        verify(faultCorrelationRepository, never()).save(any());
    }

    @Test
    void detectOnNewTicket_nullCircuitId_skipsCircuitDimension() {
        FaultTicket ticket = FaultTicket.builder().id(1L)
                .circuitId(null).deviceId(10L).status(FaultTicketStatus.OPEN).build();

        Device device = Device.builder().id(10L).deviceType(DeviceType.POLE).build();
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));

        faultCorrelationService.detectOnNewTicket(ticket);

        verify(faultTicketRepository, never()).countRecentByCircuit(any(), any());
    }

    @Test
    void detectOnNewTicket_nullDeviceId_skipsGatewayDimension() {
        FaultTicket ticket = FaultTicket.builder().id(1L)
                .circuitId(5L).deviceId(null).status(FaultTicketStatus.OPEN).build();

        when(faultTicketRepository.countRecentByCircuit(eq(5L), any())).thenReturn(0L);

        faultCorrelationService.detectOnNewTicket(ticket);

        verify(deviceRepository, never()).findById(any());
    }

    @Test
    void detectOnNewTicket_gatewayConnectivity_logsDebug() {
        FaultTicket ticket = FaultTicket.builder().id(1L)
                .circuitId(null).deviceId(20L).status(FaultTicketStatus.OPEN).build();

        Device gateway = Device.builder().id(20L).deviceType(DeviceType.CONTROLLER)
                .parentDeviceId(10L).connectivityType(ConnectivityType.GATEWAY).build();
        when(deviceRepository.findById(20L)).thenReturn(Optional.of(gateway));

        // Should not throw; gateway dimension just logs
        assertDoesNotThrow(() -> faultCorrelationService.detectOnNewTicket(ticket));
        verify(deviceRepository).findById(20L);
    }
}
