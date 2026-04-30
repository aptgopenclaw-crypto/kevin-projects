package com.taipei.iot.announcement.service;

import com.taipei.iot.announcement.dto.UnreadCountResponse;
import com.taipei.iot.announcement.repository.AnnouncementReadRepository;
import com.taipei.iot.announcement.repository.AnnouncementRepository;
import com.taipei.iot.common.dto.UserInfo;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnnouncementReadService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        UserInfo user = SecurityContextUtils.getUserInfo();
        Long deptId = user.getDeptId() != null ? user.getDeptId() : -1L;
        long count = announcementRepository.countUnread(deptId, user.getUserId(), LocalDateTime.now());
        return UnreadCountResponse.builder().count((int) count).build();
    }

    @Transactional
    public void markAsRead(Long announcementId) {
        String userId = SecurityContextUtils.getCurrentUserId();
        announcementReadRepository.markAsRead(announcementId, userId);
    }

    @Transactional
    public void markAllAsRead() {
        UserInfo user = SecurityContextUtils.getUserInfo();
        String tenantId = TenantContext.getCurrentTenantId();
        Long deptId = user.getDeptId() != null ? user.getDeptId() : -1L;
        announcementReadRepository.markAllAsRead(user.getUserId(), tenantId, deptId);
    }
}
