package com.taipei.iot.assettransfer.service;

import com.taipei.iot.assettransfer.dto.AssetTransferCreateRequest;
import com.taipei.iot.assettransfer.entity.AssetTransferApplicationEntity;
import com.taipei.iot.assettransfer.repository.AssetTransferApplicationRepository;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.dept.repository.DeptInfoRepository;
import com.taipei.iot.workflow.entity.WorkflowInstanceEntity;
import com.taipei.iot.workflow.entity.WorkflowStepLogEntity;
import com.taipei.iot.workflow.model.WorkflowContext;
import com.taipei.iot.workflow.repository.WorkflowStepLogRepository;
import com.taipei.iot.workflow.service.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetTransferService {

	private static final String WORKFLOW_CODE = "asset_transfer";

	private static final String BUSINESS_TYPE = "ASSET_TRANSFER";

	private final AssetTransferApplicationRepository repository;

	private final WorkflowEngine workflowEngine;

	private final WorkflowStepLogRepository stepLogRepository;

	private final UserRepository userRepository;

	private final DeptInfoRepository deptInfoRepository;

	// ── 建立申請（草稿）─────────────────────────────────────────────────────────

	@Transactional
	public AssetTransferApplicationEntity create(AssetTransferCreateRequest req, String applicantId) {
		String applicantName = userRepository.findById(applicantId).map(u -> u.getDisplayName()).orElse(null);
		String departmentName = deptInfoRepository.findByDeptId(req.departmentId())
			.map(d -> d.getDeptName())
			.orElse(null);

		AssetTransferApplicationEntity app = AssetTransferApplicationEntity.builder()
			.applicationNo(generateApplicationNo())
			.applicantId(applicantId)
			.applicantName(applicantName)
			.departmentId(req.departmentId())
			.departmentName(departmentName)
			.assetCode(req.assetCode())
			.assetName(req.assetName())
			.transferType(req.transferType())
			.targetDepartmentId(req.targetDepartmentId())
			.reason(req.reason())
			.assetValue(req.assetValue())
			.status("DRAFT")
			.build();
		return repository.save(app);
	}

	// ── 送出申請（啟動流程）──────────────────────────────────────────────────────

	@Transactional
	public AssetTransferApplicationEntity submit(Long applicationId, String applicantId) {
		AssetTransferApplicationEntity app = findById(applicationId);

		if (!app.getApplicantId().equals(applicantId)) {
			throw new BusinessException(ErrorCode.ASSET_TRANSFER_PERMISSION_DENIED);
		}
		if (!"DRAFT".equals(app.getStatus())) {
			throw new BusinessException(ErrorCode.ASSET_TRANSFER_INVALID_STATUS, "目前狀態：" + app.getStatus());
		}

		WorkflowContext context = WorkflowContext.builder()
			.businessId(app.getApplicationNo())
			.businessType(BUSINESS_TYPE)
			.applicantId(applicantId)
			.departmentId(String.valueOf(app.getDepartmentId()))
			.build();

		WorkflowInstanceEntity instance = workflowEngine.start(WORKFLOW_CODE, app.getApplicationNo(), BUSINESS_TYPE,
				context);

		// 自動完成 ROLE_DEPT_USER 步驟（送出即代表申請人確認），推進到下一步驟（dept_admin）
		instance = workflowEngine.approve(instance.getId(), "申請人送出", applicantId);

		app.setWorkflowInstanceId(instance.getId());
		app.setStatus("PROCESSING");
		app.setCurrentAssignee(resolveCurrentAssignee(instance.getId()));
		return repository.save(app);
	}

	// ── 審核通過 ───────────────────────────────────────────────────────────────

	@Transactional
	public AssetTransferApplicationEntity approve(Long applicationId, String userId, String comment) {
		AssetTransferApplicationEntity app = findByIdAndAssertProcessing(applicationId);

		WorkflowInstanceEntity instance = workflowEngine.approve(app.getWorkflowInstanceId(), comment, userId);

		if ("COMPLETED".equals(instance.getStatus())) {
			app.setStatus("COMPLETED");
			app.setApprovedAt(LocalDateTime.now());
			app.setApprovedBy(userId);
			app.setCurrentAssignee(null);
		}
		else {
			app.setCurrentAssignee(resolveCurrentAssignee(instance.getId()));
		}
		return repository.save(app);
	}

	// ── 審核退回 ───────────────────────────────────────────────────────────────

	@Transactional
	public AssetTransferApplicationEntity reject(Long applicationId, String userId, String comment,
			String targetStepId) {
		AssetTransferApplicationEntity app = findByIdAndAssertProcessing(applicationId);

		WorkflowInstanceEntity instance = workflowEngine.reject(app.getWorkflowInstanceId(), targetStepId, comment,
				userId);

		app.setStatus("REJECTED");
		app.setRejectReason(comment);
		app.setCurrentAssignee(resolveCurrentAssignee(instance.getId()));
		return repository.save(app);
	}

	// ── 補件重送 ───────────────────────────────────────────────────────────────

	@Transactional
	public AssetTransferApplicationEntity resubmit(Long applicationId, String userId, String comment) {
		AssetTransferApplicationEntity app = findByIdAndAssertHasWorkflow(applicationId);

		WorkflowInstanceEntity instance = workflowEngine.resubmit(app.getWorkflowInstanceId(), comment, userId);

		app.setStatus("PROCESSING");
		app.setRejectReason(null);
		app.setCurrentAssignee(resolveCurrentAssignee(instance.getId()));
		return repository.save(app);
	}

	// ── 查詢 ───────────────────────────────────────────────────────────────────

	@Transactional(readOnly = true)
	public AssetTransferApplicationEntity getById(Long id) {
		return findById(id);
	}

	@Transactional(readOnly = true)
	public List<AssetTransferApplicationEntity> getMyApplications(String applicantId) {
		return repository.findByApplicantIdOrderByCreatedAtDesc(applicantId);
	}

	@Transactional(readOnly = true)
	public List<AssetTransferApplicationEntity> getPendingTasks(String userId) {
		List<Long> instanceIds = stepLogRepository.findByAssigneeUserIdAndCompletedAtIsNull(userId)
			.stream()
			.map(WorkflowStepLogEntity::getWorkflowInstanceId)
			.distinct()
			.collect(Collectors.toList());

		if (instanceIds.isEmpty()) {
			return List.of();
		}
		return repository.findByWorkflowInstanceIdIn(instanceIds);
	}

	// ── Private helpers ────────────────────────────────────────────────────────

	private AssetTransferApplicationEntity findById(Long id) {
		return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ASSET_TRANSFER_NOT_FOUND));
	}

	private AssetTransferApplicationEntity findByIdAndAssertProcessing(Long id) {
		AssetTransferApplicationEntity app = findById(id);
		if (!"PROCESSING".equals(app.getStatus())) {
			throw new BusinessException(ErrorCode.ASSET_TRANSFER_INVALID_STATUS, "目前狀態：" + app.getStatus());
		}
		return app;
	}

	private AssetTransferApplicationEntity findByIdAndAssertHasWorkflow(Long id) {
		AssetTransferApplicationEntity app = findById(id);
		if (app.getWorkflowInstanceId() == null) {
			throw new BusinessException(ErrorCode.ASSET_TRANSFER_WORKFLOW_NOT_STARTED);
		}
		return app;
	}

	private String resolveCurrentAssignee(Long workflowInstanceId) {
		return stepLogRepository.findCurrentByInstanceId(workflowInstanceId)
			.map(WorkflowStepLogEntity::getAssigneeUserId)
			.orElse(null);
	}

	private String generateApplicationNo() {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		return "AT-" + timestamp;
	}

}
