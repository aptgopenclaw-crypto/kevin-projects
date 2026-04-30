package com.taipei.iot.smartiot.service;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.gis.dto.GeoJsonResponse;
import com.taipei.iot.gis.dto.GeoJsonResponse.Feature;
import com.taipei.iot.gis.dto.GeoJsonResponse.Geometry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IoT 地圖狀態服務 (FN-07-009, D17/D18)。
 * <p>
 * 查詢 IoT 設備 + 計算 displayStatus + 組裝 GeoJSON FeatureCollection。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IoTMapService {

    private static final int OFFLINE_THRESHOLD_MINUTES = 10;

    private final DeviceRepository deviceRepository;

    /**
     * 取得 IoT 設備地圖狀態 (GeoJSON FeatureCollection)。
     */
    public GeoJsonResponse getMapStatus() {
        Page<Device> devices = deviceRepository.findIoTDevices(null, null, Pageable.unpaged());

        List<Feature> features = devices.getContent().stream()
                .filter(d -> d.getLng() != null && d.getLat() != null)
                .map(this::toFeature)
                .toList();

        return GeoJsonResponse.of(features);
    }

    /**
     * 計算單一設備的即時狀態 (供 WebSocket 推送使用)。
     */
    public Map<String, Object> buildDeviceStatusPayload(Device device) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", device.getId());
        props.put("deviceCode", device.getDeviceCode());
        props.put("displayStatus", computeDisplayStatus(device));
        props.put("lastTelemetryAt", device.getLastTelemetryAt());
        props.put("lastHeartbeatAt", device.getLastHeartbeatAt());
        if (device.getLng() != null && device.getLat() != null) {
            props.put("lng", device.getLng());
            props.put("lat", device.getLat());
        }
        return props;
    }

    private Feature toFeature(Device d) {
        Geometry geometry = Geometry.point(d.getLng(), d.getLat());

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", d.getId());
        props.put("deviceCode", d.getDeviceCode());
        props.put("deviceName", d.getDeviceName());
        props.put("deviceType", d.getDeviceType());
        props.put("status", d.getStatus());
        props.put("displayStatus", computeDisplayStatus(d));
        props.put("deptId", d.getDeptId());
        props.put("lastHeartbeatAt", d.getLastHeartbeatAt());
        props.put("lastTelemetryAt", d.getLastTelemetryAt());
        props.put("firmwareVersion", d.getFirmwareVersion());

        return Feature.of(geometry, props);
    }

    /**
     * 計算顯示狀態 (D18)。
     * <ul>
     *   <li>UNDER_REPAIR / REPORTED → FAULT</li>
     *   <li>DECOMMISSIONED / INACTIVE → OFFLINE</li>
     *   <li>lastTelemetryAt == null 或 > 10min → OFFLINE</li>
     *   <li>否則 → ONLINE</li>
     * </ul>
     */
    String computeDisplayStatus(Device d) {
        if (d.getStatus() == DeviceStatus.UNDER_REPAIR || d.getStatus() == DeviceStatus.REPORTED) {
            return "FAULT";
        }
        if (d.getStatus() == DeviceStatus.DECOMMISSIONED || d.getStatus() == DeviceStatus.INACTIVE) {
            return "OFFLINE";
        }
        if (d.getLastTelemetryAt() == null) {
            return "OFFLINE";
        }
        if (d.getLastTelemetryAt().isBefore(LocalDateTime.now().minusMinutes(OFFLINE_THRESHOLD_MINUTES))) {
            return "OFFLINE";
        }
        return "ONLINE";
    }
}
