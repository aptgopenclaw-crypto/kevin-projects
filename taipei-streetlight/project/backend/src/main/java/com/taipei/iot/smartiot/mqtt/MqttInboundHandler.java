package com.taipei.iot.smartiot.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.service.DimmingService;
import com.taipei.iot.smartiot.service.TelemetryService;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT 上行訊息處理器。
 * 訂閱 device/+/telemetry → 解析 device_id → 驗證 token → 委派 TelemetryService.ingest()
 * 訂閱 device/+/ack → 解析 logId + success → 委派 DimmingService.onAck() (D22)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttInboundHandler {

    private static final Pattern TELEMETRY_PATTERN = Pattern.compile("^device/(\\d+)/telemetry$");
    private static final Pattern ACK_PATTERN = Pattern.compile("^device/(\\d+)/ack$");

    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;
    private final TelemetryService telemetryService;
    private final DimmingService dimmingService;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handleMessage(Message<?> message) throws MessagingException {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = message.getPayload().toString();

        if (topic == null) {
            log.warn("[MQTT] Received message without topic, skipping");
            return;
        }

        // ── ACK 路徑 (D22) ──
        Matcher ackMatcher = ACK_PATTERN.matcher(topic);
        if (ackMatcher.matches()) {
            handleAck(ackMatcher.group(1), payload);
            return;
        }

        // ── Telemetry 路徑 ──
        Matcher matcher = TELEMETRY_PATTERN.matcher(topic);
        if (!matcher.matches()) {
            log.warn("[MQTT] Unexpected topic format: {}", topic);
            return;
        }

        long deviceId;
        try {
            deviceId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            log.warn("[MQTT] Invalid device ID in topic: {}", topic);
            return;
        }

        // 驗證設備存在且有 device_token
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            log.warn("[MQTT] Unknown device ID: {}", deviceId);
            return;
        }

        Device device = deviceOpt.get();
        if (device.getDeviceToken() == null) {
            log.warn("[MQTT] Device {} has no IoT token, rejecting telemetry", deviceId);
            return;
        }

        // 解析 JSON payload
        Map<String, Object> data;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(payload, Map.class);
            data = parsed;
        } catch (JsonProcessingException e) {
            log.warn("[MQTT] Invalid JSON payload from device {}: {}", deviceId, e.getMessage());
            return;
        }

        log.info("[MQTT] Telemetry received — device={}, fields={}", deviceId, data.keySet());

        // 設定 TenantContext 以便 Service 層使用
        try {
            TenantContext.setCurrentTenantId(device.getTenantId());
            telemetryService.ingest(deviceId, data, null);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 處理設備 ACK 回報 (D22)。
     * 預期 payload: {"logId": 123, "success": true}
     */
    private void handleAck(String deviceIdStr, String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            Object logIdObj = data.get("logId");
            Object successObj = data.get("success");

            if (logIdObj == null) {
                log.warn("[MQTT] ACK missing logId — device={}", deviceIdStr);
                return;
            }

            long logId = logIdObj instanceof Number n ? n.longValue() : Long.parseLong(logIdObj.toString());
            boolean success = successObj == null || Boolean.TRUE.equals(successObj);

            dimmingService.onAck(logId, success);
            log.info("[MQTT] ACK processed — device={}, logId={}, success={}", deviceIdStr, logId, success);
        } catch (Exception e) {
            log.warn("[MQTT] Failed to process ACK — device={}, error={}", deviceIdStr, e.getMessage());
        }
    }
}
