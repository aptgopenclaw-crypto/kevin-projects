package com.taipei.iot.smartiot.service;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.dto.MeterStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 電表服務 (FN-07-019, FN-07-022, D30)。
 * <p>
 * D30: 電表數據走現有 telemetry ingest 流程 (device_type=POWER_EQUIPMENT)。
 * 本 Service 提供電表狀態統計 (FN-07-022)。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterService {

    private static final int OFFLINE_THRESHOLD_MINUTES = 10;

    private final DeviceRepository deviceRepository;

    /**
     * 電表狀態總覽 (FN-07-022)。
     */
    public MeterStatusResponse getStatus() {
        List<Device> meters = deviceRepository.findAllByFilters(
                DeviceType.POWER_EQUIPMENT, null, null, null);

        long total = meters.size();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(OFFLINE_THRESHOLD_MINUTES);

        long online = meters.stream()
                .filter(m -> m.getLastTelemetryAt() != null && m.getLastTelemetryAt().isAfter(cutoff))
                .count();

        long anomaly = meters.stream()
                .filter(m -> m.getStatus() == com.taipei.iot.device.enums.DeviceStatus.REPORTED
                        || m.getStatus() == com.taipei.iot.device.enums.DeviceStatus.UNDER_REPAIR)
                .count();

        long offline = total - online;

        return MeterStatusResponse.builder()
                .totalMeters(total)
                .onlineMeters(online)
                .offlineMeters(offline)
                .anomalyMeters(anomaly)
                .build();
    }
}
