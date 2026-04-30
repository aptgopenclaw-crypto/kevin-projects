package com.taipei.iot.smartiot.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MQTT 指令下行發佈器。
 * 發送 QoS 1 訊息至 device/{id}/command，支援超時重試。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttCommandPublisher {

    private final MqttPahoMessageHandler mqttOutboundHandler;
    private final MqttProperties mqttProperties;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 2;

    /**
     * 發送指令至指定設備。
     *
     * @param deviceId 設備 ID
     * @param command  指令 payload (將序列化為 JSON)
     */
    public void sendCommand(Long deviceId, Map<String, Object> command) {
        String topic = mqttProperties.getCommandTopicPrefix() + deviceId + "/command";
        String payload;
        try {
            payload = objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            log.error("[MQTT] Failed to serialize command for device {}: {}", deviceId, e.getMessage());
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                Message<String> message = MessageBuilder
                        .withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .setHeader(MqttHeaders.QOS, mqttProperties.getQos())
                        .setHeader(MqttHeaders.RETAINED, false)
                        .build();
                mqttOutboundHandler.handleMessage(message);
                log.info("[MQTT] Command sent — device={}, topic={}, attempt={}", deviceId, topic, attempt);
                return;
            } catch (Exception e) {
                log.warn("[MQTT] Command send failed — device={}, attempt={}/{}, error={}",
                        deviceId, attempt, MAX_RETRIES + 1, e.getMessage());
                if (attempt > MAX_RETRIES) {
                    log.error("[MQTT] Command send exhausted retries — device={}, topic={}", deviceId, topic);
                }
            }
        }
    }
}
