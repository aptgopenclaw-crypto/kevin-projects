package com.taipei.iot.notification.listener;

import com.taipei.iot.notification.dto.NotificationPayload;
import com.taipei.iot.notification.enums.NotificationRefType;
import com.taipei.iot.notification.enums.NotificationType;
import com.taipei.iot.notification.service.NotificationService;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 換裝派工 → 通知被指派人員
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplacementDispatchNotificationListener {

    private final NotificationService notificationService;
    private final ReplacementOrderRepository orderRepository;

    @EventListener
    @Order(100)
    public void onReplacementDispatched(WorkflowTransitionEvent event) {
        if (!"REPLACEMENT_REVIEW".equals(event.getInstance().getWorkflowType())) return;
        if (!"DISPATCHED".equals(event.getTargetStep())) return;

        String assignedTo = event.getInstance().getAssignedTo();
        if (assignedTo == null) return;

        Long orderId = event.getInstance().getTicketId();
        ReplacementOrder order = orderRepository.findById(orderId).orElse(null);
        String orderNumber = order != null ? order.getOrderNumber() : String.valueOf(orderId);

        NotificationPayload payload = NotificationPayload.builder()
                .tenantId(event.getInstance().getTenantId())
                .userIds(List.of(assignedTo))
                .type(NotificationType.TODO)
                .title("換裝派工通知")
                .content(String.format("換裝派工單 %s 已派工給您，請儘速處理。", orderNumber))
                .refType(NotificationRefType.REPLACEMENT)
                .refId(String.valueOf(orderId))
                .build();
        notificationService.send(payload);

        log.info("通知：換裝派工 {} → 通知 user={}", orderNumber, assignedTo);
    }
}
