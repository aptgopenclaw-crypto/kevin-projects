package com.taipei.iot.audit.async;

import com.taipei.iot.audit.entity.UserEventLogEntity;
import com.taipei.iot.audit.repository.UserEventLogRepository;
import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditAsyncWriter {

    private final UserEventLogRepository userEventLogRepository;
    private final UserRepository userRepository;

    @Async("auditExecutor")
    public void saveAsync(String tenantId, String userId, String username,
                          String eventType, String eventDesc,
                          String apiEndpoint, String payload, String errorCode,
                          String ipAddress, String userAgent, long executionTime,
                          Long deptId) {
        // @Async 在新執行緒執行，ThreadLocal 不繼承；設為 SYSTEM context 讓 TenantFilterAspect 放行
        TenantContext.setSystemContext();
        try {
            // 查詢使用者 displayName 與 email（best-effort）
            String userLabel = null;
            String email = null;
            if (userId != null) {
                UserEntity user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    userLabel = user.getDisplayName();
                    email = user.getEmail();
                }
            }

            UserEventLogEntity entity = new UserEventLogEntity();
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            entity.setUsername(email != null ? email : username);
            entity.setUserLabel(userLabel);
            entity.setEmail(email);
            entity.setEventType(eventType);
            entity.setEventDesc(eventDesc);
            entity.setApiEndpoint(apiEndpoint);
            entity.setPayload(payload);
            entity.setErrorCode(errorCode);
            entity.setIpAddress(ipAddress);
            entity.setUserAgent(userAgent);
            entity.setExecutionTime(executionTime);
            entity.setDeptId(deptId);
            entity.setCreateTime(LocalDateTime.now());

            userEventLogRepository.save(entity);
        } catch (Exception ex) {
            log.error("Audit async write failed: {} {} {}", eventType, apiEndpoint, ex.getMessage());
            // best-effort: do not rethrow, do not affect business logic
        } finally {
            TenantContext.clear();
        }
    }
}
