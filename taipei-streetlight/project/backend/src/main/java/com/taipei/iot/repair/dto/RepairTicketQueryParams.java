package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RepairTicketQueryParams {

    private RepairTicketStatus status;
    private RepairTicketSource source;
    private RepairTicketPriority priority;
    private Long deptId;
    private String keyword;
}
