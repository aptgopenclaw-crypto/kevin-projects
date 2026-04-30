package com.taipei.iot.smartiot.service;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.event.TelemetryIngestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 設備狀態即時推送服務 (FN-07-010, D19)。
 * <p>
 * 監聽 TelemetryIngestedEvent → STOMP broadcast 到
 * /topic/tenant/{tenantId}/map/device-status。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatusPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceRepository deviceRepository;
    private final IoTMapService iotMapService;

    @EventListener
    public void onTelemetryIngested(TelemetryIngestedEvent event) {
        try {
            Device device = deviceRepository.findById(event.getDeviceId()).orElse(null);
            if (device == null) {
                return;
            }

            Map<String, Object> payload = iotMapService.buildDeviceStatusPayload(device);

            String destination = "/topic/tenant/" + event.getTenantId() + "/map/device-status";
            messagingTemplate.convertAndSend(destination, payload);

            log.debug("[DeviceStatusPush] Pushed status for device={} to {}",
                    event.getDeviceId(), destination);
        } catch (Exception e) {
            log.warn("[DeviceStatusPush] Failed for device={}: {}",
                    event.getDeviceId(), e.getMessage());
        }
    }
}
