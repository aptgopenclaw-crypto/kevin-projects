package com.taipei.iot.audit.job;

import com.taipei.iot.audit.repository.UserEventLogRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditPurgeJob {

    private final UserEventLogRepository userEventLogRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void purgeOldAuditLogs() {
        // UserEventLogRepository 實作 TenantScopedRepository；排程任務跨租戶操作需設定 SYSTEM context
        TenantContext.setSystemContext();
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            int deleted = userEventLogRepository.deleteByCreateTimeBefore(cutoff);
            log.info("AuditPurgeJob: deleted {} audit records older than {}", deleted, cutoff);
        } finally {
            TenantContext.clear();
        }
    }
}
