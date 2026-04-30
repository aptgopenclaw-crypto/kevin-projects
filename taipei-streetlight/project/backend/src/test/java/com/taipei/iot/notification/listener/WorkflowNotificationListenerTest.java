package com.taipei.iot.notification.listener;

import com.taipei.iot.notification.dto.NotificationPayload;
import com.taipei.iot.notification.enums.NotificationRefType;
import com.taipei.iot.notification.enums.NotificationType;
import com.taipei.iot.notification.service.NotificationService;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
import com.taipei.iot.auth.entity.UserTenantMappingEntity;
import com.taipei.iot.auth.repository.UserTenantMappingRepository;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowNotificationListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private RepairTicketRepository repairTicketRepository;
    @Mock private ReplacementOrderRepository replacementOrderRepository;
    @Mock private UserTenantMappingRepository userTenantMappingRepository;

    @InjectMocks private RepairDispatchNotificationListener repairDispatchListener;
    @InjectMocks private CompletionReportedNotificationListener completionReportedListener;
    @InjectMocks private RepairClosedNotificationListener repairClosedListener;
    @InjectMocks private ReplacementDispatchNotificationListener replacementDispatchListener;
    @InjectMocks private IssueRequestedNotificationListener issueRequestedListener;

    // ---- Helper ----

    private WorkflowInstance buildInstance(String workflowType, Long ticketId,
                                           String assignedTo, String creatorId, String tenantId) {
        WorkflowInstance inst = new WorkflowInstance();
        inst.setWorkflowType(workflowType);
        inst.setTicketId(ticketId);
        inst.setAssignedTo(assignedTo);
        inst.setCreatorId(creatorId);
        inst.setTenantId(tenantId);
        return inst;
    }

    private WorkflowTransitionEvent buildEvent(WorkflowInstance instance, String targetStep) {
        return new WorkflowTransitionEvent(this, instance, targetStep, "APPROVE");
    }

    // ---- TC-00-014: Repair Dispatch Notification ----

    @Test
    void repairDispatch_notifiesAssignee() {
        WorkflowInstance inst = buildInstance("REPAIR_DISPATCH", 10L, "field-user-1", "admin-1", "T1");
        RepairTicket ticket = new RepairTicket();
        ticket.setTicketNumber("RPR-2026-00010");
        when(repairTicketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        repairDispatchListener.onRepairDispatched(buildEvent(inst, "DISPATCHED"));

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).send(captor.capture());

        NotificationPayload payload = captor.getValue();
        assertEquals(List.of("field-user-1"), payload.getUserIds());
        assertEquals(NotificationType.TODO, payload.getType());
        assertEquals(NotificationRefType.REPAIR, payload.getRefType());
        assertEquals("10", payload.getRefId());
        assertTrue(payload.getContent().contains("RPR-2026-00010"));
    }

    @Test
    void repairDispatch_ignoresWrongWorkflowType() {
        WorkflowInstance inst = buildInstance("REPLACEMENT_REVIEW", 10L, "user-1", null, "T1");
        repairDispatchListener.onRepairDispatched(buildEvent(inst, "DISPATCHED"));
        verify(notificationService, never()).send(any());
    }

    // ---- Completion Reported Notification ----

    @Test
    void completionReported_notifiesCreator() {
        WorkflowInstance inst = buildInstance("REPAIR_CLOSE", 20L, null, "squad-leader-1", "T1");
        RepairTicket ticket = new RepairTicket();
        ticket.setTicketNumber("RPR-2026-00020");
        when(repairTicketRepository.findById(20L)).thenReturn(Optional.of(ticket));

        completionReportedListener.onCompletionReported(buildEvent(inst, "PENDING_REVIEW"));

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).send(captor.capture());

        NotificationPayload payload = captor.getValue();
        assertEquals(List.of("squad-leader-1"), payload.getUserIds());
        assertEquals(NotificationType.TODO, payload.getType());
    }

    // ---- Repair Closed Notification ----

    @Test
    void repairClosed_notifiesTicketCreator() {
        WorkflowInstance inst = buildInstance("REPAIR_CLOSE", 30L, null, null, "T1");
        RepairTicket ticket = new RepairTicket();
        ticket.setTicketNumber("RPR-2026-00030");
        ticket.setCreatedBy("original-creator");
        when(repairTicketRepository.findById(30L)).thenReturn(Optional.of(ticket));

        repairClosedListener.onRepairClosed(buildEvent(inst, "CLOSED"));

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).send(captor.capture());

        NotificationPayload payload = captor.getValue();
        assertEquals(List.of("original-creator"), payload.getUserIds());
        assertEquals(NotificationType.INFO, payload.getType());
    }

    // ---- Replacement Dispatch Notification ----

    @Test
    void replacementDispatch_notifiesAssignee() {
        WorkflowInstance inst = buildInstance("REPLACEMENT_REVIEW", 40L, "contractor-1", null, "T1");
        ReplacementOrder order = new ReplacementOrder();
        order.setOrderNumber("RPL-2026-00040");
        when(replacementOrderRepository.findById(40L)).thenReturn(Optional.of(order));

        replacementDispatchListener.onReplacementDispatched(buildEvent(inst, "DISPATCHED"));

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).send(captor.capture());

        NotificationPayload payload = captor.getValue();
        assertEquals(List.of("contractor-1"), payload.getUserIds());
        assertEquals(NotificationType.TODO, payload.getType());
        assertEquals(NotificationRefType.REPLACEMENT, payload.getRefType());
    }

    // ---- Issue Requested Notification ----

    @Test
    void issueRequested_notifiesAdminAndOperator() {
        WorkflowInstance inst = buildInstance("REPLACEMENT_REVIEW", 50L, "user-1", null, "T1");
        ReplacementOrder order = new ReplacementOrder();
        order.setOrderNumber("RPL-2026-00050");
        when(replacementOrderRepository.findById(50L)).thenReturn(Optional.of(order));

        UserTenantMappingEntity adminMapping = new UserTenantMappingEntity();
        adminMapping.setUserId("admin-1");
        UserTenantMappingEntity opMapping = new UserTenantMappingEntity();
        opMapping.setUserId("operator-1");

        when(userTenantMappingRepository.findByTenantIdAndRoleIdAndEnabledTrue("T1", "ROLE_ADMIN"))
                .thenReturn(List.of(adminMapping));
        when(userTenantMappingRepository.findByTenantIdAndRoleIdAndEnabledTrue("T1", "ROLE_OPERATOR"))
                .thenReturn(List.of(opMapping));

        issueRequestedListener.onIssueRequested(buildEvent(inst, "DISPATCHED"));

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).send(captor.capture());

        NotificationPayload payload = captor.getValue();
        assertEquals(2, payload.getUserIds().size());
        assertTrue(payload.getUserIds().contains("admin-1"));
        assertTrue(payload.getUserIds().contains("operator-1"));
        assertEquals(NotificationRefType.MATERIAL, payload.getRefType());
    }

    @Test
    void issueRequested_noRecipients_doesNotSend() {
        WorkflowInstance inst = buildInstance("REPLACEMENT_REVIEW", 50L, "user-1", null, "T1");
        when(replacementOrderRepository.findById(50L)).thenReturn(Optional.empty());
        when(userTenantMappingRepository.findByTenantIdAndRoleIdAndEnabledTrue("T1", "ROLE_ADMIN"))
                .thenReturn(List.of());
        when(userTenantMappingRepository.findByTenantIdAndRoleIdAndEnabledTrue("T1", "ROLE_OPERATOR"))
                .thenReturn(List.of());

        issueRequestedListener.onIssueRequested(buildEvent(inst, "DISPATCHED"));

        verify(notificationService, never()).send(any());
    }
}
