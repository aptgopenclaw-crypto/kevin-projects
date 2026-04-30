package com.taipei.iot.replacement.listener;

import com.taipei.iot.material.service.IssueService;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplacementNeedMaterialListenerTest {

    @InjectMocks private ReplacementNeedMaterialListener listener;
    @Mock private ReplacementOrderRepository orderRepo;
    @Mock private IssueService issueService;

    @Test
    void onDispatched_createsIssueRequest() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPLACEMENT_REVIEW");
        instance.setTicketId(1L);

        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).orderNumber("RO-001").build();
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "DISPATCHED", "DISPATCH");
        listener.onReplacementDispatched(event);

        verify(issueService).createFromReplacement(1L);
    }

    @Test
    void onDispatched_wrongStep_ignores() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPLACEMENT_REVIEW");
        instance.setTicketId(1L);

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "CLOSED", "APPROVE");
        listener.onReplacementDispatched(event);

        verifyNoInteractions(issueService);
    }

    @Test
    void onDispatched_wrongWorkflowType_ignores() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_DISPATCH");
        instance.setTicketId(1L);

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "DISPATCHED", "DISPATCH");
        listener.onReplacementDispatched(event);

        verifyNoInteractions(orderRepo);
        verifyNoInteractions(issueService);
    }

    @Test
    void onDispatched_orderNotFound_skips() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPLACEMENT_REVIEW");
        instance.setTicketId(99L);

        when(orderRepo.findById(99L)).thenReturn(Optional.empty());

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "DISPATCHED", "DISPATCH");
        listener.onReplacementDispatched(event);

        verify(orderRepo).findById(99L);
        verifyNoInteractions(issueService);
    }
}
