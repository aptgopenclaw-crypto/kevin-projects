package com.taipei.iot.workflow.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.workflow.dto.WorkflowInstanceResponse;
import com.taipei.iot.workflow.entity.DelegateSetting;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.entity.WorkflowStepLog;
import com.taipei.iot.workflow.enums.WorkflowStatus;
import com.taipei.iot.workflow.repository.DelegateSettingRepository;
import com.taipei.iot.workflow.repository.WorkflowInstanceRepository;
import com.taipei.iot.workflow.repository.WorkflowStepLogRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @InjectMocks private WorkflowServiceImpl workflowService;
    @Mock private WorkflowInstanceRepository instanceRepository;
    @Mock private WorkflowStepLogRepository stepLogRepository;
    @Mock private DelegateSettingRepository delegateSettingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private WorkflowInstance faultReviewInstance;

    @BeforeEach
    void setUp() {
        faultReviewInstance = WorkflowInstance.builder()
                .id(1L).workflowType("FAULT_REVIEW").ticketType("FAULT_TICKET").ticketId(100L)
                .currentStep("OPEN").status(WorkflowStatus.ACTIVE)
                .creatorId("creator-001").assignedTo("reviewer-001")
                .startedAt(LocalDateTime.now()).build();
    }

    // ── createInstance ──

    @Test
    void createInstance_faultReview_setsInitialStepOPEN() {
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> {
            WorkflowInstance i = inv.getArgument(0);
            i.setId(10L);
            return i;
        });

        Long id = workflowService.createInstance("FAULT_REVIEW", "FAULT_TICKET", 1L, "user-001");

        assertNotNull(id);
        ArgumentCaptor<WorkflowInstance> captor = ArgumentCaptor.forClass(WorkflowInstance.class);
        verify(instanceRepository).save(captor.capture());
        assertEquals("OPEN", captor.getValue().getCurrentStep());
        assertEquals(WorkflowStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void createInstance_unknownType_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> workflowService.createInstance("UNKNOWN", "X", 1L, "u"));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    // ── FSM transition ──

    @Test
    void transition_OPEN_to_REVIEW_legal() {
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));
        when(stepLogRepository.save(any())).thenReturn(new WorkflowStepLog());
        when(instanceRepository.save(any())).thenReturn(faultReviewInstance);

        assertDoesNotThrow(() -> workflowService.transition(
                1L, "REVIEW", "SUBMIT", "reviewer-001", "Reviewer", "ok", null));
    }

    @Test
    void transition_OPEN_to_CONFIRMED_illegal() {
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workflowService.transition(1L, "CONFIRMED", "APPROVE", "reviewer-001", "R", null, null));
        assertEquals(ErrorCode.WORKFLOW_INVALID_TRANSITION, ex.getErrorCode());
    }

    @Test
    void transition_REVIEW_to_CONFIRMED_setsCompleted() {
        faultReviewInstance.setCurrentStep("REVIEW");
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));
        when(stepLogRepository.save(any())).thenReturn(new WorkflowStepLog());
        when(instanceRepository.save(any())).thenReturn(faultReviewInstance);

        workflowService.transition(1L, "CONFIRMED", "APPROVE", "reviewer-001", "R", null, null);

        assertEquals(WorkflowStatus.COMPLETED, faultReviewInstance.getStatus());
        assertNotNull(faultReviewInstance.getCompletedAt());
    }

    @Test
    void transition_repairDispatch_PENDING_to_ACCEPTED() {
        WorkflowInstance rd = WorkflowInstance.builder()
                .id(2L).workflowType("REPAIR_DISPATCH").currentStep("PENDING")
                .status(WorkflowStatus.ACTIVE).creatorId("creator-002").assignedTo("operator-001")
                .startedAt(LocalDateTime.now()).build();
        when(instanceRepository.findById(2L)).thenReturn(Optional.of(rd));
        when(stepLogRepository.save(any())).thenReturn(new WorkflowStepLog());
        when(instanceRepository.save(any())).thenReturn(rd);

        workflowService.transition(2L, "ACCEPTED", "ACCEPT", "operator-001", "Op", null, null);
        assertEquals("ACCEPTED", rd.getCurrentStep());
    }

    @Test
    void transition_assetChange_DRAFT_to_PENDING_REVIEW() {
        WorkflowInstance ac = WorkflowInstance.builder()
                .id(3L).workflowType("ASSET_CHANGE").currentStep("DRAFT")
                .status(WorkflowStatus.ACTIVE).creatorId("user-A").assignedTo("admin-A")
                .startedAt(LocalDateTime.now()).build();
        when(instanceRepository.findById(3L)).thenReturn(Optional.of(ac));
        when(stepLogRepository.save(any())).thenReturn(new WorkflowStepLog());
        when(instanceRepository.save(any())).thenReturn(ac);

        workflowService.transition(3L, "PENDING_REVIEW", "SUBMIT", "admin-A", "Admin", null, null);
        assertEquals("PENDING_REVIEW", ac.getCurrentStep());
    }

    @Test
    void transition_completedInstance_throws() {
        faultReviewInstance.setStatus(WorkflowStatus.COMPLETED);
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workflowService.transition(1L, "REVIEW", "X", "a", "A", null, null));
        assertEquals(ErrorCode.WORKFLOW_INVALID_TRANSITION, ex.getErrorCode());
    }

    // ── Self-approval prevention ──

    @Test
    void transition_selfApproval_throws() {
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workflowService.transition(1L, "REVIEW", "SUBMIT",
                        "creator-001", "Creator", null, null));
        assertEquals(ErrorCode.WORKFLOW_SELF_APPROVAL_NOT_ALLOWED, ex.getErrorCode());
    }

    // ── Delegation ──

    @Test
    void transition_validDelegate_succeeds() {
        DelegateSetting ds = DelegateSetting.builder()
                .delegatorId("reviewer-001").delegateId("delegate-001")
                .startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(1))
                .isActive(true).build();

        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));
        when(delegateSettingRepository.findActiveByDelegator("reviewer-001", LocalDate.now()))
                .thenReturn(Optional.of(ds));
        when(stepLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(instanceRepository.save(any())).thenReturn(faultReviewInstance);

        workflowService.transition(1L, "REVIEW", "SUBMIT", "delegate-001", "Delegate", "代理", null);

        ArgumentCaptor<WorkflowStepLog> logCaptor = ArgumentCaptor.forClass(WorkflowStepLog.class);
        verify(stepLogRepository).save(logCaptor.capture());
        assertTrue(logCaptor.getValue().getIsDelegated());
        assertEquals("reviewer-001", logCaptor.getValue().getOriginalAssigneeId());
    }

    @Test
    void transition_invalidDelegate_throws() {
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));
        when(delegateSettingRepository.findActiveByDelegator("reviewer-001", LocalDate.now()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workflowService.transition(1L, "REVIEW", "SUBMIT",
                        "random-user", "Random", null, null));
        assertEquals(ErrorCode.WORKFLOW_NOT_ASSIGNED_TO_USER, ex.getErrorCode());
    }

    // ── Pending tasks with delegation ──

    @Test
    void getMyPendingTasks_includesDelegatedTasks() {
        DelegateSetting ds = DelegateSetting.builder()
                .delegatorId("boss-001").delegateId("user-001").build();

        when(delegateSettingRepository.findActiveDelegationsForDelegate("user-001", LocalDate.now()))
                .thenReturn(List.of(ds));

        WorkflowInstance delegated = WorkflowInstance.builder()
                .id(5L).workflowType("FAULT_REVIEW").currentStep("REVIEW")
                .status(WorkflowStatus.ACTIVE).assignedTo("boss-001").creatorId("someone")
                .startedAt(LocalDateTime.now()).build();

        when(instanceRepository.findPendingByAssignees(eq(Set.of("user-001", "boss-001")),
                eq(WorkflowStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(delegated)));

        Page<WorkflowInstanceResponse> result =
                workflowService.getMyPendingTasks("user-001", PageRequest.of(0, 20));

        assertEquals(1, result.getContent().size());
        assertEquals("boss-001", result.getContent().get(0).getDelegatedFrom());
    }

    // ── Cancel ──

    @Test
    void cancel_setsStatusCancelled() {
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));
        when(instanceRepository.save(any())).thenReturn(faultReviewInstance);

        workflowService.cancel(1L, "user-001");

        assertEquals(WorkflowStatus.CANCELLED, faultReviewInstance.getStatus());
    }

    @Test
    void cancel_alreadyCompleted_throws() {
        faultReviewInstance.setStatus(WorkflowStatus.COMPLETED);
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(faultReviewInstance));

        assertThrows(BusinessException.class, () -> workflowService.cancel(1L, "u"));
    }
}
