package com.taipei.iot.tender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorSuggestResponse {
    private String vendorName;
    private String vendorTaxId;
    private long winCount;
}
