package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.SupplierStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierRequest {

    @NotBlank(message = "廠商代碼為必填")
    private String supplierCode;

    @NotBlank(message = "廠商名稱為必填")
    private String supplierName;

    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private SupplierStatus status;
}
