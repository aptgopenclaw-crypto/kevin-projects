package com.taipei.iot.tender.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * NVIDIA AI API 設定（OpenAI 相容介面）。
 * 在 application.yml 中以 nvidia.ai.* 設定。
 */
@Component
@ConfigurationProperties(prefix = "nvidia.ai")
@Data
public class NvidiaAiProperties {

    /** NVIDIA API Key */
    private String apiKey;

    /** API Base URL（NVIDIA OpenAI 相容端點） */
    private String baseUrl = "https://integrate.api.nvidia.com/v1";

    /** 使用的模型名稱 */
    private String model = "deepseek-ai/deepseek-r1";

    /** 最大輸出 Token 數 */
    private int maxTokens = 2048;

    /** 溫度（0.0 ~ 1.0），越低越穩定 */
    private double temperature = 0.6;
}
