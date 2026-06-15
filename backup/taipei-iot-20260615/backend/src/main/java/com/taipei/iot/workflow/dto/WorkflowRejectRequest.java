package com.taipei.iot.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowRejectRequest(@NotNull Long instanceId, @NotBlank String targetStepId, @NotBlank String userId,
		String comment) {

}
