package com.taipei.iot.smartiot.scheduler;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.entity.AlertHistory;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.AlertStatus;
import com.taipei.iot.smartiot.repository.AlertHistoryRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 區域停電 / 回路跳脫偵測 (FN-07-020~021, D31)。
 * <p>
 * 每 120 秒掃描所有 POWER_EQUIPMENT 設備：
 * <ul>
 *   <li>電表離線 + 同回路多燈離線 → POWER_OUTAGE (CRITICAL)</li>
 *   <li>電表在線 + 同回路多燈離線 → CIRCUIT_TRIP (WARNING)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PowerOutageDetector {

    private static final int OFFLINE_THRESHOLD_MINUTES = 10;
    private static final double OFFLINE_RATIO_THRESHOLD = 0.5;

    private final DeviceRepository deviceRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    @Scheduled(fixedRate = 120000)
    @Transactional
    public void detect() {
        // 查詢所有電表設備 (跨租戶)
        List<Device> meters;
        try {
            TenantContext.setSystemContext();
            meters = deviceRepository.findAllByFilters(
                    DeviceType.POWER_EQUIPMENT, null, null, null);
        } finally {
            TenantContext.clear();
        }

        if (meters.isEmpty()) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(OFFLINE_THRESHOLD_MINUTES);

        for (Device meter : meters) {
            if (meter.getCircuitId() == null) {
                continue;
            }

            try {
                TenantContext.setCurrentTenantId(meter.getTenantId());
                checkCircuit(meter, cutoff);
            } catch (Exception e) {
                log.warn("[PowerOutage] Error checking meter {}: {}", meter.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    void checkCircuit(Device meter, LocalDateTime cutoff) {
        Long circuitId = meter.getCircuitId();

        // 同回路 IoT 路燈
        List<Device> iotDevices = deviceRepository.findIoTDevicesInCircuit(circuitId);
        if (iotDevices.isEmpty()) {
            return;
        }

        // 離線路燈
        List<Device> offlineDevices = deviceRepository.findOfflineDevicesInCircuit(circuitId, cutoff);
        double offlineRatio = (double) offlineDevices.size() / iotDevices.size();

        if (offlineRatio < OFFLINE_RATIO_THRESHOLD) {
            return; // 離線比例不夠高
        }

        boolean meterOffline = meter.getLastTelemetryAt() == null
                || meter.getLastTelemetryAt().isBefore(cutoff);

        if (meterOffline) {
            // FN-07-020: 電表異常 + 多燈離線 → 區域停電
            createAlert(meter, AlertSeverity.CRITICAL,
                    String.format("區域停電偵測: 回路 %d 電表離線 + %d/%d 路燈離線",
                            circuitId, offlineDevices.size(), iotDevices.size()));
        } else {
            // FN-07-021: 電表正常 + 多燈離線 → 回路跳脫
            createAlert(meter, AlertSeverity.WARNING,
                    String.format("回路跳脫偵測: 回路 %d 電表正常但 %d/%d 路燈離線",
                            circuitId, offlineDevices.size(), iotDevices.size()));
        }
    }

    private void createAlert(Device meter, AlertSeverity severity, String message) {
        // 抑制: 同設備 + 同嚴重度有 OPEN 告警就不重複建立
        boolean hasOpen = alertHistoryRepository
                .findByDeviceIdOrderByTriggeredAtDesc(meter.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().stream()
                .anyMatch(a -> a.getStatus() == AlertStatus.OPEN
                        && a.getSeverity() == severity);
        if (hasOpen) {
            return;
        }

        AlertHistory alert = AlertHistory.builder()
                .deviceId(meter.getId())
                .severity(severity)
                .status(AlertStatus.OPEN)
                .message(message)
                .triggeredAt(LocalDateTime.now())
                .notificationSent(false)
                .build();
        alertHistoryRepository.save(alert);

        log.info("[PowerOutage] Alert created — meter={}, severity={}, msg={}",
                meter.getId(), severity, message);
    }
}
