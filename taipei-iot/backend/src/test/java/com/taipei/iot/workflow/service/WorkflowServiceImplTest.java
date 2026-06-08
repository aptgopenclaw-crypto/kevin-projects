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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceImplTest {

	@InjectMocks
	private WorkflowServiceImpl workflowService;

	@Mock
	private WorkflowInstanceRepository instanceRepository;

	@Mock
	private WorkflowStepLogRepository stepLogRepository;

	@Mock
	private DelegateSettingRepository delegateSettingRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	// ── helpers ───────────────────────────────────────────────────────────────

	private WorkflowInstance activeInstance(Long id, String workflowType, String currentStep, String creatorId,
			String assignedTo) {
		return WorkflowInstance.builder()
			.id(id)
			.tenantId("T1")
			.workflowType(workflowType)
			.ticketType("FAULT")
			.ticketId(100L)
			.currentStep(currentStep)
			.status(WorkflowStatus.ACTIVE)
			.creatorId(creatorId)
			.assignedTo(assignedTo)
			.startedAt(LocalDateTime.now().minusHours(1))
			.build();
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// createInstance()
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class CreateInstance {

		@Test
		void validWorkflowType_savesInstanceAndReturnsId() {
			WorkflowInstance saved = WorkflowInstance.builder()
				.id(42L)
				.workflowType("FAULT_REVIEW")
				.ticketType("FAULT")
				.ticketId(1L)
				.currentStep("OPEN")
				.status(WorkflowStatus.ACTIVE)
				.creatorId("user-1")
				.startedAt(LocalDateTime.now())
				.build();
			when(instanceRepository.save(any())).thenReturn(saved);

			Long id = workflowService.createInstance("FAULT_REVIEW", "FAULT", 1L, "user-1");

			assertThat(id).isEqualTo(42L);
			ArgumentCaptor<WorkflowInstance> cap = ArgumentCaptor.forClass(WorkflowInstance.class);
			verify(instanceRepository).save(cap.capture());
			WorkflowInstance captured = cap.getValue();
			assertThat(captured.getWorkflowType()).isEqualTo("FAULT_REVIEW");
			assertThat(captured.getCurrentStep()).isEqualTo("OPEN");
			assertThat(captured.getStatus()).isEqualTo(WorkflowStatus.ACTIVE);
			assertThat(captured.getCreatorId()).isEqualTo("user-1");
		}

		@Test
		void faultReviewInitialStep_isOPEN() {
			WorkflowInstance saved = WorkflowInstance.builder()
				.id(1L)
				.workflowType("FAULT_REVIEW")
				.currentStep("OPEN")
				.status(WorkflowStatus.ACTIVE)
				.creatorId("u1")
				.ticketType("T")
				.ticketId(1L)
				.startedAt(LocalDateTime.now())
				.build();
			when(instanceRepository.save(any())).thenReturn(saved);

			workflowService.createInstance("FAULT_REVIEW", "T", 1L, "u1");

			ArgumentCaptor<WorkflowInstance> cap = ArgumentCaptor.forClass(WorkflowInstance.class);
			verify(instanceRepository).save(cap.capture());
			assertThat(cap.getValue().getCurrentStep()).isEqualTo("OPEN");
		}

		@Test
		void repairDispatchInitialStep_isPENDING() {
			WorkflowInstance saved = WorkflowInstance.builder()
				.id(2L)
				.workflowType("REPAIR_DISPATCH")
				.currentStep("PENDING")
				.status(WorkflowStatus.ACTIVE)
				.creatorId("u1")
				.ticketType("T")
				.ticketId(2L)
				.startedAt(LocalDateTime.now())
				.build();
			when(instanceRepository.save(any())).thenReturn(saved);

			workflowService.createInstance("REPAIR_DISPATCH", "T", 2L, "u1");

			ArgumentCaptor<WorkflowInstance> cap = ArgumentCaptor.forClass(WorkflowInstance.class);
			verify(instanceRepository).save(cap.capture());
			assertThat(cap.getValue().getCurrentStep()).isEqualTo("PENDING");
		}

		@Test
		void unknownWorkflowType_throwsValidationError() {
			assertThatThrownBy(() -> workflowService.createInstance("UNKNOWN_TYPE", "T", 1L, "u1"))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.VALIDATION_ERROR));
			verify(instanceRepository, never()).save(any());
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// transition()
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class Transition {

		@Test
		void instanceNotFound_throwsNotFound() {
			when(instanceRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(
					() -> workflowService.transition(99L, "REVIEW", "APPROVE", "actor-1", "Actor", null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND));
		}

		@Test
		void instanceAlreadyCompleted_throwsInvalidTransition() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "REVIEW", "creator", "actor-1");
			inst.setStatus(WorkflowStatus.COMPLETED);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));

			assertThatThrownBy(
					() -> workflowService.transition(1L, "CONFIRMED", "APPROVE", "actor-1", "Actor", null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_INVALID_TRANSITION));
		}

		@Test
		void instanceCancelled_throwsInvalidTransition() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", "actor-1");
			inst.setStatus(WorkflowStatus.CANCELLED);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));

			assertThatThrownBy(
					() -> workflowService.transition(1L, "REVIEW", "APPROVE", "actor-1", "Actor", null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_INVALID_TRANSITION));
		}

		@Test
		void invalidStepTransition_throwsInvalidTransition() {
			// OPEN → CONFIRMED is not a valid transition in FAULT_REVIEW
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", "actor-1");
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));

			assertThatThrownBy(
					() -> workflowService.transition(1L, "CONFIRMED", "APPROVE", "actor-1", "Actor", null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_INVALID_TRANSITION));
		}

		@Test
		void selfApproval_throwsSelfApprovalNotAllowed() {
			// creator == actor
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "same-user", "same-user");
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));

			assertThatThrownBy(
					() -> workflowService.transition(1L, "REVIEW", "APPROVE", "same-user", "Same", null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_SELF_APPROVAL_NOT_ALLOWED));
		}

		@Test
		void actorNotAssigneeAndNoDelegation_throwsNotAssigned() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", "assignee-1");
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));
			when(delegateSettingRepository.findActiveByDelegator("assignee-1", LocalDate.now()))
				.thenReturn(Optional.empty());

			assertThatThrownBy(
					() -> workflowService.transition(1L, "REVIEW", "APPROVE", "stranger", "Stranger", null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_NOT_ASSIGNED_TO_USER));
		}

		@Test
		void actorNotAssigneeButDelegationExistsForDifferentDelegate_throwsNotAssigned() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", "assignee-1");
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));

			DelegateSetting ds = DelegateSetting.builder()
				.delegatorId("assignee-1")
				.delegateId("someone-else")
				.startDate(LocalDate.now().minusDays(1))
				.endDate(LocalDate.now().plusDays(1))
				.isActive(true)
				.build();
			when(delegateSettingRepository.findActiveByDelegator("assignee-1", LocalDate.now()))
				.thenReturn(Optional.of(ds));

			assertThatThrownBy(
					() -> workflowService.transition(1L, "REVIEW", "APPROVE", "stranger", "Stranger", null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_NOT_ASSIGNED_TO_USER));
		}

		@Test
		void validTransition_nonTerminalStep_remainsActive() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", null);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));
			when(stepLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			workflowService.transition(1L, "REVIEW", "APPROVE", "actor-1", "Actor", "looks good", null);

			assertThat(inst.getCurrentStep()).isEqualTo("REVIEW");
			assertThat(inst.getStatus()).isEqualTo(WorkflowStatus.ACTIVE);
			assertThat(inst.getCompletedAt()).isNull();
		}

		@Test
		void validTransition_terminalStep_marksCompleted() {
			// REVIEW → CONFIRMED is terminal
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "REVIEW", "creator", null);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));
			when(stepLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			workflowService.transition(1L, "CONFIRMED", "APPROVE", "actor-1", "Actor", null, null);

			assertThat(inst.getCurrentStep()).isEqualTo("CONFIRMED");
			assertThat(inst.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
			assertThat(inst.getCompletedAt()).isNotNull();
		}

		@Test
		void validTransition_savesStepLogWithCorrectFields() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", null);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));
			when(stepLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			workflowService.transition(1L, "REVIEW", "APPROVE", "actor-1", "Actor", "comment", null);

			ArgumentCaptor<WorkflowStepLog> logCap = ArgumentCaptor.forClass(WorkflowStepLog.class);
			verify(stepLogRepository).save(logCap.capture());
			WorkflowStepLog log = logCap.getValue();
			assertThat(log.getInstanceId()).isEqualTo(1L);
			assertThat(log.getStepCode()).isEqualTo("REVIEW");
			assertThat(log.getAction()).isEqualTo("APPROVE");
			assertThat(log.getActorId()).isEqualTo("actor-1");
			assertThat(log.getIsDelegated()).isFalse();
			assertThat(log.getOriginalAssigneeId()).isNull();
			assertThat(log.getComment()).isEqualTo("comment");
		}

		@Test
		void delegatedTransition_savesLogWithDelegateFlag() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", "assignee-1");
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));

			DelegateSetting ds = DelegateSetting.builder()
				.delegatorId("assignee-1")
				.delegateId("delegate-1")
				.startDate(LocalDate.now().minusDays(1))
				.endDate(LocalDate.now().plusDays(1))
				.isActive(true)
				.build();
			when(delegateSettingRepository.findActiveByDelegator("assignee-1", LocalDate.now()))
				.thenReturn(Optional.of(ds));
			when(stepLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			workflowService.transition(1L, "REVIEW", "APPROVE", "delegate-1", "Delegate", "ok", null);

			ArgumentCaptor<WorkflowStepLog> logCap = ArgumentCaptor.forClass(WorkflowStepLog.class);
			verify(stepLogRepository).save(logCap.capture());
			WorkflowStepLog log = logCap.getValue();
			assertThat(log.getIsDelegated()).isTrue();
			assertThat(log.getOriginalAssigneeId()).isEqualTo("assignee-1");
			assertThat(log.getComment()).contains("[代理簽核]");
		}

		@Test
		void validTransition_publishesWorkflowTransitionEvent() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", null);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));
			when(stepLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
			when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			workflowService.transition(1L, "REVIEW", "APPROVE", "actor-1", "Actor", null, null);

			ArgumentCaptor<WorkflowTransitionEvent> evtCap = ArgumentCaptor.forClass(WorkflowTransitionEvent.class);
			verify(eventPublisher).publishEvent(evtCap.capture());
			WorkflowTransitionEvent evt = evtCap.getValue();
			assertThat(evt.getTargetStep()).isEqualTo("REVIEW");
			assertThat(evt.getAction()).isEqualTo("APPROVE");
			assertThat(evt.getInstance()).isEqualTo(inst);
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// getMyPendingTasks()
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class GetMyPendingTasks {

		@Test
		void noDelegations_queriesOnlyUserSelf() {
			when(delegateSettingRepository.findActiveDelegationsForDelegate("user-1", LocalDate.now()))
				.thenReturn(List.of());
			Page<WorkflowInstance> emptyPage = new PageImpl<>(List.of());
			when(instanceRepository.findPendingByAssignees(any(), eq(WorkflowStatus.ACTIVE), any()))
				.thenReturn(emptyPage);

			Page<WorkflowInstanceResponse> result = workflowService.getMyPendingTasks("user-1", PageRequest.of(0, 10));

			assertThat(result.getContent()).isEmpty();
			ArgumentCaptor<java.util.Collection<String>> assigneesCap = ArgumentCaptor
				.forClass(java.util.Collection.class);
			verify(instanceRepository).findPendingByAssignees(assigneesCap.capture(), eq(WorkflowStatus.ACTIVE),
					any(Pageable.class));
			assertThat(assigneesCap.getValue()).containsExactlyInAnyOrder("user-1");
		}

		@Test
		void withActiveDelegations_includesDelegatorIds() {
			DelegateSetting ds1 = DelegateSetting.builder()
				.delegatorId("boss-1")
				.delegateId("user-1")
				.startDate(LocalDate.now().minusDays(1))
				.endDate(LocalDate.now().plusDays(1))
				.isActive(true)
				.build();
			DelegateSetting ds2 = DelegateSetting.builder()
				.delegatorId("boss-2")
				.delegateId("user-1")
				.startDate(LocalDate.now().minusDays(1))
				.endDate(LocalDate.now().plusDays(1))
				.isActive(true)
				.build();
			when(delegateSettingRepository.findActiveDelegationsForDelegate("user-1", LocalDate.now()))
				.thenReturn(List.of(ds1, ds2));
			when(instanceRepository.findPendingByAssignees(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

			workflowService.getMyPendingTasks("user-1", PageRequest.of(0, 10));

			ArgumentCaptor<java.util.Collection<String>> cap = ArgumentCaptor.forClass(java.util.Collection.class);
			verify(instanceRepository).findPendingByAssignees(cap.capture(), any(), any());
			assertThat(cap.getValue()).containsExactlyInAnyOrder("user-1", "boss-1", "boss-2");
		}

		@Test
		void delegatedInstance_setsDelegatedFrom() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", "boss-1");
			DelegateSetting ds = DelegateSetting.builder()
				.delegatorId("boss-1")
				.delegateId("user-1")
				.startDate(LocalDate.now().minusDays(1))
				.endDate(LocalDate.now().plusDays(1))
				.isActive(true)
				.build();
			when(delegateSettingRepository.findActiveDelegationsForDelegate("user-1", LocalDate.now()))
				.thenReturn(List.of(ds));
			when(instanceRepository.findPendingByAssignees(any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(inst)));

			Page<WorkflowInstanceResponse> result = workflowService.getMyPendingTasks("user-1", PageRequest.of(0, 10));

			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).getDelegatedFrom()).isEqualTo("boss-1");
		}

		@Test
		void ownInstance_doesNotSetDelegatedFrom() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", "user-1");
			when(delegateSettingRepository.findActiveDelegationsForDelegate("user-1", LocalDate.now()))
				.thenReturn(List.of());
			when(instanceRepository.findPendingByAssignees(any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(inst)));

			Page<WorkflowInstanceResponse> result = workflowService.getMyPendingTasks("user-1", PageRequest.of(0, 10));

			assertThat(result.getContent().get(0).getDelegatedFrom()).isNull();
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// getStepLogs()
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class GetStepLogs {

		@Test
		void returnsOrderedMappedLogs() {
			WorkflowStepLog log1 = WorkflowStepLog.builder()
				.id(1L)
				.instanceId(10L)
				.stepCode("OPEN")
				.action("CREATE")
				.actorId("u1")
				.actorName("User One")
				.isDelegated(false)
				.actedAt(LocalDateTime.now().minusHours(2))
				.build();
			WorkflowStepLog log2 = WorkflowStepLog.builder()
				.id(2L)
				.instanceId(10L)
				.stepCode("REVIEW")
				.action("APPROVE")
				.actorId("u2")
				.actorName("User Two")
				.isDelegated(false)
				.actedAt(LocalDateTime.now().minusHours(1))
				.build();
			when(stepLogRepository.findByInstanceIdOrderByActedAtAsc(10L)).thenReturn(List.of(log1, log2));

			List<WorkflowStepLogResponse> result = workflowService.getStepLogs(10L);

			assertThat(result).hasSize(2);
			assertThat(result.get(0).getStepCode()).isEqualTo("OPEN");
			assertThat(result.get(0).getActorId()).isEqualTo("u1");
			assertThat(result.get(1).getStepCode()).isEqualTo("REVIEW");
			assertThat(result.get(1).getActorId()).isEqualTo("u2");
		}

		@Test
		void noLogs_returnsEmptyList() {
			when(stepLogRepository.findByInstanceIdOrderByActedAtAsc(99L)).thenReturn(List.of());

			assertThat(workflowService.getStepLogs(99L)).isEmpty();
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// cancel()
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class Cancel {

		@Test
		void instanceNotFound_throwsNotFound() {
			when(instanceRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> workflowService.cancel(99L, "actor-1")).isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND));
		}

		@Test
		void alreadyCompleted_throwsInvalidTransition() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "CONFIRMED", "creator", null);
			inst.setStatus(WorkflowStatus.COMPLETED);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));

			assertThatThrownBy(() -> workflowService.cancel(1L, "actor-1")).isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_INVALID_TRANSITION));
		}

		@Test
		void alreadyCancelled_throwsInvalidTransition() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", null);
			inst.setStatus(WorkflowStatus.CANCELLED);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));

			assertThatThrownBy(() -> workflowService.cancel(1L, "actor-1")).isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_INVALID_TRANSITION));
		}

		@Test
		void validCancel_marksInstanceCancelled() {
			WorkflowInstance inst = activeInstance(1L, "FAULT_REVIEW", "OPEN", "creator", null);
			when(instanceRepository.findById(1L)).thenReturn(Optional.of(inst));
			when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			workflowService.cancel(1L, "creator");

			assertThat(inst.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
			assertThat(inst.getCompletedAt()).isNotNull();
			verify(instanceRepository).save(inst);
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// findByTicket()
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class FindByTicket {

		@Test
		void found_returnsInstance() {
			WorkflowInstance inst = activeInstance(5L, "FAULT_REVIEW", "OPEN", "creator", null);
			inst.setTicketType("FAULT");
			inst.setTicketId(200L);
			when(instanceRepository.findByTicketTypeAndTicketId("FAULT", 200L)).thenReturn(Optional.of(inst));

			WorkflowInstance result = workflowService.findByTicket("FAULT", 200L);

			assertThat(result.getId()).isEqualTo(5L);
		}

		@Test
		void notFound_throwsNotFound() {
			when(instanceRepository.findByTicketTypeAndTicketId("FAULT", 999L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> workflowService.findByTicket("FAULT", 999L)).isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND));
		}

	}

}
