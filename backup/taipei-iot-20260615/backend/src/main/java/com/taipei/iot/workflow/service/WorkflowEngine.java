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
	public WorkflowInstanceEntity start(String workflowCode, String businessId, String businessType,
			WorkflowContext context) {
		WorkflowDefinitionEntity def = definitionRepo.findByCodeAndEnabledTrue(workflowCode)
			.orElseThrow(() -> new WorkflowNotFoundException(workflowCode));

		WorkflowStepsJson stepsJson = parseStepsJson(def.getStepsJson());
		StepDefinition firstStep = findStep(stepsJson, stepsJson.getInitialStep());

		WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
			.workflowDefId(def.getId())
			.businessId(businessId)
			.businessType(businessType)
			.currentStepId(firstStep.getId())
			.status("IN_PROGRESS")
			.contextJson(toJson(context))
			.build();
		instance = instanceRepo.save(instance);

		createStepLog(instance, firstStep, context);
		return instance;
	}

	/**
	 * 審核通過，推進到下一步驟。
	 */
	@Transactional
	public WorkflowInstanceEntity approve(Long instanceId, String comment, String userId) {
		WorkflowInstanceEntity instance = instanceRepo.findByIdForUpdate(instanceId)
			.orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));

		WorkflowStepLogEntity currentLog = requireCurrentLog(instanceId);
		validateAssignee(currentLog, userId);

		// early check：步驟已完成則立即拋出，不需再 loadDef
		if (currentLog.getCompletedAt() != null) {
			throw new WorkflowStepAlreadyCompletedException(currentLog.getStepName());
		}

		WorkflowStepsJson stepsJson = parseStepsJson(loadDef(instance).getStepsJson());
		StepDefinition currentStep = findStep(stepsJson, currentLog.getStepId());

		completeLog(currentLog, "approve", comment, null);

		if ("end".equals(currentStep.getType())) {
			instance.setStatus("COMPLETED");
			instance.setUpdatedAt(LocalDateTime.now());
			return instanceRepo.save(instance);
		}

		StepDefinition nextStep = findStep(stepsJson, currentStep.getNext());
		instance.setCurrentStepId(nextStep.getId());
		instance.setUpdatedAt(LocalDateTime.now());
		instance = instanceRepo.save(instance);

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
		WorkflowInstanceEntity instance = instanceRepo.findByIdForUpdate(instanceId)
			.orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));

		WorkflowStepLogEntity currentLog = requireCurrentLog(instanceId);
		validateAssignee(currentLog, userId);

		if (currentLog.getCompletedAt() != null) {
			throw new WorkflowStepAlreadyCompletedException(currentLog.getStepName());
		}

		WorkflowStepsJson stepsJson = parseStepsJson(loadDef(instance).getStepsJson());
		StepDefinition currentStep = findStep(stepsJson, currentLog.getStepId());

		if (!targetStepId.equals(currentStep.getRejectTarget())) {
			throw new WorkflowInvalidTransitionException(currentStep.getId(), targetStepId);
		}

		completeLog(currentLog, "reject", comment, targetStepId);

		StepDefinition targetStep = findStep(stepsJson, targetStepId);
		instance.setCurrentStepId(targetStep.getId());
		instance.setUpdatedAt(LocalDateTime.now());
		instance = instanceRepo.save(instance);

		WorkflowContext context = parseContext(instance.getContextJson());
		createStepLog(instance, targetStep, context);
		return instance;
	}

	/**
	 * 補件重送，回到最近一次退回的來源步驟。
	 */
	@Transactional
	public WorkflowInstanceEntity resubmit(Long instanceId, String comment, String userId) {
		WorkflowInstanceEntity instance = instanceRepo.findByIdForUpdate(instanceId)
			.orElseThrow(() -> new WorkflowInstanceNotFoundException(instanceId));

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
