package com.taipei.iot.device.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.device.dto.ContractRequest;
import com.taipei.iot.device.dto.ContractResponse;
import com.taipei.iot.device.entity.Contract;
import com.taipei.iot.device.enums.ContractStatus;
import com.taipei.iot.device.repository.ContractRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;

    public Page<ContractResponse> list(ContractStatus status, String keyword, Pageable pageable) {
        return contractRepository.findByFilters(status, keyword, pageable).map(this::toResponse);
    }

    public ContractResponse getById(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));
        return toResponse(contract);
    }

    @Transactional
    public ContractResponse create(ContractRequest request) {
        Contract contract = Contract.builder()
                .contractCode(request.getContractCode())
                .contractName(request.getContractName())
                .budgetYear(request.getBudgetYear())
                .procurementNumber(request.getProcurementNumber())
                .contractorName(request.getContractorName())
                .contractorContact(request.getContractorContact())
                .assetCategory(request.getAssetCategory())
                .quantity(request.getQuantity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .acceptanceDate(request.getAcceptanceDate())
                .warrantyYears(request.getWarrantyYears())
                .warrantyExpiry(request.getWarrantyExpiry())
                .status(request.getStatus() != null ? request.getStatus() : ContractStatus.ACTIVE)
                .attributes(request.getAttributes())
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();

        return toResponse(contractRepository.save(contract));
    }

    @Transactional
    public ContractResponse update(Long id, ContractRequest request) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));

        contract.setContractCode(request.getContractCode());
        contract.setContractName(request.getContractName());
        contract.setBudgetYear(request.getBudgetYear());
        contract.setProcurementNumber(request.getProcurementNumber());
        contract.setContractorName(request.getContractorName());
        contract.setContractorContact(request.getContractorContact());
        contract.setAssetCategory(request.getAssetCategory());
        contract.setQuantity(request.getQuantity());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setAcceptanceDate(request.getAcceptanceDate());
        contract.setWarrantyYears(request.getWarrantyYears());
        contract.setWarrantyExpiry(request.getWarrantyExpiry());
        if (request.getStatus() != null) {
            contract.setStatus(request.getStatus());
        }
        contract.setAttributes(request.getAttributes());

        return toResponse(contractRepository.save(contract));
    }

    @Transactional
    public void delete(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));
        contractRepository.delete(contract);
    }

    private ContractResponse toResponse(Contract c) {
        return ContractResponse.builder()
                .id(c.getId())
                .contractCode(c.getContractCode())
                .contractName(c.getContractName())
                .budgetYear(c.getBudgetYear())
                .procurementNumber(c.getProcurementNumber())
                .contractorName(c.getContractorName())
                .contractorContact(c.getContractorContact())
                .assetCategory(c.getAssetCategory())
                .quantity(c.getQuantity())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .acceptanceDate(c.getAcceptanceDate())
                .warrantyYears(c.getWarrantyYears())
                .warrantyExpiry(c.getWarrantyExpiry())
                .status(c.getStatus())
                .attributes(c.getAttributes())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
