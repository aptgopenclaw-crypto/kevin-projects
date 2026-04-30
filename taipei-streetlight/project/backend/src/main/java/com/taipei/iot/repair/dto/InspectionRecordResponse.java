package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.InspectionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InspectionRecordResponse {

    private Long id;
    private Long taskId;
    private Long inspectorId;
    private LocalDateTime inspectionDate;
    private Long deviceId;
    private InspectionResult result;
    private String notes;
    private List<Map<String, Object>> attachments;
    private Long faultTicketId;
    private LocalDateTime createdAt;
}
