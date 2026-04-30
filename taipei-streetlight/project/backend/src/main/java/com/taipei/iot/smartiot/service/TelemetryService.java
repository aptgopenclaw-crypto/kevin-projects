package com.taipei.iot.smartiot.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.dto.TelemetryIngestRequest;
import com.taipei.iot.smartiot.dto.TelemetryResponse;
import com.taipei.iot.smartiot.engine.DataQualityEngine;
import com.taipei.iot.smartiot.entity.Telemetry;
import com.taipei.iot.smartiot.enums.QualityFlag;
import com.taipei.iot.smartiot.event.TelemetryIngestedEvent;
import com.taipei.iot.smartiot.repository.TelemetryRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final DeviceRepository deviceRepository;
    private final DataQualityEngine dataQualityEngine;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 核心遙測上行 (FN-07-003, FN-07-004)。
     * 1. 查詢 device → 取 format_id
     * 2. DataQualityEngine.check() → quality_flag
     * 3. INSERT telemetry
     * 4. UPDATE devices.last_telemetry_at / last_heartbeat_at
     * 5. 發布 TelemetryIngestedEvent (D8: 7e1 接 EventRuleEngine)
     */
    @Transactional
    public TelemetryResponse ingest(Long deviceId, Map<String, Object> payload, LocalDateTime timestamp) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        LocalDateTime time = (timestamp != null) ? timestamp : LocalDateTime.now();
        Long formatId = device.getFormatId();

        // 品質檢查
        QualityFlag qualityFlag = dataQualityEngine.check(formatId, payload);

        // 寫入 telemetry
        String tenantId = TenantContext.getCurrentTenantId();
        Telemetry telemetry = Telemetry.builder()
                .time(time)
                .tenantId(tenantId)
                .deviceId(deviceId)
                .formatId(formatId)
                .payload(payload)
                .qualityFlag(qualityFlag)
                .build();
        telemetry = telemetryRepository.save(telemetry);

        // 更新 device 時間戳
        LocalDateTime now = LocalDateTime.now();
        device.setLastTelemetryAt(now);
        device.setLastHeartbeatAt(now);
        deviceRepository.save(device);

        log.info("[Telemetry] Ingested — device={}, quality={}, fields={}", deviceId, qualityFlag, payload.keySet());

        // D8: 發布事件，7e1 透過 @EventListener 接入 EventRuleEngine
        eventPublisher.publishEvent(new TelemetryIngestedEvent(
                this, deviceId, formatId, payload, qualityFlag, tenantId));

        return toResponse(telemetry);
    }

    /**
     * 批次回補 (FN-07-008)。
     */
    @Transactional
    public List<TelemetryResponse> batchIngest(Long deviceId, List<TelemetryIngestRequest> records) {
        return records.stream()
                .map(r -> ingest(deviceId, r.getPayload(), r.getTimestamp()))
                .toList();
    }

    /**
     * 心跳上報 (FN-07-005)。
     * 更新 last_heartbeat_at（同時更新 last_telemetry_at 以反映設備在線）。
     */
    @Transactional
    public void heartbeat(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        device.setLastHeartbeatAt(now);
        deviceRepository.save(device);

        log.info("[Heartbeat] device={}", deviceId);
    }

    /**
     * 最新一筆 telemetry (FN-07-011)。
     */
    public TelemetryResponse getLatest(Long deviceId) {
        return telemetryRepository.findLatestByDeviceId(deviceId)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * 歷史 telemetry 查詢 (FN-07-012)。
     */
    public Page<TelemetryResponse> getHistory(Long deviceId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return telemetryRepository.findByDeviceIdAndTimeBetweenOrderByTimeDesc(
                deviceId, from, to, pageable).map(this::toResponse);
    }

    private TelemetryResponse toResponse(Telemetry t) {
        return TelemetryResponse.builder()
                .id(t.getId())
                .time(t.getTime())
                .deviceId(t.getDeviceId())
                .formatId(t.getFormatId())
                .payload(t.getPayload())
                .qualityFlag(t.getQualityFlag())
                .build();
    }
}
