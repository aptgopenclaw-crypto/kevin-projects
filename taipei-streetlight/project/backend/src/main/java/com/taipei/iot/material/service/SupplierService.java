package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.material.dto.SupplierRequest;
import com.taipei.iot.material.dto.SupplierResponse;
import com.taipei.iot.material.entity.Supplier;
import com.taipei.iot.material.enums.SupplierStatus;
import com.taipei.iot.material.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public Page<SupplierResponse> list(SupplierStatus status, String keyword, Pageable pageable) {
        return supplierRepository.findByFilters(status, keyword, pageable)
                .map(this::toResponse);
    }

    public SupplierResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<SupplierResponse> listActive() {
        return supplierRepository.findByStatus(SupplierStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .supplierCode(request.getSupplierCode())
                .supplierName(request.getSupplierName())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .address(request.getAddress())
                .status(request.getStatus() != null ? request.getStatus() : SupplierStatus.ACTIVE)
                .build();
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = findOrThrow(id);
        supplier.setSupplierName(request.getSupplierName());
        supplier.setContactName(request.getContactName());
        supplier.setContactPhone(request.getContactPhone());
        supplier.setContactEmail(request.getContactEmail());
        supplier.setAddress(request.getAddress());
        if (request.getStatus() != null) supplier.setStatus(request.getStatus());
        return toResponse(supplierRepository.save(supplier));
    }

    Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    private SupplierResponse toResponse(Supplier s) {
        return SupplierResponse.builder()
                .id(s.getId())
                .supplierCode(s.getSupplierCode())
                .supplierName(s.getSupplierName())
                .contactName(s.getContactName())
                .contactPhone(s.getContactPhone())
                .contactEmail(s.getContactEmail())
                .address(s.getAddress())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
