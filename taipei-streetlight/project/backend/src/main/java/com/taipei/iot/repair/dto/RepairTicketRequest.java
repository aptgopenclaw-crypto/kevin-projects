package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RepairTicketRequest {

    @NotNull(message = "報修來源為必填")
    private RepairTicketSource source;

    private String reporterName;
    private String reporterPhone;
    private String reporterEmail;
    private String reportAddress;
    private String reportDescription;
    private Long deviceId;
    private Long circuitId;
    private Long contractId;
    private Long faultTicketId;
    private RepairTicketPriority priority;
    private Long deptId;
}
