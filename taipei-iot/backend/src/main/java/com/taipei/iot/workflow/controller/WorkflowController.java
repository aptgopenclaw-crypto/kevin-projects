package com.taipei.iot.workflow.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.common.dto.PageResponse;
import com.taipei.iot.workflow.dto.WorkflowInstanceResponse;
import com.taipei.iot.workflow.dto.WorkflowStepLogResponse;
import com.taipei.iot.workflow.dto.WorkflowTransitionRequest;
import com.taipei.iot.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/workflow")
@RequiredArgsConstructor
public class WorkflowController {

	private final WorkflowService workflowService;

	@GetMapping("/pending")
	@PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
	public BaseResponse<PageResponse<WorkflowInstanceResponse>> getPendingTasks(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		String userId = SecurityContextUtils.getCurrentUserId();
		Page<WorkflowInstanceResponse> result = workflowService.getMyPendingTasks(userId, PageRequest.of(page, size));
		return BaseResponse.success(toPageResponse(result));
	}

	@GetMapping("/{instanceId}/logs")
	@PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
	public BaseResponse<List<WorkflowStepLogResponse>> getStepLogs(@PathVariable Long instanceId) {
		return BaseResponse.success(workflowService.getStepLogs(instanceId));
	}

	@PostMapping("/{instanceId}/transition")
	@PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
	@AuditEvent(AuditEventType.WORKFLOW_SUBMIT)
	public BaseResponse<Void> transition(@PathVariable Long instanceId,
			@Valid @RequestBody WorkflowTransitionRequest request) {
		String userId = SecurityContextUtils.getCurrentUserId();
		workflowService.transition(instanceId, request.getTargetStep(), request.getAction(), userId, null,
				request.getComment(), request.getAttachments());
		return BaseResponse.success(null);
	}

	@PostMapping("/{instanceId}/cancel")
	@PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
	public BaseResponse<Void> cancel(@PathVariable Long instanceId) {
		String userId = SecurityContextUtils.getCurrentUserId();
		workflowService.cancel(instanceId, userId);
		return BaseResponse.success(null);
	}

	private <T> PageResponse<T> toPageResponse(Page<T> page) {
		return PageResponse.<T>builder()
			.content(page.getContent())
			.totalElements(page.getTotalElements())
			.totalPages(page.getTotalPages())
			.page(page.getNumber())
			.size(page.getSize())
			.build();
	}

}
