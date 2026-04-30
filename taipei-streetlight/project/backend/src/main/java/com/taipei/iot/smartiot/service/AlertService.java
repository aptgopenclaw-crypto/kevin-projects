package com.taipei.iot.smartiot.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.fault.dto.FaultTicketRequest;
import com.taipei.iot.fault.dto.FaultTicketResponse;
import com.taipei.iot.fault.enums.FaultTicketSource;
import com.taipei.iot.fault.service.FaultTicketService;
import com.taipei.iot.smartiot.dto.AlertHistoryResponse;
import com.taipei.iot.smartiot.entity.AlertHistory;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.AlertStatus;
import com.taipei.iot.smartiot.event.AlertTriggeredEvent;
import com.taipei.iot.smartiot.repository.AlertHistoryRepository;
import com.taipei.iot.smartiot.repository.AlertNotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 告警管理服務 (FN-07-015 / FN-07-016)。
 * <p>
 * 建立告警、確認 (ACK)、解除 (RESOLVE)、查詢列表。
 * D12: autoCreateTicket 時呼叫 FaultTicketService.create()。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertNotificationLogRepository notificationLogRepository;
    private final FaultTicketService faultTicketService;
    private final AlertDispatcher alertDispatcher;

    /**
     * 建立告警紀錄 (由 AlertSuppressionEngine 呼叫)。
     */
    @Transactional
    public AlertHistory createAlert(AlertTriggeredEvent event, EventRule rule, String message) {
        AlertHistory alert = AlertHistory.builder()
                .ruleId(event.getRuleId())
                .deviceId(event.getDeviceId())
                .severity(event.getSeverity())
                .status(AlertStatus.OPEN)
                .message(message)
                .triggeredValues(event.getTriggeredValues())
                .triggeredAt(LocalDateTime.now())
                .notificationSent(false)
                .build();

        alert = alertHistoryRepository.save(alert);

        // D12: autoCreateTicket → 呼叫 FaultTicketService
        if (Boolean.TRUE.equals(rule.getAutoCreateTicket())) {
            try {
                FaultTicketRequest ticketReq = FaultTicketRequest.builder()
                        .deviceId(event.getDeviceId())
                        .source(FaultTicketSource.AUTO_ALERT)
                        .priority(mapSeverityToPriority(event.getSeverity()))
                        .description(message)
                        .build();
                FaultTicketResponse ticket = faultTicketService.create(ticketReq);
                alert.setFaultTicketId(ticket.getId());
                alert = alertHistoryRepository.save(alert);
                log.info("[AlertService] Auto-created fault ticket {} for alert {}", ticket.getId(), alert.getId());
            } catch (Exception e) {
                log.warn("[AlertService] Failed to auto-create fault ticket for alert {}: {}",
                        alert.getId(), e.getMessage());
            }
        }

        // 派送通知
        try {
            alertDispatcher.dispatch(alert, rule);
            alert.setNotificationSent(true);
            alertHistoryRepository.save(alert);
        } catch (Exception e) {
            log.warn("[AlertService] Notification dispatch failed for alert {}: {}", alert.getId(), e.getMessage());
        }

        return alert;
    }

    /**
     * 告警列表 — 依 status + severity 篩選。
     */
    public Page<AlertHistoryResponse> list(AlertStatus status, AlertSeverity severity, Pageable pageable) {
        return alertHistoryRepository.findByFilters(status, severity, pageable)
                .map(this::toResponse);
    }

    /**
     * 確認告警 (OPEN → ACKNOWLEDGED)。
     */
    @Transactional
    public AlertHistoryResponse acknowledge(Long id) {
        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IOT_ALERT_NOT_FOUND));

        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new BusinessException(ErrorCode.IOT_ALERT_INVALID_STATUS);
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAckBy(SecurityContextUtils.getCurrentUserId());
        alert.setAckAt(LocalDateTime.now());
        alert = alertHistoryRepository.save(alert);

        return toResponse(alert);
    }

    /**
     * 解除告警 (ACKNOWLEDGED → RESOLVED) 含 MTTR 計算。
     */
    @Transactional
    public AlertHistoryResponse resolve(Long id) {
        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IOT_ALERT_NOT_FOUND));

        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new BusinessException(ErrorCode.IOT_ALERT_INVALID_STATUS);
        }

        LocalDateTime now = LocalDateTime.now();
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(now);
        alert.setMttrMinutes((int) ChronoUnit.MINUTES.between(alert.getTriggeredAt(), now));
        alert = alertHistoryRepository.save(alert);

        return toResponse(alert);
    }

    private String mapSeverityToPriority(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "HIGH";
            case WARNING -> "NORMAL";
            case INFO -> "LOW";
        };
    }

    AlertHistoryResponse toResponse(AlertHistory a) {
        return AlertHistoryResponse.builder()
                .id(a.getId())
                .tenantId(a.getTenantId())
                .ruleId(a.getRuleId())
                .deviceId(a.getDeviceId())
                .severity(a.getSeverity())
                .status(a.getStatus())
                .message(a.getMessage())
                .triggeredValues(a.getTriggeredValues())
                .triggeredAt(a.getTriggeredAt())
                .ackBy(a.getAckBy())
                .ackAt(a.getAckAt())
                .resolvedAt(a.getResolvedAt())
                .mttrMinutes(a.getMttrMinutes())
                .faultTicketId(a.getFaultTicketId())
                .notificationSent(a.getNotificationSent())
                .build();
    }
}
