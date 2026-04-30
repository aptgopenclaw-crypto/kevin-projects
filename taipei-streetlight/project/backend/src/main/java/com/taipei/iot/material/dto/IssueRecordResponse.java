package com.taipei.iot.material.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IssueRecordResponse {
    private Long id;
    private Long requestId;
    private Long inventoryId;
    private Long materialSpecId;
    private String specName;
    private Integer quantity;
    private String issuedBy;
    private LocalDateTime issuedAt;
}
