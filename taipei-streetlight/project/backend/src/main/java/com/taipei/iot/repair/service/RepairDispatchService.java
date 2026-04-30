package com.taipei.iot.repair.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.repair.dto.DispatchRequest;
import com.taipei.iot.repair.dto.DispatchResponse;
import com.taipei.iot.repair.entity.RepairDispatch;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.enums.RepairDispatchStatus;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.repository.RepairDispatchRepository;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RepairDispatchService {

    private final RepairDispatchRepository repairDispatchRepository;
    private final RepairTicketRepository repairTicketRepository;
    private final WorkflowService workflowService;

    public List<DispatchResponse> getByTicketId(Long ticketId) {
        return repairDispatchRepository.findByRepairTicketIdOrderByDispatchedAtDesc(ticketId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    @AuditEvent(AuditEventType.DISPATCH_REPAIR)
    public DispatchResponse dispatch(Long ticketId, DispatchRequest request) {
        RepairTicket ticket = repairTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_TICKET_NOT_FOUND));

        if (ticket.getStatus() != RepairTicketStatus.ACCEPTED
                && ticket.getStatus() != RepairTicketStatus.TRANSFERRED) {
            throw new BusinessException(ErrorCode.REPAIR_TICKET_INVALID_STATUS,
                    "只有已收案或改分轉送的工單可以派工");
        }

        String currentUserId = SecurityContextUtils.getCurrentUserId();
        Long currentUserIdLong = currentUserId != null ? Long.parseLong(currentUserId) : null;

        RepairDispatch dispatch = RepairDispatch.builder()
                .repairTicketId(ticketId)
                .assignedTo(request.getAssignedTo())
                .assignedOrg(request.getAssignedOrg())
                .contractId(request.getContractId())
                .dueDate(request.getDueDate())
                .dispatchNote(request.getNote())
                .dispatchedBy(currentUserIdLong)
                .dispatchedAt(LocalDateTime.now())
                .status(RepairDispatchStatus.DISPATCHED)
                .build();
        repairDispatchRepository.save(dispatch);

        ticket.setStatus(RepairTicketStatus.DISPATCHED);
        ticket.setContractId(request.getContractId());
        repairTicketRepository.save(ticket);

        WorkflowInstance instance = workflowService.findByTicket("REPAIR_TICKET", ticketId);
        workflowService.transition(instance.getId(), "DISPATCHED", "派工",
                currentUserId, SecurityContextUtils.getCurrentUsername(),
                "派工給 " + request.getAssignedOrg(), null);

        return toResponse(dispatch);
    }

    private DispatchResponse toResponse(RepairDispatch d) {
        return DispatchResponse.builder()
                .id(d.getId())
                .repairTicketId(d.getRepairTicketId())
                .contractId(d.getContractId())
                .assignedTo(d.getAssignedTo())
                .assignedOrg(d.getAssignedOrg())
                .dispatchNote(d.getDispatchNote())
                .dispatchedAt(d.getDispatchedAt())
                .dispatchedBy(d.getDispatchedBy())
                .dueDate(d.getDueDate())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
