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
 * 報修結案 → 通知原建單人
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairClosedNotificationListener {

    private final NotificationService notificationService;
    private final RepairTicketRepository repairTicketRepository;

    @EventListener
    @Order(100)
    public void onRepairClosed(WorkflowTransitionEvent event) {
        if (!"REPAIR_CLOSE".equals(event.getInstance().getWorkflowType())) return;
        if (!"CLOSED".equals(event.getTargetStep())) return;

        Long ticketId = event.getInstance().getTicketId();
        RepairTicket ticket = repairTicketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.getCreatedBy() == null) return;

        String ticketNumber = ticket.getTicketNumber();

        NotificationPayload payload = NotificationPayload.builder()
                .tenantId(event.getInstance().getTenantId())
                .userIds(List.of(ticket.getCreatedBy()))
                .type(NotificationType.INFO)
                .title("報修工單已結案")
                .content(String.format("報修工單 %s 已結案完成。", ticketNumber))
                .refType(NotificationRefType.REPAIR)
                .refId(String.valueOf(ticketId))
                .build();
        notificationService.send(payload);

        log.info("通知：報修結案 {} → 通知 user={}", ticketNumber, ticket.getCreatedBy());
    }
}
