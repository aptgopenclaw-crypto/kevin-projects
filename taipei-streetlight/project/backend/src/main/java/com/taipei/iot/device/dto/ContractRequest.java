package com.taipei.iot.device.dto;

import com.taipei.iot.device.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContractRequest {

    @NotBlank(message = "契約代碼為必填")
    private String contractCode;

    @NotBlank(message = "契約名稱為必填")
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
}
