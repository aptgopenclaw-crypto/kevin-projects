package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.InspectionTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InspectionTaskRequest {

    @NotBlank(message = "任務名稱為必填")
    private String taskName;

    @NotNull(message = "任務類型為必填")
    private InspectionTaskType taskType;

    private String scheduleCron;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> areaScope;
    private Long deptId;
    private Long assignedTo;
}
