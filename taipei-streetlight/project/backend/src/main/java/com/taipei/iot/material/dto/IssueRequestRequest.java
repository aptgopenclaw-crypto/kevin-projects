package com.taipei.iot.material.dto;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IssueRequestRequest {
    private Long repairTicketId;
    private Long replacementOrderId;
}
