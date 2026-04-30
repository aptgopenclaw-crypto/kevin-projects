package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.material.dto.MaterialSpecRequest;
import com.taipei.iot.material.dto.MaterialSpecResponse;
import com.taipei.iot.material.entity.MaterialSpec;
import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.material.enums.MaterialStatus;
import com.taipei.iot.material.repository.MaterialSpecRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialSpecService {

    private final MaterialSpecRepository materialSpecRepository;

    public Page<MaterialSpecResponse> list(MaterialCategory category, MaterialStatus status,
                                            String keyword, Pageable pageable) {
        return materialSpecRepository.findByFilters(category, status, keyword, pageable)
                .map(this::toResponse);
    }

    public MaterialSpecResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public MaterialSpecResponse create(MaterialSpecRequest request) {
        MaterialSpec spec = MaterialSpec.builder()
                .specCode(request.getSpecCode())
                .specName(request.getSpecName())
                .category(request.getCategory())
                .unit(request.getUnit() != null ? request.getUnit() : "PCS")
                .attributes(request.getAttributes())
                .status(request.getStatus() != null ? request.getStatus() : MaterialStatus.ACTIVE)
                .build();
        return toResponse(materialSpecRepository.save(spec));
    }

    @Transactional
    public MaterialSpecResponse update(Long id, MaterialSpecRequest request) {
        MaterialSpec spec = findOrThrow(id);
        spec.setSpecName(request.getSpecName());
        spec.setCategory(request.getCategory());
        if (request.getUnit() != null) spec.setUnit(request.getUnit());
        if (request.getAttributes() != null) spec.setAttributes(request.getAttributes());
        if (request.getStatus() != null) spec.setStatus(request.getStatus());
        return toResponse(materialSpecRepository.save(spec));
    }

    MaterialSpec findOrThrow(Long id) {
        return materialSpecRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_SPEC_NOT_FOUND));
    }

    Optional<MaterialSpec> findBySpecCode(String specCode) {
        return materialSpecRepository.findByTenantIdAndSpecCode(
                TenantContext.getCurrentTenantId(), specCode);
    }

    private MaterialSpecResponse toResponse(MaterialSpec ms) {
        return MaterialSpecResponse.builder()
                .id(ms.getId())
                .specCode(ms.getSpecCode())
                .specName(ms.getSpecName())
                .category(ms.getCategory())
                .unit(ms.getUnit())
                .attributes(ms.getAttributes())
                .status(ms.getStatus())
                .createdAt(ms.getCreatedAt())
                .updatedAt(ms.getUpdatedAt())
                .build();
    }
}
