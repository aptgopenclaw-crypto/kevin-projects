package com.taipei.iot.smartiot.scheduler;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.entity.AlertHistory;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.AlertStatus;
import com.taipei.iot.smartiot.repository.AlertHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PowerOutageDetectorTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private AlertHistoryRepository alertHistoryRepository;
    @InjectMocks private PowerOutageDetector detector;

    private Device buildMeter(Long id, Long circuitId, LocalDateTime lastTelemetry) {
        return Device.builder()
                .id(id).tenantId("T1").deviceType(DeviceType.POWER_EQUIPMENT)
                .circuitId(circuitId).lastTelemetryAt(lastTelemetry)
                .status(DeviceStatus.ACTIVE).build();
    }

    private Device buildLight(Long id, Long circuitId, LocalDateTime lastHeartbeat) {
        return Device.builder()
                .id(id).tenantId("T1").deviceType(DeviceType.LUMINAIRE)
                .circuitId(circuitId).deviceToken("tok")
                .lastHeartbeatAt(lastHeartbeat).status(DeviceStatus.ACTIVE).build();
    }

    // TC-1: 電表離線 + 多燈離線 → POWER_OUTAGE (CRITICAL)
    @Test
    void detect_meterOffline_multiLightsOffline_createsCriticalAlert() {
        Device meter = buildMeter(1L, 100L, LocalDateTime.now().minusHours(1));
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<Device> iotDevices = List.of(
                buildLight(10L, 100L, null),
                buildLight(11L, 100L, null));
        List<Device> offlineDevices = List.of(
                buildLight(10L, 100L, null),
                buildLight(11L, 100L, null));

        when(deviceRepository.findIoTDevicesInCircuit(100L)).thenReturn(iotDevices);
        when(deviceRepository.findOfflineDevicesInCircuit(eq(100L), any())).thenReturn(offlineDevices);
        when(alertHistoryRepository.findByDeviceIdOrderByTriggeredAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        detector.checkCircuit(meter, cutoff);

        ArgumentCaptor<AlertHistory> captor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(captor.getValue().getMessage()).contains("區域停電");
    }

    // TC-2: 電表在線 + 多燈離線 → CIRCUIT_TRIP (WARNING)
    @Test
    void detect_meterOnline_multiLightsOffline_createsWarningAlert() {
        Device meter = buildMeter(2L, 200L, LocalDateTime.now().minusMinutes(1)); // 在線
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<Device> iotDevices = List.of(
                buildLight(20L, 200L, null),
                buildLight(21L, 200L, null));
        List<Device> offlineDevices = List.of(
                buildLight(20L, 200L, null),
                buildLight(21L, 200L, null));

        when(deviceRepository.findIoTDevicesInCircuit(200L)).thenReturn(iotDevices);
        when(deviceRepository.findOfflineDevicesInCircuit(eq(200L), any())).thenReturn(offlineDevices);
        when(alertHistoryRepository.findByDeviceIdOrderByTriggeredAtDesc(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        detector.checkCircuit(meter, cutoff);

        ArgumentCaptor<AlertHistory> captor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(captor.getValue().getMessage()).contains("回路跳脫");
    }

    // TC-3: 離線比例不足 → 不建告警
    @Test
    void detect_lowOfflineRatio_noAlert() {
        Device meter = buildMeter(3L, 300L, LocalDateTime.now().minusHours(1));
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<Device> iotDevices = List.of(
                buildLight(30L, 300L, LocalDateTime.now()),
                buildLight(31L, 300L, LocalDateTime.now()),
                buildLight(32L, 300L, null));
        // 只有 1/3 離線 < 50%
        List<Device> offlineDevices = List.of(buildLight(32L, 300L, null));

        when(deviceRepository.findIoTDevicesInCircuit(300L)).thenReturn(iotDevices);
        when(deviceRepository.findOfflineDevicesInCircuit(eq(300L), any())).thenReturn(offlineDevices);

        detector.checkCircuit(meter, cutoff);

        verify(alertHistoryRepository, never()).save(any());
    }

    // TC-4: 已有 OPEN 告警 → 抑制不重複建立
    @Test
    void detect_existingOpenAlert_suppressed() {
        Device meter = buildMeter(4L, 400L, LocalDateTime.now().minusHours(1));
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<Device> iotDevices = List.of(buildLight(40L, 400L, null));
        List<Device> offlineDevices = List.of(buildLight(40L, 400L, null));

        when(deviceRepository.findIoTDevicesInCircuit(400L)).thenReturn(iotDevices);
        when(deviceRepository.findOfflineDevicesInCircuit(eq(400L), any())).thenReturn(offlineDevices);

        AlertHistory existing = AlertHistory.builder()
                .status(AlertStatus.OPEN).severity(AlertSeverity.CRITICAL).build();
        when(alertHistoryRepository.findByDeviceIdOrderByTriggeredAtDesc(eq(4L), any()))
                .thenReturn(new PageImpl<>(List.of(existing)));

        detector.checkCircuit(meter, cutoff);

        verify(alertHistoryRepository, never()).save(any());
    }
}
