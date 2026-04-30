package com.taipei.iot.material.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.material.dto.DisposalRequest;
import com.taipei.iot.material.dto.DisposalResponse;
import com.taipei.iot.material.entity.DisposalRecord;
import com.taipei.iot.material.repository.DisposalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisposalService {

    private final DisposalRecordRepository disposalRecordRepository;

    public Page<DisposalResponse> list(Pageable pageable) {
        return disposalRecordRepository.findAllByOrderByDisposedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional
    @AuditEvent(AuditEventType.DISPOSE_MATERIAL)
    public DisposalResponse create(DisposalRequest request) {
        DisposalRecord record = DisposalRecord.builder()
                .materialSpecId(request.getMaterialSpecId())
                .quantity(request.getQuantity())
                .disposalType(request.getDisposalType())
                .reason(request.getReason())
                .disposedBy(SecurityContextUtils.getCurrentUsername())
                .disposedAt(LocalDateTime.now())
                .build();
        return toResponse(disposalRecordRepository.save(record));
    }

    private DisposalResponse toResponse(DisposalRecord d) {
        return DisposalResponse.builder()
                .id(d.getId())
                .materialSpecId(d.getMaterialSpecId())
                .quantity(d.getQuantity())
                .disposalType(d.getDisposalType())
                .reason(d.getReason())
                .disposedBy(d.getDisposedBy())
                .disposedAt(d.getDisposedAt())
                .build();
    }
}
