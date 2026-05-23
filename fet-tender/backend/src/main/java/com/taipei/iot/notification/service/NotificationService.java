package com.taipei.iot.notification.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.notification.channel.NotificationChannel;
import com.taipei.iot.notification.dto.NotificationPayload;
import com.taipei.iot.notification.dto.NotificationResponse;
import com.taipei.iot.notification.dto.UnreadCountResponse;
import com.taipei.iot.notification.entity.NotificationEntity;
import com.taipei.iot.notification.enums.NotificationType;
import com.taipei.iot.notification.repository.NotificationRepository;
import com.taipei.iot.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final List<NotificationChannel> channels;

    public void send(NotificationPayload payload) {
        for (NotificationChannel channel : channels) {
            try {
                channel.send(payload);
            } catch (Exception e) {
                log.warn("Channel {} failed: {}", channel.channelType(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(String userId, int page, int size) {
        Page<NotificationEntity> result = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        List<NotificationResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return toPageResponse(items, result, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listTodos(String userId, int page, int size) {
        Page<NotificationEntity> result = notificationRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(userId, NotificationType.TODO, PageRequest.of(page, size));
        List<NotificationResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return toPageResponse(items, result, page, size);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(String userId) {
        long count = notificationRepository.countUnreadByUserId(userId);
        return UnreadCountResponse.builder().count(count).build();
    }

    @Transactional
    public void markRead(String userId, Long notificationId) {
        NotificationEntity entity = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (Boolean.FALSE.equals(entity.getRead())) {
            entity.setRead(true);
            entity.setReadAt(LocalDateTime.now());
            notificationRepository.save(entity);
        }
    }

    @Transactional
    public void markAllRead(String userId) {
        int updated = notificationRepository.markAllReadByUserId(userId);
        log.debug("Marked {} notifications as read for userId={}", updated, userId);
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .refType(entity.getRefType())
                .refId(entity.getRefId())
                .read(entity.getRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PageResponse<NotificationResponse> toPageResponse(
            List<NotificationResponse> items, Page<?> result, int page, int size) {
        return PageResponse.<NotificationResponse>builder()
                .content(items)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }
}
