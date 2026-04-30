package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.ApprovedMaterialStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovedMaterialResponse {
    private Long id;
    private Long materialSpecId;
    private String specCode;
    private String specName;
    private Long contractId;
    private String materialNumber;
    private LocalDate approvalDate;
    private String batchNumber;
    private String brand;
    private String model;
    private Map<String, Object> specDetails;
    private ApprovedMaterialStatus status;
    private LocalDateTime createdAt;
}
