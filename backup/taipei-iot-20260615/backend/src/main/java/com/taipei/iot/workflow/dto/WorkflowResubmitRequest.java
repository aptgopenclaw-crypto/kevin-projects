package com.taipei.iot.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowResubmitRequest(@NotNull Long instanceId, @NotBlank String userId, String comment) {

}
