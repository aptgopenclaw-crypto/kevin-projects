package com.taipei.iot.user.service;

import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.user.entity.UserInfoLogEntity;
import com.taipei.iot.user.repository.UserInfoLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuditService {

    private final UserInfoLogRepository userInfoLogRepository;

    public void logAction(String actionType, String actionUserId, String targetUserId, String detail) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = "SYSTEM";
        }

        UserInfoLogEntity log = UserInfoLogEntity.builder()
                .tenantId(tenantId)
                .actionType(actionType)
                .actionUserId(actionUserId)
                .targetUserId(targetUserId)
                .detail(detail)
                .build();
        userInfoLogRepository.save(log);
    }
}
