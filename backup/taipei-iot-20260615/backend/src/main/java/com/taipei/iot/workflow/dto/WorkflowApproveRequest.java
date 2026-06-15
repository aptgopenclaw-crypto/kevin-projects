package com.taipei.iot.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowApproveRequest(@NotNull Long instanceId, @NotBlank String userId, String comment) {

}
