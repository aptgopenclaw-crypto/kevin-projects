package com.taipei.iot.smartiot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TelemetryFormatRequest {

    @NotBlank(message = "廠商名稱為必填")
    @Size(max = 100)
    private String vendorName;

    @NotBlank(message = "設備型號為必填")
    @Size(max = 100)
    private String deviceModel;

    private Integer version;

    private String description;

    /** 直接給定欄位定義 [{name, type, unit}, ...] */
    private List<Map<String, Object>> fieldDefinitions;

    /** 或上傳 JSON 範例，系統自動解析產生 fieldDefinitions */
    private Map<String, Object> samplePayload;
}
