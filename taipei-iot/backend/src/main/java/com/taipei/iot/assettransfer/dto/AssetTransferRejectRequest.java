package com.taipei.iot.assettransfer.dto;

import jakarta.validation.constraints.NotBlank;

public record AssetTransferRejectRequest(String comment, @NotBlank String targetStepId) {

}
