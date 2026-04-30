package com.taipei.iot.device.dto;

import com.taipei.iot.device.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContractResponse {

    private Long id;
    private String contractCode;
    private String contractName;
    private Integer budgetYear;
    private String procurementNumber;
    private String contractorName;
    private String contractorContact;
    private String assetCategory;
    private Integer quantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate acceptanceDate;
    private Integer warrantyYears;
    private LocalDate warrantyExpiry;
    private ContractStatus status;
    private Map<String, Object> attributes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
