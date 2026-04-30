package com.taipei.iot.repair.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.service.FaultTicketService;
import com.taipei.iot.repair.dto.InspectionRecordRequest;
import com.taipei.iot.repair.dto.InspectionRecordResponse;
import com.taipei.iot.repair.dto.InspectionTaskRequest;
import com.taipei.iot.repair.dto.InspectionTaskResponse;
import com.taipei.iot.repair.entity.InspectionRecord;
import com.taipei.iot.repair.entity.InspectionTask;
import com.taipei.iot.repair.enums.InspectionResult;
import com.taipei.iot.repair.enums.InspectionTaskStatus;
import com.taipei.iot.repair.repository.InspectionRecordRepository;
import com.taipei.iot.repair.repository.InspectionTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionService {

    private final InspectionTaskRepository taskRepository;
    private final InspectionRecordRepository recordRepository;
    private final FaultTicketService faultTicketService;

    // ── 巡查任務 CRUD ──────────────────────────────────

    public Page<InspectionTaskResponse> listTasks(Pageable pageable) {
        return taskRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toTaskResponse);
    }

    public InspectionTaskResponse getTaskById(Long id) {
        return toTaskResponse(findTaskOrThrow(id));
    }

    @Transactional
    public InspectionTaskResponse createTask(InspectionTaskRequest request) {
        InspectionTask task = InspectionTask.builder()
                .taskName(request.getTaskName())
                .taskType(request.getTaskType())
                .scheduleCron(request.getScheduleCron())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .areaScope(request.getAreaScope())
                .deptId(request.getDeptId())
                .assignedTo(request.getAssignedTo())
                .status(InspectionTaskStatus.ACTIVE)
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();
        return toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public InspectionTaskResponse updateTask(Long id, InspectionTaskRequest request) {
        InspectionTask task = findTaskOrThrow(id);
        task.setTaskName(request.getTaskName());
        task.setTaskType(request.getTaskType());
        task.setScheduleCron(request.getScheduleCron());
        task.setStartDate(request.getStartDate());
        task.setEndDate(request.getEndDate());
        task.setAreaScope(request.getAreaScope());
        task.setDeptId(request.getDeptId());
        task.setAssignedTo(request.getAssignedTo());
        return toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public void deactivateTask(Long id) {
        InspectionTask task = findTaskOrThrow(id);
        task.setStatus(InspectionTaskStatus.INACTIVE);
        taskRepository.save(task);
    }

    // ── 巡查紀錄 ──────────────────────────────────────

    public Page<InspectionRecordResponse> getRecordsByTask(Long taskId, Pageable pageable) {
        return recordRepository.findByTaskIdOrderByInspectionDateDesc(taskId, pageable)
                .map(this::toRecordResponse);
    }

    public InspectionRecordResponse getRecordById(Long id) {
        InspectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_RECORD_NOT_FOUND));
        return toRecordResponse(record);
    }

    @Transactional
    public InspectionRecordResponse createRecord(InspectionRecordRequest request) {
        findTaskOrThrow(request.getTaskId());

        String currentUserId = SecurityContextUtils.getCurrentUserId();
        Long inspectorId = currentUserId != null ? Long.parseLong(currentUserId) : null;

        InspectionRecord record = InspectionRecord.builder()
                .taskId(request.getTaskId())
                .inspectorId(inspectorId)
                .inspectionDate(LocalDateTime.now())
                .deviceId(request.getDeviceId())
                .result(request.getResult())
                .notes(request.getNotes())
                .attachments(request.getAttachments())
                .build();

        InspectionRecord saved = recordRepository.save(record);

        // E13：巡查結果為 NEED_REPAIR → 自動建立障礙工單
        if (InspectionResult.NEED_REPAIR == request.getResult() && request.getDeviceId() != null) {
            FaultTicket fault = faultTicketService.createFromInspection(
                    request.getDeviceId(), request.getNotes(), saved.getId());
            saved.setFaultTicketId(fault.getId());
            recordRepository.save(saved);
        }

        return toRecordResponse(saved);
    }

    // ── helpers ──────────────────────────────────────

    private InspectionTask findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TASK_NOT_FOUND));
    }

    private InspectionTaskResponse toTaskResponse(InspectionTask t) {
        return InspectionTaskResponse.builder()
                .id(t.getId())
                .taskName(t.getTaskName())
                .taskType(t.getTaskType())
                .scheduleCron(t.getScheduleCron())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .areaScope(t.getAreaScope())
                .deptId(t.getDeptId())
                .assignedTo(t.getAssignedTo())
                .status(t.getStatus())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private InspectionRecordResponse toRecordResponse(InspectionRecord r) {
        return InspectionRecordResponse.builder()
                .id(r.getId())
                .taskId(r.getTaskId())
                .inspectorId(r.getInspectorId())
                .inspectionDate(r.getInspectionDate())
                .deviceId(r.getDeviceId())
                .result(r.getResult())
                .notes(r.getNotes())
                .attachments(r.getAttachments())
                .faultTicketId(r.getFaultTicketId())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
