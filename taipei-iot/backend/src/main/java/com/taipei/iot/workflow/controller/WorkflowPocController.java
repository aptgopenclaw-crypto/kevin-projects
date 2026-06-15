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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "WorkflowPOC", description = "簽核流程 POC：啟動、審核、退回、重送與代理設定")
public class WorkflowPocController {

	private final WorkflowEngine workflowEngine;

	private final DelegateSettingRepository delegateSettingRepository;

	@PostMapping("/start")
	@Operation(summary = "啟動流程", description = "依 workflowCode 建立新的流程實例並寫入第一個待辦步驟")
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
	@Operation(summary = "流程審核通過", description = "將指定流程實例推進到下一步")
	public BaseResponse<WorkflowInstanceEntity> approve(@Valid @RequestBody WorkflowApproveRequest req) {
		WorkflowInstanceEntity result = workflowEngine.approve(req.instanceId(), req.comment(), req.userId());
		return BaseResponse.success(result);
	}

	@PostMapping("/reject")
	@Operation(summary = "流程退回", description = "將指定流程實例退回到 targetStepId")
	public BaseResponse<WorkflowInstanceEntity> reject(@Valid @RequestBody WorkflowRejectRequest req) {
		WorkflowInstanceEntity result = workflowEngine.reject(req.instanceId(), req.targetStepId(), req.comment(),
				req.userId());
		return BaseResponse.success(result);
	}

	@PostMapping("/resubmit")
	@Operation(summary = "流程重送", description = "將已退回的流程重送回最近一次退回來源步驟")
	public BaseResponse<WorkflowInstanceEntity> resubmit(@Valid @RequestBody WorkflowResubmitRequest req) {
		WorkflowInstanceEntity result = workflowEngine.resubmit(req.instanceId(), req.comment(), req.userId());
		return BaseResponse.success(result);
	}

	@GetMapping("/instance/{id}")
	@Operation(summary = "查詢流程實例", description = "依流程實例 ID 查詢目前狀態與流程資訊")
	public BaseResponse<WorkflowInstanceEntity> getInstance(@PathVariable Long id) {
		return BaseResponse.success(workflowEngine.getInstance(id));
	}

	@GetMapping("/history/{id}")
	@Operation(summary = "查詢流程歷程", description = "回傳指定流程實例的步驟歷程清單")
	public BaseResponse<List<WorkflowStepLogEntity>> getHistory(@PathVariable Long id) {
		return BaseResponse.success(workflowEngine.getHistory(id));
	}

	@PostMapping("/delegate")
	@Operation(summary = "設定代理人", description = "建立一筆流程代理設定（delegate for / to）")
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
