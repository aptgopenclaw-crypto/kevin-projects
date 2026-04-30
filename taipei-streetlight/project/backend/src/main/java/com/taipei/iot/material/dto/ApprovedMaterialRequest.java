package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.ApprovedMaterialStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovedMaterialRequest {

    @NotNull(message = "材料規格為必填")
    private Long materialSpecId;

    private Long contractId;

    @NotBlank(message = "材料編號為必填")
    private String materialNumber;

    @NotNull(message = "審驗日期為必填")
    private LocalDate approvalDate;

    private String batchNumber;
    private String brand;
    private String model;
    private Map<String, Object> specDetails;
    private ApprovedMaterialStatus status;
}
