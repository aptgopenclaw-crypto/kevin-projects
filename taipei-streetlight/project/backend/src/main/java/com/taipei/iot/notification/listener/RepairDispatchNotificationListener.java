package com.taipei.iot.notification.listener;

import com.taipei.iot.notification.dto.NotificationPayload;
import com.taipei.iot.notification.enums.NotificationRefType;
import com.taipei.iot.notification.enums.NotificationType;
import com.taipei.iot.notification.service.NotificationService;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 報修派工 → 通知被指派的外勤人員
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairDispatchNotificationListener {

    private final NotificationService notificationService;
    private final RepairTicketRepository repairTicketRepository;

    @EventListener
    @Order(100)
    public void onRepairDispatched(WorkflowTransitionEvent event) {
        if (!"REPAIR_DISPATCH".equals(event.getInstance().getWorkflowType())) return;
        if (!"DISPATCHED".equals(event.getTargetStep())) return;

        String assignedTo = event.getInstance().getAssignedTo();
        if (assignedTo == null) return;

        Long ticketId = event.getInstance().getTicketId();
        RepairTicket ticket = repairTicketRepository.findById(ticketId).orElse(null);
        String ticketNumber = ticket != null ? ticket.getTicketNumber() : String.valueOf(ticketId);

        NotificationPayload payload = NotificationPayload.builder()
                .tenantId(event.getInstance().getTenantId())
                .userIds(List.of(assignedTo))
                .type(NotificationType.TODO)
                .title("報修派工通知")
                .content(String.format("報修工單 %s 已派工給您，請儘速處理。", ticketNumber))
                .refType(NotificationRefType.REPAIR)
                .refId(String.valueOf(ticketId))
                .build();
        notificationService.send(payload);

        log.info("通知：報修派工 {} → 通知 user={}", ticketNumber, assignedTo);
    }
}
