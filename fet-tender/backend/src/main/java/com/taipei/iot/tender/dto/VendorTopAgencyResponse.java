package com.taipei.iot.tender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorTopAgencyResponse {
    private String agencyName;
    private String agencyCode;
    private long count;
    private BigDecimal totalAmount;
}
