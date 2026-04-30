package com.taipei.iot.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DelegateSettingResponse {

    private Long id;
    private String delegatorId;
    private String delegatorName;
    private String delegateId;
    private String delegateName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
