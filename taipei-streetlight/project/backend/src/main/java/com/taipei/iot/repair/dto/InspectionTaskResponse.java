package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.InspectionTaskStatus;
import com.taipei.iot.repair.enums.InspectionTaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InspectionTaskResponse {

    private Long id;
    private String taskName;
    private InspectionTaskType taskType;
    private String scheduleCron;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> areaScope;
    private Long deptId;
    private Long assignedTo;
    private InspectionTaskStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
