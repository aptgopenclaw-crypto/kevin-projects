package com.taipei.iot.tender.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 設定連線至 NVIDIA AI (OpenAI 相容介面) 的 RestClient。
 */
@Configuration
public class NvidiaAiConfig {

    @Bean("nvidiaRestClient")
    public RestClient nvidiaRestClient(NvidiaAiProperties props) {
        return RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
