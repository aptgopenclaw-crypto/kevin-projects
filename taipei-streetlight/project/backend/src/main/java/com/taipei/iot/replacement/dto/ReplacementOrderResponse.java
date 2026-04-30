package com.taipei.iot.replacement.dto;

import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementOrderResponse {

    private Long id;
    private String orderNumber;
    private Long repairTicketId;
    private Long contractId;
    private ReplacementOrderType orderType;
    private String dispatchReason;
    private String location;
    private Integer expectedQuantity;
    private LocalDate workPeriodStart;
    private LocalDate workPeriodEnd;
    private String assignedContractor;
    private ReplacementOrderStatus status;
    private Long deptId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReplacementItemResponse> items;
    private String currentStep;
}
