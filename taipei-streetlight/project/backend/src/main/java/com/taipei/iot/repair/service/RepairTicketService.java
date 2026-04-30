package com.taipei.iot.repair.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import com.taipei.iot.repair.dto.CompletionReportRequest;
import com.taipei.iot.repair.dto.PublicRepairRequest;
import com.taipei.iot.repair.dto.PublicRepairStatusResponse;
import com.taipei.iot.repair.dto.RepairTicketQueryParams;
import com.taipei.iot.repair.dto.RepairTicketRequest;
import com.taipei.iot.repair.dto.RepairTicketResponse;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.replacement.entity.LightPoleNumber;
import com.taipei.iot.replacement.repository.LightPoleNumberRepository;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RepairTicketService {

    private final RepairTicketRepository repairTicketRepository;
    private final FaultTicketRepository faultTicketRepository;
    private final DeviceRepository deviceRepository;
    private final LightPoleNumberRepository lightPoleNumberRepository;
    private final WorkflowService workflowService;
    private final DataScopeHelper dataScopeHelper;

    private static final String DEFAULT_TENANT_ID = "TENANT_A";
    private static final String CITIZEN_USER_ID = "CITIZEN";

    private final AtomicLong dailySequence = new AtomicLong(1);
    private volatile String lastDate = "";

    // ── 查詢 ──────────────────────────────────────────

    public Page<RepairTicketResponse> list(RepairTicketQueryParams params, Pageable pageable) {
        List<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();
        Page<RepairTicket> page = repairTicketRepository.findByFilters(
                params.getStatus(), params.getSource(), params.getPriority(),
                params.getDeptId(), params.getKeyword(),
                visibleDeptIds.isEmpty() ? null : visibleDeptIds,
                pageable);
        return page.map(this::toResponse);
    }

    public RepairTicketResponse getById(Long id) {
        RepairTicket ticket = findTicketOrThrow(id);
        return toResponse(ticket);
    }

    // ── 路徑 A：障礙審核通過 → 自動建立 ──────────────

    @Transactional
    public RepairTicket createFromFault(Long faultTicketId) {
        FaultTicket fault = faultTicketRepository.findById(faultTicketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "障礙工單不存在"));

        RepairTicket ticket = RepairTicket.builder()
                .ticketNumber(generateTicketNumber())
                .faultTicketId(faultTicketId)
                .deviceId(fault.getDeviceId())
                .circuitId(fault.getCircuitId())
                .source(RepairTicketSource.FAULT_TICKET)
                .reporterName(fault.getReportedBy())
                .reportDescription(fault.getDescription())
                .reportedAt(LocalDateTime.now())
                .status(RepairTicketStatus.PENDING)
                .priority(RepairTicketPriority.NORMAL)
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();

        RepairTicket saved = repairTicketRepository.save(ticket);

        workflowService.createInstance(
                "REPAIR_DISPATCH", "REPAIR_TICKET", saved.getId(),
                SecurityContextUtils.getCurrentUserId());

        return saved;
    }

    // ── 路徑 B：外部系統(1999)/ 民眾網頁 / 電話 → 手動立案 ──

    @Transactional
    @AuditEvent(AuditEventType.CREATE_REPAIR_TICKET)
    public RepairTicketResponse createDirect(RepairTicketRequest request) {
        if (request.getDeviceId() != null) {
            deviceRepository.findById(request.getDeviceId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        }

        RepairTicket ticket = RepairTicket.builder()
                .ticketNumber(generateTicketNumber())
                .faultTicketId(request.getFaultTicketId())
                .deviceId(request.getDeviceId())
                .circuitId(request.getCircuitId())
                .contractId(request.getContractId())
                .source(request.getSource())
                .reporterName(request.getReporterName())
                .reporterPhone(request.getReporterPhone())
                .reporterEmail(request.getReporterEmail())
                .reportAddress(request.getReportAddress())
                .reportDescription(request.getReportDescription())
                .reportedAt(LocalDateTime.now())
                .status(RepairTicketStatus.PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : RepairTicketPriority.NORMAL)
                .deptId(request.getDeptId())
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();

        RepairTicket saved = repairTicketRepository.save(ticket);

        workflowService.createInstance(
                "REPAIR_DISPATCH", "REPAIR_TICKET", saved.getId(),
                SecurityContextUtils.getCurrentUserId());

        return toResponse(saved);
    }

    // ── 更新 ──────────────────────────────────────────

    @Transactional
    @AuditEvent(AuditEventType.UPDATE_REPAIR_TICKET)
    public RepairTicketResponse update(Long id, RepairTicketRequest request) {
        RepairTicket ticket = findTicketOrThrow(id);

        ticket.setReporterName(request.getReporterName());
        ticket.setReporterPhone(request.getReporterPhone());
        ticket.setReporterEmail(request.getReporterEmail());
        ticket.setReportAddress(request.getReportAddress());
        ticket.setReportDescription(request.getReportDescription());
        ticket.setDeviceId(request.getDeviceId());
        ticket.setCircuitId(request.getCircuitId());
        ticket.setContractId(request.getContractId());
        ticket.setPriority(request.getPriority());
        ticket.setDeptId(request.getDeptId());

        return toResponse(repairTicketRepository.save(ticket));
    }

    // ── 收案 ──────────────────────────────────────────

    @Transactional
    public RepairTicketResponse accept(Long id) {
        RepairTicket ticket = findTicketOrThrow(id);
        validateStatus(ticket, RepairTicketStatus.PENDING);

        ticket.setStatus(RepairTicketStatus.ACCEPTED);
        repairTicketRepository.save(ticket);

        WorkflowInstance instance = workflowService.findByTicket("REPAIR_TICKET", id);
        workflowService.transition(instance.getId(), "ACCEPTED", "收案",
                SecurityContextUtils.getCurrentUserId(),
                SecurityContextUtils.getCurrentUsername(),
                "收案處理", null);

        return toResponse(ticket);
    }

    // ── 完工回報 ──────────────────────────────────────

    @Transactional
    @AuditEvent(AuditEventType.COMPLETE_REPAIR)
    public void reportCompletion(Long ticketId, CompletionReportRequest request) {
        RepairTicket ticket = findTicketOrThrow(ticketId);
        validateStatus(ticket, RepairTicketStatus.IN_PROGRESS);

        ticket.setRepairDescription(request.getRepairDescription());
        ticket.setFaultCause(request.getFaultCause());
        ticket.setCompletedAt(LocalDateTime.now());
        ticket.setStatus(RepairTicketStatus.COMPLETION_REPORTED);
        repairTicketRepository.save(ticket);

        WorkflowInstance instance = workflowService.findByTicket("REPAIR_TICKET", ticketId);
        workflowService.transition(instance.getId(), "COMPLETION_REPORTED", "完工回報",
                SecurityContextUtils.getCurrentUserId(),
                SecurityContextUtils.getCurrentUsername(),
                "完工回報：" + request.getRepairDescription(), null);
    }

    // ── 改分轉送 ──────────────────────────────────────

    @Transactional
    public RepairTicketResponse transfer(Long id) {
        RepairTicket ticket = findTicketOrThrow(id);
        validateStatus(ticket, RepairTicketStatus.DISPATCHED);

        ticket.setStatus(RepairTicketStatus.TRANSFERRED);
        repairTicketRepository.save(ticket);

        WorkflowInstance instance = workflowService.findByTicket("REPAIR_TICKET", id);
        workflowService.transition(instance.getId(), "TRANSFERRED", "改分轉送",
                SecurityContextUtils.getCurrentUserId(),
                SecurityContextUtils.getCurrentUsername(),
                "改分轉送", null);

        return toResponse(ticket);
    }

    // ── 路徑 C：民眾公開報修（匿名） ──────────────────

    @Transactional
    public RepairTicket createPublicTicket(PublicRepairRequest request) {
        String tenantId = DEFAULT_TENANT_ID;
        Long deviceId = null;

        if (request.getPoleNumber() != null && !request.getPoleNumber().isBlank()) {
            TenantContext.setSystemContext();
            try {
                LightPoleNumber pole = lightPoleNumberRepository.findByPoleNumber(request.getPoleNumber())
                        .orElse(null);
                if (pole != null) {
                    tenantId = pole.getTenantId();
                    deviceId = pole.getDeviceId();
                }
            } finally {
                TenantContext.clear();
            }
        }

        // Set tenant for entity persist and workflow creation
        TenantContext.setCurrentTenantId(tenantId);
        try {
            RepairTicket ticket = RepairTicket.builder()
                    .ticketNumber(generateTicketNumber())
                    .deviceId(deviceId)
                    .source(RepairTicketSource.CITIZEN_WEB)
                    .reporterName(request.getReporterName())
                    .reporterPhone(request.getReporterPhone())
                    .reporterEmail(request.getReporterEmail())
                    .reportAddress(request.getReportAddress())
                    .reportDescription(request.getReportDescription())
                    .reportedAt(LocalDateTime.now())
                    .status(RepairTicketStatus.PENDING)
                    .priority(RepairTicketPriority.NORMAL)
                    .createdBy(CITIZEN_USER_ID)
                    .build();

            RepairTicket saved = repairTicketRepository.save(ticket);

            workflowService.createInstance(
                    "REPAIR_DISPATCH", "REPAIR_TICKET", saved.getId(), CITIZEN_USER_ID);

            return saved;
        } finally {
            TenantContext.clear();
        }
    }

    public PublicRepairStatusResponse getPublicStatus(String ticketNumber, String phone) {
        TenantContext.setSystemContext();
        try {
            RepairTicket ticket = repairTicketRepository.findByTicketNumberAndReporterPhone(ticketNumber, phone)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_TICKET_NOT_FOUND));

            return PublicRepairStatusResponse.builder()
                    .ticketNumber(ticket.getTicketNumber())
                    .status(ticket.getStatus())
                    .statusLabel(resolveStatusLabel(ticket.getStatus()))
                    .createdAt(ticket.getCreatedAt())
                    .updatedAt(ticket.getUpdatedAt())
                    .build();
        } finally {
            TenantContext.clear();
        }
    }

    // ── 內部 helpers ──────────────────────────────────

    RepairTicket findTicketOrThrow(Long id) {
        return repairTicketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_TICKET_NOT_FOUND));
    }

    private void validateStatus(RepairTicket ticket, RepairTicketStatus expected) {
        if (ticket.getStatus() != expected) {
            throw new BusinessException(ErrorCode.REPAIR_TICKET_INVALID_STATUS,
                    String.format("預期狀態 %s，實際 %s", expected, ticket.getStatus()));
        }
    }

    private synchronized String generateTicketNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(1);
        }
        return String.format("RT-%s-%03d", today, dailySequence.getAndIncrement());
    }

    private RepairTicketResponse toResponse(RepairTicket t) {
        return RepairTicketResponse.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .faultTicketId(t.getFaultTicketId())
                .deviceId(t.getDeviceId())
                .circuitId(t.getCircuitId())
                .contractId(t.getContractId())
                .source(t.getSource())
                .reporterName(t.getReporterName())
                .reporterPhone(t.getReporterPhone())
                .reporterEmail(t.getReporterEmail())
                .reportAddress(t.getReportAddress())
                .reportDescription(t.getReportDescription())
                .reportedAt(t.getReportedAt())
                .faultCategory(t.getFaultCategory())
                .faultCause(t.getFaultCause())
                .repairDescription(t.getRepairDescription())
                .completedAt(t.getCompletedAt())
                .status(t.getStatus())
                .priority(t.getPriority())
                .deptId(t.getDeptId())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private String resolveStatusLabel(RepairTicketStatus status) {
        return switch (status) {
            case PENDING -> "待處理";
            case ACCEPTED -> "已收案";
            case DISPATCHED -> "已派工";
            case IN_PROGRESS -> "施工中";
            case COMPLETION_REPORTED -> "已完工";
            case PENDING_REVIEW -> "審查中";
            case RETURNED -> "退回修正";
            case TRANSFERRED -> "改分轉送";
            case TRACKING -> "追蹤中";
            case CLOSED -> "已結案";
        };
    }
}
