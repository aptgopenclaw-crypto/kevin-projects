package com.taipei.iot.smartiot.scheduler;

import com.taipei.iot.smartiot.entity.DimmingLog;
import com.taipei.iot.smartiot.enums.DimmingResult;
import com.taipei.iot.smartiot.repository.DimmingLogRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 調光指令 Timeout 標記 (D25)。
 * <p>
 * PENDING 超過 30 秒 → 標記 TIMEOUT。
 * 7g 再加完整重試 + fail-safe。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DimmingTimeoutJob {

    private static final int TIMEOUT_SECONDS = 30;

    private final DimmingLogRepository logRepository;

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void markTimeout() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(TIMEOUT_SECONDS);
        List<DimmingLog> pendingLogs;
        try {
            TenantContext.setSystemContext();
            pendingLogs = logRepository.findPendingBefore(DimmingResult.PENDING, cutoff);
        } finally {
            TenantContext.clear();
        }

        if (pendingLogs.isEmpty()) {
            return;
        }

        for (DimmingLog dimmingLog : pendingLogs) {
            try {
                TenantContext.setCurrentTenantId(dimmingLog.getTenantId());
                dimmingLog.setResult(DimmingResult.TIMEOUT);
                logRepository.save(dimmingLog);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("[DimmingTimeout] Marked {} logs as TIMEOUT", pendingLogs.size());
    }
}
