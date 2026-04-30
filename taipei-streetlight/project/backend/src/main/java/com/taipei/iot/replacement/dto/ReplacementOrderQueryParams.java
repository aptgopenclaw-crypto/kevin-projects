package com.taipei.iot.replacement.dto;

import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementOrderQueryParams {

    private ReplacementOrderStatus status;
    private ReplacementOrderType orderType;
    private Long contractId;
    private String keyword;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
