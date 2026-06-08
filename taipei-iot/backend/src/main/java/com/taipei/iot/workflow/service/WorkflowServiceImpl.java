package com.taipei.iot.workflow.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.workflow.dto.WorkflowInstanceResponse;
import com.taipei.iot.workflow.dto.WorkflowStepLogResponse;
import com.taipei.iot.workflow.entity.DelegateSetting;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.entity.WorkflowStepLog;
import com.taipei.iot.workflow.enums.WorkflowStatus;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import com.taipei.iot.workflow.repository.DelegateSettingRepository;
import com.taipei.iot.workflow.repository.WorkflowInstanceRepository;
import com.taipei.iot.workflow.repository.WorkflowStepLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkflowServiceImpl implements WorkflowService {

	private final WorkflowInstanceRepository instanceRepository;

	private final WorkflowStepLogRepository stepLogRepository;

	private final DelegateSettingRepository delegateSettingRepository;

	private final ApplicationEventPublisher eventPublisher;

	// ── 狀態轉換表（核心 FSM） ──
	private static final Map<String, Map<String, Set<String>>> TRANSITIONS = Map
		.of("FAULT_REVIEW", Map.of("OPEN", Set.of("REVIEW"), "REVIEW", Set.of("CONFIRMED", "REJECTED", "MERGED")),
				"REPAIR_DISPATCH",
				Map.of("PENDING", Set.of("ACCEPTED"), "ACCEPTED", Set.of("DISPATCHED"), "DISPATCHED",
						Set.of("IN_PROGRESS", "TRANSFERRED"), "IN_PROGRESS", Set.of("COMPLETION_REPORTED")),
				"REPAIR_CLOSE",
				Map.of("COMPLETION_REPORTED", Set.of("PENDING_REVIEW"), "PENDING_REVIEW", Set.of("CLOSED", "RETURNED"),
						"RETURNED", Set.of("COMPLETION_REPORTED")),
				"REPLACEMENT_REVIEW",
				Map.of("DRAFT", Set.of("DISPATCHED"), "DISPATCHED", Set.of("IN_PROGRESS"), "IN_PROGRESS",
						Set.of("SELF_CHECKED"), "SELF_CHECKED", Set.of("PENDING_REVIEW"), "PENDING_REVIEW",
						Set.of("CLOSED", "RETURNED"), "RETURNED", Set.of("PENDING_REVIEW")),
				"ASSET_CHANGE",
				Map.of("DRAFT", Set.of("PENDING_REVIEW"), "PENDING_REVIEW", Set.of("APPROVED", "RETURNED"), "APPROVED",
						Set.of("APPLIED"), "RETURNED", Set.of("PENDING_REVIEW")));

	private static final Set<String> TERMINAL_STEPS = Set.of("CONFIRMED", "REJECTED", "MERGED", "CLOSED", "APPLIED",
			"TRANSFERRED");

	private static final Map<String, String> INITIAL_STEPS = Map.of("FAULT_REVIEW", "OPEN", "REPAIR_DISPATCH",
			"PENDING", "REPAIR_CLOSE", "COMPLETION_REPORTED", "REPLACEMENT_REVIEW", "DRAFT", "ASSET_CHANGE", "DRAFT");

	@Override
	@Transactional
	public Long createInstance(String workflowType, String ticketType, Long ticketId, String creatorId) {
		Map<String, Set<String>> wfTransitions = TRANSITIONS.get(workflowType);
		if (wfTransitions == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "未知的流程類型: " + workflowType);
		}

		String initialStep = INITIAL_STEPS.get(workflowType);

		WorkflowInstance instance = WorkflowInstance.builder()
			.workflowType(workflowType)
			.ticketType(ticketType)
			.ticketId(ticketId)
			.currentStep(initialStep)
			.status(WorkflowStatus.ACTIVE)
			.creatorId(creatorId)
			.startedAt(LocalDateTime.now())
			.build();

		instance = instanceRepository.save(instance);
		return instance.getId();
	}

	@Override
	@Transactional
	public void transition(Long instanceId, String targetStep, String action, String actorId, String actorName,
			String comment, List<Map<String, Object>> attachments) {
		WorkflowInstance instance = instanceRepository.findById(instanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND));

		if (instance.getStatus() != WorkflowStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.WORKFLOW_INVALID_TRANSITION, "流程已結束");
		}

		// 1. 驗證狀態轉換合法性
		Map<String, Set<String>> wfTransitions = TRANSITIONS.get(instance.getWorkflowType());
		if (wfTransitions == null) {
			throw new BusinessException(ErrorCode.WORKFLOW_INVALID_TRANSITION);
		}
		Set<String> allowedTargets = wfTransitions.getOrDefault(instance.getCurrentStep(), Set.of());
		if (!allowedTargets.contains(targetStep)) {
			throw new BusinessException(ErrorCode.WORKFLOW_INVALID_TRANSITION,
					String.format("%s → %s 不合法", instance.getCurrentStep(), targetStep));
		}

		// 2. 自審防護：creator_id == actor_id 被擋
		if (instance.getCreatorId().equals(actorId)) {
			throw new BusinessException(ErrorCode.WORKFLOW_SELF_APPROVAL_NOT_ALLOWED);
		}

		// 3. 代理人判斷
		boolean isDelegated = false;
		String originalAssigneeId = null;

		if (instance.getAssignedTo() != null && !instance.getAssignedTo().equals(actorId)) {
			// actorId 不是原始簽核人 → 檢查是否為有效代理人
			Optional<DelegateSetting> delegate = delegateSettingRepository
				.findActiveByDelegator(instance.getAssignedTo(), LocalDate.now());

			if (delegate.isEmpty() || !delegate.get().getDelegateId().equals(actorId)) {
				throw new BusinessException(ErrorCode.WORKFLOW_NOT_ASSIGNED_TO_USER);
			}
			isDelegated = true;
			originalAssigneeId = instance.getAssignedTo();
		}

		// 4. 寫入 step_log
		String finalComment = isDelegated ? String.format("[代理簽核] %s。%s", actorName, comment != null ? comment : "")
				: comment;

		WorkflowStepLog log = WorkflowStepLog.builder()
			.instanceId(instanceId)
			.stepCode(targetStep)
			.action(action)
			.actorId(actorId)
			.actorName(actorName)
			.originalAssigneeId(originalAssigneeId)
			.isDelegated(isDelegated)
			.comment(finalComment)
			.attachments(attachments)
			.actedAt(LocalDateTime.now())
			.build();
		stepLogRepository.save(log);

		// 5. 更新 instance 狀態
		instance.setCurrentStep(targetStep);
		if (TERMINAL_STEPS.contains(targetStep)) {
			instance.setStatus(WorkflowStatus.COMPLETED);
			instance.setCompletedAt(LocalDateTime.now());
		}
		instanceRepository.save(instance);

		// 6. 發布事件
		eventPublisher.publishEvent(new WorkflowTransitionEvent(this, instance, targetStep, action));
	}

	@Override
	public Page<WorkflowInstanceResponse> getMyPendingTasks(String userId, Pageable pageable) {
		Set<String> assigneeIds = new HashSet<>();
		assigneeIds.add(userId);

		// 查詢誰把我設為代理人（且日期有效）
		List<DelegateSetting> activeDelegations = delegateSettingRepository.findActiveDelegationsForDelegate(userId,
				LocalDate.now());
		for (DelegateSetting ds : activeDelegations) {
			assigneeIds.add(ds.getDelegatorId());
		}

		Page<WorkflowInstance> instances = instanceRepository.findPendingByAssignees(assigneeIds, WorkflowStatus.ACTIVE,
				pageable);

		return instances.map(inst -> {
			WorkflowInstanceResponse resp = toInstanceResponse(inst);
			if (!userId.equals(inst.getAssignedTo())) {
				resp.setDelegatedFrom(inst.getAssignedTo());
			}
			return resp;
		});
	}

	@Override
	public List<WorkflowStepLogResponse> getStepLogs(Long instanceId) {
		return stepLogRepository.findByInstanceIdOrderByActedAtAsc(instanceId)
			.stream()
			.map(this::toStepLogResponse)
			.toList();
	}

	@Override
	@Transactional
	public void cancel(Long instanceId, String actorId) {
		WorkflowInstance instance = instanceRepository.findById(instanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND));

		if (instance.getStatus() != WorkflowStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.WORKFLOW_INVALID_TRANSITION, "流程已結束");
		}

		instance.setStatus(WorkflowStatus.CANCELLED);
		instance.setCompletedAt(LocalDateTime.now());
		instanceRepository.save(instance);
	}

	@Override
	public WorkflowInstance findByTicket(String ticketType, Long ticketId) {
		return instanceRepository.findByTicketTypeAndTicketId(ticketType, ticketId)
			.orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND));
	}

	private WorkflowInstanceResponse toInstanceResponse(WorkflowInstance inst) {
		return WorkflowInstanceResponse.builder()
			.id(inst.getId())
			.workflowType(inst.getWorkflowType())
			.ticketType(inst.getTicketType())
			.ticketId(inst.getTicketId())
			.currentStep(inst.getCurrentStep())
			.status(inst.getStatus())
			.assignedTo(inst.getAssignedTo())
			.creatorId(inst.getCreatorId())
			.startedAt(inst.getStartedAt())
			.completedAt(inst.getCompletedAt())
			.updatedAt(inst.getUpdatedAt())
			.build();
	}

	private WorkflowStepLogResponse toStepLogResponse(WorkflowStepLog log) {
		return WorkflowStepLogResponse.builder()
			.id(log.getId())
			.stepCode(log.getStepCode())
			.action(log.getAction())
			.actorId(log.getActorId())
			.actorName(log.getActorName())
			.originalAssigneeId(log.getOriginalAssigneeId())
			.isDelegated(log.getIsDelegated())
			.comment(log.getComment())
			.attachments(log.getAttachments())
			.actedAt(log.getActedAt())
			.build();
	}

}
