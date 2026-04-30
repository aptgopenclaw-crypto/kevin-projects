package com.taipei.iot.smartiot.service;

import com.taipei.iot.notification.dto.NotificationPayload;
import com.taipei.iot.notification.enums.NotificationRefType;
import com.taipei.iot.notification.enums.NotificationType;
import com.taipei.iot.notification.service.NotificationService;
import com.taipei.iot.smartiot.entity.AlertHistory;
import com.taipei.iot.smartiot.entity.AlertNotificationLog;
import com.taipei.iot.smartiot.entity.EventNotificationChannel;
import com.taipei.iot.smartiot.entity.EventNotificationTarget;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.enums.NotificationChannel;
import com.taipei.iot.smartiot.enums.NotificationTargetType;
import com.taipei.iot.smartiot.repository.AlertNotificationLogRepository;
import com.taipei.iot.smartiot.repository.EventNotificationChannelRepository;
import com.taipei.iot.smartiot.repository.EventNotificationTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 告警通知派送器 (D11: 橋接既有 NotificationService)。
 * <p>
 * 解析 EventNotificationTarget → userIds，
 * 建構 NotificationPayload 委派 NotificationService.send()，
 * 並寫入 AlertNotificationLog。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertDispatcher {

    private final EventNotificationTargetRepository targetRepository;
    private final EventNotificationChannelRepository channelRepository;
    private final AlertNotificationLogRepository notificationLogRepository;
    private final NotificationService notificationService;

    /**
     * 派送告警通知。
     */
    public void dispatch(AlertHistory alert, EventRule rule) {
        List<EventNotificationTarget> targets = targetRepository.findByRuleId(rule.getId());
        List<EventNotificationChannel> channels = channelRepository.findByRuleIdAndEnabledTrue(rule.getId());

        if (targets.isEmpty()) {
            log.info("[AlertDispatcher] No notification targets for rule {}, skipping", rule.getId());
            return;
        }

        // 解析目標 → userIds
        List<String> userIds = resolveUserIds(targets);
        if (userIds.isEmpty()) {
            log.info("[AlertDispatcher] Resolved 0 userIds for rule {}, skipping", rule.getId());
            return;
        }

        // 建構 payload 委派 NotificationService
        NotificationPayload payload = NotificationPayload.builder()
                .tenantId(alert.getTenantId())
                .userIds(userIds)
                .type(NotificationType.ALERT)
                .title(String.format("告警 [%s] — %s", rule.getSeverity(), rule.getRuleName()))
                .content(alert.getMessage())
                .refType(NotificationRefType.ALERT)
                .refId(alert.getId().toString())
                .build();

        try {
            notificationService.send(payload);
            log.info("[AlertDispatcher] Notification sent for alert {} to {} users", alert.getId(), userIds.size());
        } catch (Exception e) {
            log.warn("[AlertDispatcher] NotificationService.send() failed for alert {}: {}",
                    alert.getId(), e.getMessage());
        }

        // 記錄 AlertNotificationLog
        for (EventNotificationChannel ch : channels) {
            for (String userId : userIds) {
                AlertNotificationLog logEntry = AlertNotificationLog.builder()
                        .alertId(alert.getId())
                        .channel(ch.getChannel())
                        .recipient(userId)
                        .status("SENT")
                        .sentAt(LocalDateTime.now())
                        .build();
                notificationLogRepository.save(logEntry);
            }
        }

        // 若無 channels 設定，以 WEBSOCKET 作為預設記錄
        if (channels.isEmpty()) {
            for (String userId : userIds) {
                AlertNotificationLog logEntry = AlertNotificationLog.builder()
                        .alertId(alert.getId())
                        .channel(NotificationChannel.WEBSOCKET)
                        .recipient(userId)
                        .status("SENT")
                        .sentAt(LocalDateTime.now())
                        .build();
                notificationLogRepository.save(logEntry);
            }
        }
    }

    /**
     * 解析通知目標為 userId 清單。
     * <p>
     * USER → targetId 即 userId；
     * ROLE / GROUP → 目前以 targetId 代替，實際生產環境需查 UserRepository。
     * </p>
     */
    private List<String> resolveUserIds(List<EventNotificationTarget> targets) {
        List<String> userIds = new ArrayList<>();
        for (EventNotificationTarget target : targets) {
            if (target.getTargetType() == NotificationTargetType.USER) {
                userIds.add(target.getTargetId());
            } else {
                // ROLE / GROUP: 直接用 targetId 作為佔位，後續可擴充查表
                userIds.add(target.getTargetId());
            }
        }
        return userIds;
    }
}
