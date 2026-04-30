package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.ConditionOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventRuleConditionRequest {

    private Integer conditionGroup;

    @NotBlank(message = "欄位名稱為必填")
    @Size(max = 100)
    private String field;

    @NotNull(message = "運算子為必填")
    private ConditionOperator operator;

    @NotBlank(message = "門檻值為必填")
    @Size(max = 100)
    private String thresholdValue;

    private Integer sortOrder;
}
