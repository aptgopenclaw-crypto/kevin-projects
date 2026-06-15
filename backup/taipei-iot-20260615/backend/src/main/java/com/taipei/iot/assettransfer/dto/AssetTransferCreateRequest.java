package com.taipei.iot.assettransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AssetTransferCreateRequest(@NotBlank String assetCode, @NotBlank String assetName,
		@NotBlank String transferType, @NotNull Long departmentId, Long targetDepartmentId, String reason,
		BigDecimal assetValue) {

}
