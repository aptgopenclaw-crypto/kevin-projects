package com.taipei.iot.device.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.device.dto.CircuitRequest;
import com.taipei.iot.device.dto.CircuitResponse;
import com.taipei.iot.device.entity.Circuit;
import com.taipei.iot.device.repository.CircuitRepository;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CircuitService {

    private final CircuitRepository circuitRepository;
    private final DeviceRepository deviceRepository;

    public Page<CircuitResponse> list(String keyword, Pageable pageable) {
        return circuitRepository.findByFilters(keyword, pageable).map(this::toResponse);
    }

    public CircuitResponse getById(Long id) {
        Circuit circuit = circuitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CIRCUIT_NOT_FOUND));
        return toResponse(circuit);
    }

    @Transactional
    public CircuitResponse create(CircuitRequest request) {
        String tenantId = TenantContext.getCurrentTenantId();
        circuitRepository.findByTenantIdAndCircuitNumber(tenantId, request.getCircuitNumber())
                .ifPresent(c -> { throw new BusinessException(ErrorCode.VALIDATION_ERROR, "回路編號已存在"); });

        Circuit circuit = Circuit.builder()
                .panelBoxDeviceId(request.getPanelBoxDeviceId())
                .circuitNumber(request.getCircuitNumber())
                .circuitName(request.getCircuitName())
                .taipowerAccount(request.getTaipowerAccount())
                .usageType(request.getUsageType())
                .status("ACTIVE")
                .build();

        return toResponse(circuitRepository.save(circuit));
    }

    @Transactional
    public CircuitResponse update(Long id, CircuitRequest request) {
        Circuit circuit = circuitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CIRCUIT_NOT_FOUND));

        circuit.setPanelBoxDeviceId(request.getPanelBoxDeviceId());
        circuit.setCircuitNumber(request.getCircuitNumber());
        circuit.setCircuitName(request.getCircuitName());
        circuit.setTaipowerAccount(request.getTaipowerAccount());
        circuit.setUsageType(request.getUsageType());

        return toResponse(circuitRepository.save(circuit));
    }

    @Transactional
    public void delete(Long id) {
        Circuit circuit = circuitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CIRCUIT_NOT_FOUND));

        long deviceCount = deviceRepository.countByCircuitId(id);
        if (deviceCount > 0) {
            throw new BusinessException(ErrorCode.CIRCUIT_HAS_DEVICES);
        }

        circuitRepository.delete(circuit);
    }

    private CircuitResponse toResponse(Circuit c) {
        return CircuitResponse.builder()
                .id(c.getId())
                .panelBoxDeviceId(c.getPanelBoxDeviceId())
                .circuitNumber(c.getCircuitNumber())
                .circuitName(c.getCircuitName())
                .taipowerAccount(c.getTaipowerAccount())
                .usageType(c.getUsageType())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
