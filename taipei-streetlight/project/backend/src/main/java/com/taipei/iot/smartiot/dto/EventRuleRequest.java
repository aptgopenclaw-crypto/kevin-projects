package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.ConditionLogic;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventRuleRequest {

    @NotBlank(message = "規則名稱為必填")
    @Size(max = 200)
    private String ruleName;

    private String description;

    @NotNull(message = "嚴重等級為必填")
    private AlertSeverity severity;

    /** JSONB: {"deviceType":"LUMINAIRE","areaIds":[1,2]} */
    private Map<String, Object> targetScope;

    private Long formatId;

    private ConditionLogic conditionLogic;

    private Integer suppressDurationMin;

    private Boolean autoCreateTicket;

    private Boolean enabled;

    /** 建立時一併帶入條件 */
    @Valid
    private List<EventRuleConditionRequest> conditions;
}
