package com.taipei.iot.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.workflow.entity.WorkflowDefinitionEntity;
import com.taipei.iot.workflow.entity.WorkflowInstanceEntity;
import com.taipei.iot.workflow.entity.WorkflowStepLogEntity;
import com.taipei.iot.workflow.exception.WorkflowInvalidTransitionException;
import com.taipei.iot.workflow.exception.WorkflowInstanceNotFoundException;
import com.taipei.iot.workflow.exception.WorkflowNoRejectHistoryException;
import com.taipei.iot.workflow.exception.WorkflowNotFoundException;
import com.taipei.iot.workflow.exception.WorkflowPermissionException;
import com.taipei.iot.workflow.exception.WorkflowStepAlreadyCompletedException;
import com.taipei.iot.workflow.model.StepDefinition;
import com.taipei.iot.workflow.model.WorkflowContext;
import com.taipei.iot.workflow.model.WorkflowStepsJson;
import com.taipei.iot.workflow.repository.WorkflowDefinitionRepository;
import com.taipei.iot.workflow.repository.WorkflowInstanceRepository;
import com.taipei.iot.workflow.repository.WorkflowStepLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowEngine {

	private final WorkflowDefinitionRepository definitionRepo;

	private final WorkflowInstanceRepository instanceRepo;

	private final WorkflowStepLogRepository stepLogRepo;

	private final IAssigneeResolver assigneeResolver;

	private final ObjectMapper objectMapper;

	// ── Public API ─────────────────────────────────────────────────────────────

	/**
	 * 啟動新流程，建立實例並建立第一步驟待辦。
	 */
	@Transactional
	public WorkflowInstanceEntity start(String workflowCode, String businessId, String businessType,WorkflowContext context) {

		// 1. 讀取流程定義
		WorkflowDefinitionEntity def = definitionRepo.findByCodeAndEnabledTrue(workflowCode)
			.orElseThrow(() -> new WorkflowNotFoundException(workflowCode));

		// 2. 建立流程實例
		WorkflowStepsJson stepsJson = parseStepsJson(def.getStepsJson());
		StepDefinition firstStep = findStep(stepsJson, stepsJson.getInitialStep());

		// 2. 建立流程實例	
		WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
			.workflowDefId(def.getId())
			.businessId(businessId)
			.businessType(businessType)
			.currentStepId(firstStep.getId())
			.status("IN_PROGRESS")
			.contextJson(toJson(context))
			.build();
		
		// 3. 寫入資料庫
		instance = instanceRepo.save(instance);

		// 4. 建立第一步驟的待辦
		createStepLog(instance, firstStep, context);

		return instance;
	}

	/**
	 * 審核通過，推進到下一步驟。
	 */
	@Transactional
	public WorkflowInstanceEntity approve(Long instanceId, String comment, String userId) {

		// 加鎖讀取流程實例與目前步驟待辦，確保同一時間只有一個人能對同一個流程實例進行審核動作
		WorkflowInstanceEntity instance = instanceRepo.findByIdForUpdate(instanceId)
			.orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));

		// 讀取目前步驟待辦，驗證審核人員身份與步驟狀態
		WorkflowStepLogEntity currentLog = requireCurrentLog(instanceId);
		validateAssignee(currentLog, userId);

		//（這裡的狀態檢查主要是為了避免重複審核同一個步驟，造成流程狀態異常；
		// 實際上在 controller 層也會檢查待辦清單，理論上不太可能發生，但還是加上這層保險）
		if (currentLog.getCompletedAt() != null) {
			throw new WorkflowStepAlreadyCompletedException(currentLog.getStepName());
		}

		// 讀取流程定義與步驟資訊，驗證流程定義與下一步驟存在
		WorkflowStepsJson stepsJson = parseStepsJson(loadDef(instance).getStepsJson());

		// 讀取目前步驟資訊，驗證流程定義與目前步驟存在
		StepDefinition currentStep = findStep(stepsJson, currentLog.getStepId());

		// 完成目前步驟待辦
		completeLog(currentLog, "approve", comment, null);

		// 如果下一步驟是 end 就直接完成流程，否则推进到下一步
		if ("end".equals(currentStep.getType())) {
			instance.setStatus("COMPLETED");
			instance.setUpdatedAt(LocalDateTime.now());
			return instanceRepo.save(instance);
		}

		// 推進到下一步，更新流程實例的 currentStepId，並建立下一步的待辦
		StepDefinition nextStep = findStep(stepsJson, currentStep.getNext());
		instance.setCurrentStepId(nextStep.getId());
		instance.setUpdatedAt(LocalDateTime.now());

		// 寫入資料庫
		instance = instanceRepo.save(instance);

		// 如果下一步不是 end 就建立待辦；如果是 end 就直接完成流程，不建立待辦
		if (!"end".equals(nextStep.getType())) {
			WorkflowContext context = parseContext(instance.getContextJson());
			createStepLog(instance, nextStep, context);
		}
		else {
			instance.setStatus("COMPLETED");
			instance.setUpdatedAt(LocalDateTime.now());
			instance = instanceRepo.save(instance);
		}
		return instance;
	}

	/**
	 * 審核退回，跳到指定步驟。
	 */
	@Transactional
	public WorkflowInstanceEntity reject(Long instanceId, String targetStepId, String comment, String userId) {

		// 加鎖讀取流程實例與目前步驟待辦，確保同一時間只有一個人能對同一個流程實例進行審核動作
		WorkflowInstanceEntity instance = instanceRepo.findByIdForUpdate(instanceId)
			.orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));

		// 讀取目前步驟待辦，驗證審核人員身份與步驟狀態
		WorkflowStepLogEntity currentLog = requireCurrentLog(instanceId);

		// end step 的 assigneeUserId 為 null，不做審核人驗證；其他步驟則驗證 userId 是否與 assigneeUserId 相符
		validateAssignee(currentLog, userId);

		//（這裡的狀態檢查主要是為了避免重複審核同一個步驟，造成流程狀態異常；
		// 實際上在 controller 層也會檢查待辦清單，理論上不太可能發生，但還是加上這層保險）
		if (currentLog.getCompletedAt() != null) {
			throw new WorkflowStepAlreadyCompletedException(currentLog.getStepName());
		}

		// 讀取流程定義與步驟資訊，驗證流程定義與目標步驟存在，並驗證目標步驟是目前步驟的 reject_target
		WorkflowStepsJson stepsJson = parseStepsJson(loadDef(instance).getStepsJson());
		
		// 讀取目前步驟資訊，驗證流程定義與目前步驟存在
		StepDefinition currentStep = findStep(stepsJson, currentLog.getStepId());

		// 驗證目標步驟是目前步驟的 reject_target
		if (!targetStepId.equals(currentStep.getRejectTarget())) {
			throw new WorkflowInvalidTransitionException(currentStep.getId(), targetStepId);
		}

		// 完成目前步驟待辦，並註記退回目標步驟
		completeLog(currentLog, "reject", comment, targetStepId);

		// 更新流程實例的 currentStepId 為退回目標步驟，並建立退回目標步驟的待辦
		StepDefinition targetStep = findStep(stepsJson, targetStepId);
		instance.setCurrentStepId(targetStep.getId());
		instance.setUpdatedAt(LocalDateTime.now());
		instance = instanceRepo.save(instance);

		// 建立退回目標步驟的待辦
		WorkflowContext context = parseContext(instance.getContextJson());

		createStepLog(instance, targetStep, context);
		return instance;
	}

	/**
	 * 補件重送，回到最近一次退回的來源步驟。
	 */
	@Transactional
	public WorkflowInstanceEntity resubmit(Long instanceId, String comment, String userId) {

		// 加鎖讀取流程實例與目前步驟待辦，確保同一時間只有一個人能對同一個流程實例進行審核動作
		WorkflowInstanceEntity instance = instanceRepo.findByIdForUpdate(instanceId)
			.orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));

		// 讀取目前步驟待辦，驗證審核人員身份與步驟狀態
		WorkflowStepLogEntity currentLog = requireCurrentLog(instanceId);
		validateAssignee(currentLog, userId);

		List<WorkflowStepLogEntity> history = stepLogRepo.findByWorkflowInstanceIdOrderByEnteredAtAsc(instanceId);

		WorkflowStepLogEntity lastReject = history.stream()
			.filter(l -> "reject".equals(l.getAction()))
			.reduce((a, b) -> b) // 最後一筆
			.orElseThrow(() -> new WorkflowNoRejectHistoryException(instanceId));

		String returnStepId = lastReject.getStepId();
		completeLog(currentLog, "resubmit", comment, null);

		WorkflowStepsJson stepsJson = parseStepsJson(loadDef(instance).getStepsJson());
		StepDefinition returnStep = findStep(stepsJson, returnStepId);
		instance.setCurrentStepId(returnStep.getId());
		instance.setUpdatedAt(LocalDateTime.now());
		instance = instanceRepo.save(instance);

		WorkflowContext context = parseContext(instance.getContextJson());
		createStepLog(instance, returnStep, context);
		return instance;
	}

	/**
	 * 取得目前步驟允許退回的目標步驟（若無退回目標則回傳 null）。
	 */
	@Transactional(readOnly = true)
	public StepDefinition getRejectTargetStep(Long instanceId) {
		WorkflowInstanceEntity instance = instanceRepo.findById(instanceId)
			.orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));
		WorkflowStepLogEntity currentLog = requireCurrentLog(instanceId);
		if (currentLog.getCompletedAt() != null) {
			return null;
		}
		WorkflowStepsJson stepsJson = parseStepsJson(loadDef(instance).getStepsJson());
		StepDefinition currentStep = findStep(stepsJson, currentLog.getStepId());
		if (currentStep.getRejectTarget() == null) {
			return null;
		}
		return findStep(stepsJson, currentStep.getRejectTarget());
	}

	/**
	 * 查詢流程實例。
	 */
	@Transactional(readOnly = true)
	public WorkflowInstanceEntity getInstance(Long instanceId) {
		return instanceRepo.findById(instanceId).orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));
	}

	/**
	 * 查詢步驟歷程。
	 */
	@Transactional(readOnly = true)
	public List<WorkflowStepLogEntity> getHistory(Long instanceId) {
		return stepLogRepo.findByWorkflowInstanceIdOrderByEnteredAtAsc(instanceId);
	}

	// ── Private helpers ────────────────────────────────────────────────────────

	private WorkflowDefinitionEntity loadDef(WorkflowInstanceEntity instance) {
		return definitionRepo.findById(instance.getWorkflowDefId())
			.orElseThrow(() -> new WorkflowNotFoundException("id=" + instance.getWorkflowDefId()));
	}

	private void createStepLog(WorkflowInstanceEntity instance, StepDefinition step, WorkflowContext context) {
		String assignee = null;
		if (step.getRoleCode() != null) {
			assignee = assigneeResolver.resolve(step, context);
		}
		WorkflowStepLogEntity log = WorkflowStepLogEntity.builder()
			.workflowInstanceId(instance.getId())
			.stepId(step.getId())
			.stepName(step.getName())
			.assigneeUserId(assignee)
			.build();
		stepLogRepo.save(log);
	}

	private void completeLog(WorkflowStepLogEntity log, String action, String comment, String targetStepId) {
		log.setAction(action);
		log.setComment(comment);
		log.setTargetStepId(targetStepId);
		log.setCompletedAt(LocalDateTime.now());
		stepLogRepo.save(log);
	}

	private void validateAssignee(WorkflowStepLogEntity log, String userId) {
		// end step 的 assigneeUserId 為 null，不做審核人驗證
		if (log.getAssigneeUserId() == null) {
			return;
		}
		if (!userId.equals(log.getAssigneeUserId())) {
			throw new WorkflowPermissionException(userId, log.getStepName());
		}
	}

	private WorkflowStepLogEntity requireCurrentLog(Long instanceId) {
		return stepLogRepo.findCurrentByInstanceId(instanceId)
			.orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));
	}

	private StepDefinition findStep(WorkflowStepsJson stepsJson, String stepId) {
		return stepsJson.getSteps()
			.stream()
			.filter(s -> s.getId().equals(stepId))
			.findFirst()
			.orElseThrow(() -> new WorkflowNotFoundException("step=" + stepId));
	}

	private WorkflowStepsJson parseStepsJson(String json) {
		try {
			return objectMapper.readValue(json, WorkflowStepsJson.class);
		}
		catch (JsonProcessingException e) {
			throw new WorkflowNotFoundException("steps_json 解析失敗：" + e.getMessage());
		}
	}

	private WorkflowContext parseContext(String json) {
		if (json == null) {
			return WorkflowContext.builder().build();
		}
		try {
			return objectMapper.readValue(json, WorkflowContext.class);
		}
		catch (JsonProcessingException e) {
			return WorkflowContext.builder().build();
		}
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		}
		catch (JsonProcessingException e) {
			return "{}";
		}
	}

}
