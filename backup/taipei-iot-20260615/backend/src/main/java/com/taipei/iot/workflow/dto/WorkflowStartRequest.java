package com.taipei.iot.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkflowStartRequest(@NotBlank String workflowCode, @NotBlank String businessId,
		@NotBlank String businessType, String applicantId, String departmentId, String district, String contractId) {

}
