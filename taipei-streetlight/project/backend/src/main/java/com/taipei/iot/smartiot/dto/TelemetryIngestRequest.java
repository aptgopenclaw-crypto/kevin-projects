package com.taipei.iot.smartiot.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TelemetryIngestRequest {

    /** 上報時間（可選，預設 server now） */
    private LocalDateTime timestamp;

    /** 遙測資料 JSONB payload */
    @NotEmpty(message = "payload 不可為空")
    private Map<String, Object> payload;
}
