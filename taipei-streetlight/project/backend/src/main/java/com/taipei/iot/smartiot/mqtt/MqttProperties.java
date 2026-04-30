package com.taipei.iot.smartiot.mqtt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private String brokerUrl = "tcp://localhost:1883";
    private String clientId = "taipei-iot-server";
    private String username;
    private String password;
    private String telemetryTopic = "device/+/telemetry";
    private String ackTopic = "device/+/ack";
    private String commandTopicPrefix = "device/";
    private int qos = 1;
    private long completionTimeout = 30000;
}
