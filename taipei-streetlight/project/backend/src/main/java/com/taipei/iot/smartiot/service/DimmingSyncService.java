package com.taipei.iot.smartiot.service;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.entity.DimmingLog;
import com.taipei.iot.smartiot.enums.DimmingCommandType;
import com.taipei.iot.smartiot.enums.DimmingResult;
import com.taipei.iot.smartiot.event.TelemetryIngestedEvent;
import com.taipei.iot.smartiot.mqtt.MqttCommandPublisher;
import com.taipei.iot.smartiot.repository.DimmingLogRepository;
import com.taipei.iot.smartiot.repository.DimmingScheduleRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 調光同步服務 (FN-07-027, D28/D29)。
 * <p>
 * 設備恢復上線（收到 telemetry）後，偵測是否有未完成的 TIMEOUT 指令，
 * 自動重新下發最後亮度指令，記錄 command_type=FAILSAFE。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DimmingSyncService {

    private final DimmingLogRepository logRepository;
    private final DimmingScheduleRepository scheduleRepository;
    private final DeviceRepository deviceRepository;
    private final MqttCommandPublisher mqttCommandPublisher;

    /**
     * D29: 設備恢復上線 → 自動同步最後調光指令。
     */
    @EventListener
    @Transactional
    public void onTelemetryIngested(TelemetryIngestedEvent event) {
        try {
            TenantContext.setCurrentTenantId(event.getTenantId());
            syncIfNeeded(event.getDeviceId());
        } catch (Exception e) {
            log.debug("[DimmingSync] Skip sync for device={}: {}", event.getDeviceId(), e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 手動同步 API 用。
     */
    @Transactional
    public void syncDevice(Long deviceId) {
        syncIfNeeded(deviceId);
    }

    private void syncIfNeeded(Long deviceId) {
        // 查最近一筆 TIMEOUT 的調光指令
        Optional<DimmingLog> timeoutLog = logRepository
                .findFirstByDeviceIdAndResultOrderBySentAtDesc(deviceId, DimmingResult.TIMEOUT);

        if (timeoutLog.isEmpty()) {
            return;
        }

        DimmingLog lastTimeout = timeoutLog.get();

        // 建立 FAILSAFE 同步指令
        DimmingLog syncLog = DimmingLog.builder()
                .deviceId(deviceId)
                .commandType(DimmingCommandType.FAILSAFE)
                .brightnessPct(lastTimeout.getBrightnessPct())
                .result(DimmingResult.PENDING)
                .sentAt(LocalDateTime.now())
                .scheduleId(lastTimeout.getScheduleId())
                .build();
        logRepository.save(syncLog);

        mqttCommandPublisher.sendCommand(deviceId, Map.of(
                "cmd", "dim",
                "value", lastTimeout.getBrightnessPct(),
                "logId", syncLog.getId(),
                "sync", true
        ));

        log.info("[DimmingSync] FAILSAFE sync sent — device={}, brightness={}%, logId={}",
                deviceId, lastTimeout.getBrightnessPct(), syncLog.getId());
    }
}
