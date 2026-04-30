package com.taipei.iot.material.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.material.dto.IssueRecordRequest;
import com.taipei.iot.material.dto.IssueRequestRequest;
import com.taipei.iot.material.dto.IssueRequestResponse;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.entity.IssueRecord;
import com.taipei.iot.material.entity.IssueRequest;
import com.taipei.iot.material.enums.IssueRequestStatus;
import com.taipei.iot.material.repository.InventoryRepository;
import com.taipei.iot.material.repository.IssueRecordRepository;
import com.taipei.iot.material.repository.IssueRequestRepository;
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
public class IssueService {

    private final IssueRequestRepository issueRequestRepository;
    private final IssueRecordRepository issueRecordRepository;
    private final InventoryRepository inventoryRepository;

    private final AtomicLong dailySequence = new AtomicLong(1);
    private volatile String lastDate = "";

    public Page<IssueRequestResponse> list(IssueRequestStatus status, String keyword, Pageable pageable) {
        return issueRequestRepository.findByFilters(status, keyword, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public IssueRequestResponse createManual(IssueRequestRequest request) {
        IssueRequest issueRequest = IssueRequest.builder()
                .requestNumber(generateRequestNumber())
                .repairTicketId(request.getRepairTicketId())
                .replacementOrderId(request.getReplacementOrderId())
                .requestedBy(SecurityContextUtils.getCurrentUserId())
                .status(IssueRequestStatus.PENDING)
                .build();
        return toResponse(issueRequestRepository.save(issueRequest));
    }

    @Transactional
    public IssueRequestResponse approve(Long id) {
        IssueRequest request = findOrThrow(id);
        validateStatus(request, IssueRequestStatus.PENDING);
        request.setStatus(IssueRequestStatus.APPROVED);
        return toResponse(issueRequestRepository.save(request));
    }

    @Transactional
    public IssueRequestResponse reject(Long id) {
        IssueRequest request = findOrThrow(id);
        validateStatus(request, IssueRequestStatus.PENDING);
        request.setStatus(IssueRequestStatus.REJECTED);
        return toResponse(issueRequestRepository.save(request));
    }

    @Transactional
    @AuditEvent(AuditEventType.ISSUE_MATERIAL)
    public void issue(Long requestId, List<IssueRecordRequest> items) {
        IssueRequest request = findOrThrow(requestId);
        validateStatus(request, IssueRequestStatus.APPROVED);

        for (var item : items) {
            Inventory inventory = inventoryRepository.findById(item.getInventoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

            if (inventory.getQuantityOnHand() < item.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY);
            }

            inventory.setQuantityOnHand(inventory.getQuantityOnHand() - item.getQuantity());
            inventoryRepository.save(inventory);

            IssueRecord record = IssueRecord.builder()
                    .requestId(requestId)
                    .inventoryId(item.getInventoryId())
                    .materialSpecId(item.getMaterialSpecId())
                    .quantity(item.getQuantity())
                    .issuedBy(SecurityContextUtils.getCurrentUsername())
                    .issuedAt(LocalDateTime.now())
                    .build();
            issueRecordRepository.save(record);
        }

        request.setStatus(IssueRequestStatus.ISSUED);
        issueRequestRepository.save(request);
    }

    private IssueRequest findOrThrow(Long id) {
        return issueRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ISSUE_REQUEST_NOT_FOUND));
    }

    private void validateStatus(IssueRequest request, IssueRequestStatus expected) {
        if (request.getStatus() != expected) {
            throw new BusinessException(ErrorCode.INVALID_ISSUE_REQUEST_STATUS);
        }
    }

    private synchronized String generateRequestNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(1);
        }
        return String.format("IR-%s-%03d", today, dailySequence.getAndIncrement());
    }

    @Transactional
    public IssueRequest createFromReplacement(Long replacementOrderId) {
        IssueRequest request = IssueRequest.builder()
                .requestNumber(generateRequestNumber())
                .replacementOrderId(replacementOrderId)
                .requestedBy(SecurityContextUtils.getCurrentUserId())
                .status(IssueRequestStatus.PENDING)
                .build();
        return issueRequestRepository.save(request);
    }

    private IssueRequestResponse toResponse(IssueRequest r) {
        return IssueRequestResponse.builder()
                .id(r.getId())
                .requestNumber(r.getRequestNumber())
                .repairTicketId(r.getRepairTicketId())
                .replacementOrderId(r.getReplacementOrderId())
                .requestedBy(r.getRequestedBy())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
