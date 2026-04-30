package com.taipei.iot.smartiot.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.smartiot.dto.DimmingGroupRequest;
import com.taipei.iot.smartiot.dto.DimmingGroupResponse;
import com.taipei.iot.smartiot.dto.DimmingLogResponse;
import com.taipei.iot.smartiot.dto.DimmingScheduleRequest;
import com.taipei.iot.smartiot.dto.DimmingScheduleResponse;
import com.taipei.iot.smartiot.dto.GroupDimRequest;
import com.taipei.iot.smartiot.dto.InstantDimRequest;
import com.taipei.iot.smartiot.entity.DimmingGroup;
import com.taipei.iot.smartiot.entity.DimmingLog;
import com.taipei.iot.smartiot.entity.DimmingSchedule;
import com.taipei.iot.smartiot.enums.DimmingCommandType;
import com.taipei.iot.smartiot.enums.DimmingResult;
import com.taipei.iot.smartiot.mqtt.MqttCommandPublisher;
import com.taipei.iot.smartiot.repository.DimmingGroupRepository;
import com.taipei.iot.smartiot.repository.DimmingLogRepository;
import com.taipei.iot.smartiot.repository.DimmingScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 調光控制服務 (FN-07-023~029, D21~D27)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DimmingService {

    private final DimmingGroupRepository groupRepository;
    private final DimmingScheduleRepository scheduleRepository;
    private final DimmingLogRepository logRepository;
    private final MqttCommandPublisher mqttCommandPublisher;

    // ────────── 即時調光 (FN-07-023) ──────────

    @Transactional
    public DimmingLogResponse instantDim(InstantDimRequest req) {
        DimmingLog dimmingLog = DimmingLog.builder()
                .deviceId(req.getDeviceId())
                .commandType(DimmingCommandType.INSTANT)
                .brightnessPct(req.getBrightness())
                .result(DimmingResult.PENDING)
                .sentAt(LocalDateTime.now())
                .build();
        logRepository.save(dimmingLog);

        mqttCommandPublisher.sendCommand(req.getDeviceId(), Map.of(
                "cmd", "dim",
                "value", req.getBrightness(),
                "logId", dimmingLog.getId()
        ));

        log.info("[Dimming] Instant dim sent — device={}, brightness={}%, logId={}",
                req.getDeviceId(), req.getBrightness(), dimmingLog.getId());

        return toLogResponse(dimmingLog);
    }

    // ────────── 群組調光 (FN-07-024, D23: 逐燈循序) ──────────

    @Transactional
    public List<DimmingLogResponse> groupDim(GroupDimRequest req) {
        DimmingGroup group = groupRepository.findById(req.getGroupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IOT_DIMMING_GROUP_NOT_FOUND));

        List<DimmingLogResponse> results = new ArrayList<>();
        for (Long deviceId : group.getDeviceIds()) {
            InstantDimRequest dimReq = InstantDimRequest.builder()
                    .deviceId(deviceId)
                    .brightness(req.getBrightness())
                    .build();
            results.add(instantDim(dimReq));
        }

        log.info("[Dimming] Group dim sent — group={}, devices={}, brightness={}%",
                req.getGroupId(), group.getDeviceIds().length, req.getBrightness());

        return results;
    }

    // ────────── ACK 處理 ──────────

    @Transactional
    public void onAck(Long logId, boolean success) {
        logRepository.findById(logId).ifPresent(dimmingLog -> {
            if (dimmingLog.getResult() == DimmingResult.PENDING) {
                dimmingLog.setResult(success ? DimmingResult.SUCCESS : DimmingResult.FAILED);
                dimmingLog.setAckAt(LocalDateTime.now());
                logRepository.save(dimmingLog);
                log.info("[Dimming] ACK received — logId={}, result={}", logId,
                        dimmingLog.getResult());
            }
        });
    }

    // ────────── 調光群組 CRUD (FN-07-028) ──────────

    public Page<DimmingGroupResponse> listGroups(Pageable pageable) {
        return groupRepository.findAll(pageable).map(this::toGroupResponse);
    }

    @Transactional
    public DimmingGroupResponse createGroup(DimmingGroupRequest req) {
        DimmingGroup group = DimmingGroup.builder()
                .groupName(req.getGroupName())
                .deviceIds(req.getDeviceIds())
                .build();
        groupRepository.save(group);
        return toGroupResponse(group);
    }

    @Transactional
    public DimmingGroupResponse updateGroup(Long id, DimmingGroupRequest req) {
        DimmingGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IOT_DIMMING_GROUP_NOT_FOUND));
        group.setGroupName(req.getGroupName());
        group.setDeviceIds(req.getDeviceIds());
        groupRepository.save(group);
        return toGroupResponse(group);
    }

    @Transactional
    public void deleteGroup(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.IOT_DIMMING_GROUP_NOT_FOUND);
        }
        groupRepository.deleteById(id);
    }

    // ────────── 調光排程 CRUD (FN-07-025, D27: Service 層驗證互斥) ──────────

    public Page<DimmingScheduleResponse> listSchedules(Pageable pageable) {
        return scheduleRepository.findAll(pageable).map(this::toScheduleResponse);
    }

    @Transactional
    public DimmingScheduleResponse createSchedule(DimmingScheduleRequest req) {
        validateScheduleExclusive(req);
        DimmingSchedule schedule = DimmingSchedule.builder()
                .scheduleName(req.getScheduleName())
                .targetType(req.getTargetType())
                .targetId(req.getTargetId())
                .brightnessPct(req.getBrightnessPct())
                .scheduleCron(req.getScheduleCron())
                .oneTimeAt(req.getOneTimeAt())
                .enabled(req.getEnabled() != null ? req.getEnabled() : true)
                .build();
        scheduleRepository.save(schedule);
        return toScheduleResponse(schedule);
    }

    @Transactional
    public DimmingScheduleResponse updateSchedule(Long id, DimmingScheduleRequest req) {
        validateScheduleExclusive(req);
        DimmingSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IOT_DIMMING_SCHEDULE_NOT_FOUND));
        schedule.setScheduleName(req.getScheduleName());
        schedule.setTargetType(req.getTargetType());
        schedule.setTargetId(req.getTargetId());
        schedule.setBrightnessPct(req.getBrightnessPct());
        schedule.setScheduleCron(req.getScheduleCron());
        schedule.setOneTimeAt(req.getOneTimeAt());
        if (req.getEnabled() != null) {
            schedule.setEnabled(req.getEnabled());
        }
        scheduleRepository.save(schedule);
        return toScheduleResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.IOT_DIMMING_SCHEDULE_NOT_FOUND);
        }
        scheduleRepository.deleteById(id);
    }

    // ────────── 調光指令紀錄 (FN-07-029) ──────────

    public Page<DimmingLogResponse> listLogs(Long deviceId, Pageable pageable) {
        if (deviceId != null) {
            return logRepository.findByDeviceIdOrderBySentAtDesc(deviceId, pageable)
                    .map(this::toLogResponse);
        }
        return logRepository.findAllByOrderBySentAtDesc(pageable).map(this::toLogResponse);
    }

    // ────────── D27: cron / one_time_at 互斥驗證 ──────────

    private void validateScheduleExclusive(DimmingScheduleRequest req) {
        boolean hasCron = req.getScheduleCron() != null && !req.getScheduleCron().isBlank();
        boolean hasOneTime = req.getOneTimeAt() != null;
        if (hasCron == hasOneTime) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "scheduleCron 與 oneTimeAt 必須擇一填寫，不可同時有值或同時為空");
        }
    }

    // ────────── Mappers ──────────

    private DimmingLogResponse toLogResponse(DimmingLog log) {
        return DimmingLogResponse.builder()
                .id(log.getId())
                .deviceId(log.getDeviceId())
                .commandType(log.getCommandType())
                .brightnessPct(log.getBrightnessPct())
                .result(log.getResult())
                .sentAt(log.getSentAt())
                .ackAt(log.getAckAt())
                .scheduleId(log.getScheduleId())
                .build();
    }

    private DimmingGroupResponse toGroupResponse(DimmingGroup g) {
        return DimmingGroupResponse.builder()
                .id(g.getId())
                .groupName(g.getGroupName())
                .deviceIds(g.getDeviceIds())
                .createdAt(g.getCreatedAt())
                .build();
    }

    private DimmingScheduleResponse toScheduleResponse(DimmingSchedule s) {
        return DimmingScheduleResponse.builder()
                .id(s.getId())
                .scheduleName(s.getScheduleName())
                .targetType(s.getTargetType())
                .targetId(s.getTargetId())
                .brightnessPct(s.getBrightnessPct())
                .scheduleCron(s.getScheduleCron())
                .oneTimeAt(s.getOneTimeAt())
                .enabled(s.getEnabled())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
