package com.taipei.iot.notification.listener;

import com.taipei.iot.auth.entity.UserTenantMappingEntity;
import com.taipei.iot.auth.repository.UserTenantMappingRepository;
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
 * 換裝派工（觸發領料需求建立）→ 通知倉管相關人員（ADMIN + OPERATOR）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueRequestedNotificationListener {

    private final NotificationService notificationService;
    private final ReplacementOrderRepository orderRepository;
    private final UserTenantMappingRepository userTenantMappingRepository;

    @EventListener
    @Order(200) // after ReplacementNeedMaterialListener creates the issue
    public void onIssueRequested(WorkflowTransitionEvent event) {
        if (!"REPLACEMENT_REVIEW".equals(event.getInstance().getWorkflowType())) return;
        if (!"DISPATCHED".equals(event.getTargetStep())) return;

        String tenantId = event.getInstance().getTenantId();
        Long orderId = event.getInstance().getTicketId();

        ReplacementOrder order = orderRepository.findById(orderId).orElse(null);
        String orderNumber = order != null ? order.getOrderNumber() : String.valueOf(orderId);

        // Find ADMIN + OPERATOR users in the tenant for warehouse notification
        List<String> adminUserIds = userTenantMappingRepository
                .findByTenantIdAndRoleIdAndEnabledTrue(tenantId, "ROLE_ADMIN")
                .stream().map(UserTenantMappingEntity::getUserId).toList();
        List<String> operatorUserIds = userTenantMappingRepository
                .findByTenantIdAndRoleIdAndEnabledTrue(tenantId, "ROLE_OPERATOR")
                .stream().map(UserTenantMappingEntity::getUserId).toList();

        List<String> recipientIds = new java.util.ArrayList<>(adminUserIds);
        for (String uid : operatorUserIds) {
            if (!recipientIds.contains(uid)) recipientIds.add(uid);
        }

        if (recipientIds.isEmpty()) return;

        NotificationPayload payload = NotificationPayload.builder()
                .tenantId(tenantId)
                .userIds(recipientIds)
                .type(NotificationType.TODO)
                .title("領料需求待處理")
                .content(String.format("換裝派工單 %s 已產生領料需求，請進行材料出庫作業。", orderNumber))
                .refType(NotificationRefType.MATERIAL)
                .refId(String.valueOf(orderId))
                .build();
        notificationService.send(payload);

        log.info("通知：領料需求 {} → 通知 {} 位倉管人員", orderNumber, recipientIds.size());
    }
}
