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
 * 完工回報 → 通知流程發起人（分隊長/建單人）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompletionReportedNotificationListener {

    private final NotificationService notificationService;
    private final RepairTicketRepository repairTicketRepository;

    @EventListener
    @Order(100)
    public void onCompletionReported(WorkflowTransitionEvent event) {
        if (!"REPAIR_CLOSE".equals(event.getInstance().getWorkflowType())) return;
        if (!"PENDING_REVIEW".equals(event.getTargetStep())) return;

        String creatorId = event.getInstance().getCreatorId();
        if (creatorId == null) return;

        Long ticketId = event.getInstance().getTicketId();
        RepairTicket ticket = repairTicketRepository.findById(ticketId).orElse(null);
        String ticketNumber = ticket != null ? ticket.getTicketNumber() : String.valueOf(ticketId);

        NotificationPayload payload = NotificationPayload.builder()
                .tenantId(event.getInstance().getTenantId())
                .userIds(List.of(creatorId))
                .type(NotificationType.TODO)
                .title("完工回報待審核")
                .content(String.format("報修工單 %s 已完工回報，請進行結案審核。", ticketNumber))
                .refType(NotificationRefType.REPAIR)
                .refId(String.valueOf(ticketId))
                .build();
        notificationService.send(payload);

        log.info("通知：完工回報 {} → 通知 user={}", ticketNumber, creatorId);
    }
}
