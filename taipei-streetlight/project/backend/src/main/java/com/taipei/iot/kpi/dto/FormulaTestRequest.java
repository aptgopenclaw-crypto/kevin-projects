package com.taipei.iot.kpi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FormulaTestRequest {

    private String formulaType;

    @NotBlank(message = "公式為必填")
    private String formula;

    @NotNull(message = "測試數據為必填")
    private Map<String, Object> testData;
}
