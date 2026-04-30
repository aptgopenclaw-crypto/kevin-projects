package com.taipei.iot.kpi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KpiIndicatorRequest {

    @NotBlank(message = "指標代碼為必填")
    private String indicatorCode;

    @NotBlank(message = "指標名稱為必填")
    private String indicatorName;

    @NotNull(message = "指標分類為必填")
    private String category;

    private String formulaType;

    @NotBlank(message = "公式為必填")
    private String formula;

    private BigDecimal targetValue;

    private BigDecimal weight;

    private String dataSource;

    private String unit;

    private String description;
}
