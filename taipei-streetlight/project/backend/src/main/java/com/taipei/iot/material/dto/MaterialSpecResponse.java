package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.material.enums.MaterialStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MaterialSpecResponse {
    private Long id;
    private String specCode;
    private String specName;
    private MaterialCategory category;
    private String unit;
    private Map<String, Object> attributes;
    private MaterialStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
