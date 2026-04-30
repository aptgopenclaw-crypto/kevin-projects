package com.taipei.iot.fault.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.fault.dto.FaultTicketRequest;
import com.taipei.iot.fault.dto.FaultTicketResponse;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.enums.FaultTicketSource;
import com.taipei.iot.fault.enums.FaultTicketStatus;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaultTicketService {

    private final FaultTicketRepository faultTicketRepository;
    private final FaultCorrelationService faultCorrelationService;

    public Page<FaultTicketResponse> list(FaultTicketStatus status, String keyword, Pageable pageable) {
        return faultTicketRepository.findByFilters(status, keyword, pageable).map(this::toResponse);
    }

    public FaultTicketResponse getById(Long id) {
        FaultTicket ticket = faultTicketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "障礙工單不存在"));
        return toResponse(ticket);
    }

    @Transactional
    public FaultTicketResponse create(FaultTicketRequest request) {
        FaultTicket ticket = FaultTicket.builder()
                .deviceId(request.getDeviceId())
                .circuitId(request.getCircuitId())
                .source(request.getSource())
                .status(FaultTicketStatus.OPEN)
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .description(request.getDescription())
                .reportedBy(SecurityContextUtils.getCurrentUserId())
                .reportedAt(LocalDateTime.now())
                .build();

        ticket = faultTicketRepository.save(ticket);

        // 新工單建立後同步檢查關聯障礙
        faultCorrelationService.detectOnNewTicket(ticket);

        return toResponse(ticket);
    }

    @Transactional
    public FaultTicketResponse resolve(Long id, String resolutionNote) {
        FaultTicket ticket = faultTicketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "障礙工單不存在"));
        ticket.setStatus(FaultTicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setResolvedBy(SecurityContextUtils.getCurrentUserId());
        ticket.setResolutionNote(resolutionNote);
        return toResponse(faultTicketRepository.save(ticket));
    }

    /**
     * 巡查發現異常 → 自動建立障礙工單 (E13)
     */
    @Transactional
    public FaultTicket createFromInspection(Long deviceId, String notes, Long inspectionRecordId) {
        FaultTicket ticket = FaultTicket.builder()
                .deviceId(deviceId)
                .source(FaultTicketSource.PATROL)
                .status(FaultTicketStatus.OPEN)
                .priority("NORMAL")
                .description("巡查發現異常：" + (notes != null ? notes : ""))
                .reportedBy(SecurityContextUtils.getCurrentUserId())
                .reportedAt(LocalDateTime.now())
                .build();

        ticket = faultTicketRepository.save(ticket);
        faultCorrelationService.detectOnNewTicket(ticket);
        return ticket;
    }

    private FaultTicketResponse toResponse(FaultTicket t) {
        return FaultTicketResponse.builder()
                .id(t.getId())
                .deviceId(t.getDeviceId())
                .circuitId(t.getCircuitId())
                .correlationId(t.getCorrelationId())
                .source(t.getSource())
                .status(t.getStatus())
                .priority(t.getPriority())
                .description(t.getDescription())
                .reportedBy(t.getReportedBy())
                .reportedAt(t.getReportedAt())
                .resolvedAt(t.getResolvedAt())
                .resolvedBy(t.getResolvedBy())
                .resolutionNote(t.getResolutionNote())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
