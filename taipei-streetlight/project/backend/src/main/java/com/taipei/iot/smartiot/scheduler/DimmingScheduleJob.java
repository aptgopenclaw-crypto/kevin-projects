package com.taipei.iot.smartiot.scheduler;

import com.taipei.iot.smartiot.dto.InstantDimRequest;
import com.taipei.iot.smartiot.entity.DimmingGroup;
import com.taipei.iot.smartiot.entity.DimmingSchedule;
import com.taipei.iot.smartiot.enums.DimmingTargetType;
import com.taipei.iot.smartiot.repository.DimmingGroupRepository;
import com.taipei.iot.smartiot.repository.DimmingScheduleRepository;
import com.taipei.iot.smartiot.service.DimmingService;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 調光排程執行器 (FN-07-026, D24)。
 * <p>
 * 每 60 秒掃描 enabled=true 的排程：
 * <ul>
 *   <li>cron 排程: 判斷上一分鐘是否匹配 cron</li>
 *   <li>單次排程: oneTimeAt <= now → 執行後停用</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DimmingScheduleJob {

    private final DimmingScheduleRepository scheduleRepository;
    private final DimmingGroupRepository groupRepository;
    private final DimmingService dimmingService;

    @Scheduled(fixedRate = 60000)
    public void execute() {
        List<DimmingSchedule> schedules;
        try {
            TenantContext.setSystemContext();
            schedules = scheduleRepository.findByEnabledTrue();
        } finally {
            TenantContext.clear();
        }
        if (schedules.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (DimmingSchedule schedule : schedules) {
            try {
                TenantContext.setCurrentTenantId(schedule.getTenantId());

                if (shouldExecute(schedule, now)) {
                    executeSchedule(schedule);

                    // 單次排程執行後停用
                    if (schedule.getOneTimeAt() != null) {
                        schedule.setEnabled(false);
                        scheduleRepository.save(schedule);
                        log.info("[DimmingSchedule] One-time schedule {} disabled after execution",
                                schedule.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("[DimmingSchedule] Failed to execute schedule {}: {}",
                        schedule.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private boolean shouldExecute(DimmingSchedule schedule, LocalDateTime now) {
        // 單次排程
        if (schedule.getOneTimeAt() != null) {
            return !schedule.getOneTimeAt().isAfter(now);
        }

        // Cron 排程: 檢查當前分鐘是否匹配
        if (schedule.getScheduleCron() != null && !schedule.getScheduleCron().isBlank()) {
            try {
                CronExpression cron = CronExpression.parse(schedule.getScheduleCron());
                LocalDateTime windowStart = now.withSecond(0).withNano(0);
                LocalDateTime windowEnd = windowStart.plusMinutes(1);
                LocalDateTime next = cron.next(windowStart.minusSeconds(1));
                return next != null && !next.isAfter(windowEnd);
            } catch (IllegalArgumentException e) {
                log.warn("[DimmingSchedule] Invalid cron '{}' for schedule {}",
                        schedule.getScheduleCron(), schedule.getId());
                return false;
            }
        }

        return false;
    }

    private void executeSchedule(DimmingSchedule schedule) {
        if (schedule.getTargetType() == DimmingTargetType.DEVICE) {
            // 單燈
            InstantDimRequest req = InstantDimRequest.builder()
                    .deviceId(schedule.getTargetId())
                    .brightness(schedule.getBrightnessPct())
                    .build();
            dimmingService.instantDim(req);
            log.info("[DimmingSchedule] Executed for device={}, brightness={}%",
                    schedule.getTargetId(), schedule.getBrightnessPct());
        } else if (schedule.getTargetType() == DimmingTargetType.GROUP) {
            // 群組 (D23: 逐燈循序)
            DimmingGroup group = groupRepository.findById(schedule.getTargetId()).orElse(null);
            if (group == null) {
                log.warn("[DimmingSchedule] Group {} not found for schedule {}",
                        schedule.getTargetId(), schedule.getId());
                return;
            }
            for (Long deviceId : group.getDeviceIds()) {
                InstantDimRequest req = InstantDimRequest.builder()
                        .deviceId(deviceId)
                        .brightness(schedule.getBrightnessPct())
                        .build();
                dimmingService.instantDim(req);
            }
            log.info("[DimmingSchedule] Executed for group={}, devices={}, brightness={}%",
                    schedule.getTargetId(), group.getDeviceIds().length, schedule.getBrightnessPct());
        }
    }
}
