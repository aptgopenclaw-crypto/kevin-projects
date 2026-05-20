package com.taipei.iot.tender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorOverviewResponse {
    private String vendorName;
    private String vendorTaxId;
    private long totalWins;
    private BigDecimal totalAmount;
    private long agencyCount;
    private long solutionCount;
    private LocalDate firstAwardDate;
    private LocalDate latestAwardDate;
}
