package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.InspectionResult;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InspectionRecordRequest {

    @NotNull(message = "任務 ID 為必填")
    private Long taskId;

    private Long deviceId;

    @NotNull(message = "巡查結果為必填")
    private InspectionResult result;

    private String notes;
    private List<Map<String, Object>> attachments;
}
