package com.taipei.iot.replacement.dto;

import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementItemResponse {

    private Long id;
    private Long orderId;
    private Long parentDeviceId;
    private String parentDeviceCode;
    private Long oldDeviceId;
    private String oldDeviceCode;
    private Long newDeviceId;
    private String newDeviceCode;
    private String beforeDeviceType;
    private Map<String, Object> beforeSpec;
    private String afterDeviceType;
    private Map<String, Object> afterSpec;
    private Long materialSpecId;
    private Long approvedMaterialId;
    private ReplacementItemStatus status;
    private LocalDateTime completedAt;
    private String completedBy;
    private String notes;
    private LocalDateTime createdAt;
}
