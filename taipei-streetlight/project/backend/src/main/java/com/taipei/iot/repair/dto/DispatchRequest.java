package com.taipei.iot.repair.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DispatchRequest {

    private Long assignedTo;
    private String assignedOrg;
    @NotNull(message = "契約為必填")
    private Long contractId;
    private LocalDate dueDate;
    private String note;
}
