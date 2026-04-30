package com.taipei.iot.replacement.dto;

import com.taipei.iot.replacement.enums.ReplacementOrderType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementOrderRequest {

    @NotNull(message = "換裝類型為必填")
    private ReplacementOrderType orderType;

    private Long repairTicketId;
    private Long contractId;
    private String dispatchReason;
    private String location;
    private Integer expectedQuantity;
    private LocalDate workPeriodStart;
    private LocalDate workPeriodEnd;
    private String assignedContractor;
    private Long deptId;
}
