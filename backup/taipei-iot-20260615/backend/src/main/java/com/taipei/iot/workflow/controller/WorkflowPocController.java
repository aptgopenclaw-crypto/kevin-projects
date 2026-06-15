package com.taipei.iot.workflow.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.workflow.dto.DelegateSetRequest;
import com.taipei.iot.workflow.dto.WorkflowApproveRequest;
import com.taipei.iot.workflow.dto.WorkflowRejectRequest;
import com.taipei.iot.workflow.dto.WorkflowResubmitRequest;
import com.taipei.iot.workflow.dto.WorkflowStartRequest;
import com.taipei.iot.workflow.entity.DelegateSettingEntity;
import com.taipei.iot.workflow.entity.WorkflowInstanceEntity;
import com.taipei.iot.workflow.entity.WorkflowStepLogEntity;
import com.taipei.iot.workflow.model.WorkflowContext;
import com.taipei.iot.workflow.repository.DelegateSettingRepository;
import com.taipei.iot.workflow.service.WorkflowEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POC 簽核引擎 REST API — 僅供開發驗證使用，不掛 Spring Security 細粒度權限。
 */
@RestController
@RequestMapping("/api/poc/workflow")
@RequiredArgsConstructor
public class WorkflowPocController {

	private final WorkflowEngine workflowEngine;

	private final DelegateSettingRepository delegateSettingRepository;

	@PostMapping("/start")
	public BaseResponse<WorkflowInstanceEntity> start(@Valid @RequestBody WorkflowStartRequest req) {
		WorkflowContext context = WorkflowContext.builder()
			.businessId(req.businessId())
			.businessType(req.businessType())
			.applicantId(req.applicantId())
			.departmentId(req.departmentId())
			.district(req.district())
			.contractId(req.contractId())
			.build();
		WorkflowInstanceEntity result = workflowEngine.start(req.workflowCode(), req.businessId(), req.businessType(),
				context);
		return BaseResponse.success(result);
	}

	@PostMapping("/approve")
	public BaseResponse<WorkflowInstanceEntity> approve(@Valid @RequestBody WorkflowApproveRequest req) {
		WorkflowInstanceEntity result = workflowEngine.approve(req.instanceId(), req.comment(), req.userId());
		return BaseResponse.success(result);
	}

	@PostMapping("/reject")
	public BaseResponse<WorkflowInstanceEntity> reject(@Valid @RequestBody WorkflowRejectRequest req) {
		WorkflowInstanceEntity result = workflowEngine.reject(req.instanceId(), req.targetStepId(), req.comment(),
				req.userId());
		return BaseResponse.success(result);
	}

	@PostMapping("/resubmit")
	public BaseResponse<WorkflowInstanceEntity> resubmit(@Valid @RequestBody WorkflowResubmitRequest req) {
		WorkflowInstanceEntity result = workflowEngine.resubmit(req.instanceId(), req.comment(), req.userId());
		return BaseResponse.success(result);
	}

	@GetMapping("/instance/{id}")
	public BaseResponse<WorkflowInstanceEntity> getInstance(@PathVariable Long id) {
		return BaseResponse.success(workflowEngine.getInstance(id));
	}

	@GetMapping("/history/{id}")
	public BaseResponse<List<WorkflowStepLogEntity>> getHistory(@PathVariable Long id) {
		return BaseResponse.success(workflowEngine.getHistory(id));
	}

	@PostMapping("/delegate")
	public BaseResponse<DelegateSettingEntity> setDelegate(@Valid @RequestBody DelegateSetRequest req) {
		DelegateSettingEntity entity = DelegateSettingEntity.builder()
			.delegateFor(req.delegateFor())
			.delegateTo(req.delegateTo())
			.businessType(req.businessType())
			.effectiveFrom(req.effectiveFrom())
			.effectiveTo(req.effectiveTo())
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
		return BaseResponse.success(delegateSettingRepository.save(entity));
	}

}
