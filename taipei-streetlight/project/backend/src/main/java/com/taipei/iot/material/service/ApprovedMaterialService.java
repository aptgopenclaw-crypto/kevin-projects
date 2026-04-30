package com.taipei.iot.material.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.device.repository.ContractRepository;
import com.taipei.iot.material.dto.ApprovedMaterialRequest;
import com.taipei.iot.material.dto.ApprovedMaterialResponse;
import com.taipei.iot.material.dto.ImportResult;
import com.taipei.iot.material.entity.ApprovedMaterial;
import com.taipei.iot.material.enums.ApprovedMaterialStatus;
import com.taipei.iot.material.repository.ApprovedMaterialRepository;
import com.taipei.iot.material.repository.MaterialSpecRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovedMaterialService {

    private final ApprovedMaterialRepository approvedMaterialRepository;
    private final MaterialSpecRepository materialSpecRepository;
    private final ContractRepository contractRepository;

    public Page<ApprovedMaterialResponse> list(ApprovedMaterialStatus status, Long materialSpecId,
                                                String keyword, Pageable pageable) {
        return approvedMaterialRepository.findByFilters(status, materialSpecId, keyword, pageable)
                .map(this::toResponse);
    }

    public ApprovedMaterialResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ApprovedMaterialResponse create(ApprovedMaterialRequest request) {
        ApprovedMaterial material = ApprovedMaterial.builder()
                .materialSpecId(request.getMaterialSpecId())
                .contractId(request.getContractId())
                .materialNumber(request.getMaterialNumber())
                .approvalDate(request.getApprovalDate())
                .batchNumber(request.getBatchNumber())
                .brand(request.getBrand())
                .model(request.getModel())
                .specDetails(request.getSpecDetails())
                .status(request.getStatus() != null ? request.getStatus() : ApprovedMaterialStatus.ACTIVE)
                .build();
        return toResponse(approvedMaterialRepository.save(material));
    }

    @Transactional
    public ApprovedMaterialResponse update(Long id, ApprovedMaterialRequest request) {
        ApprovedMaterial material = findOrThrow(id);
        material.setMaterialSpecId(request.getMaterialSpecId());
        material.setContractId(request.getContractId());
        material.setApprovalDate(request.getApprovalDate());
        material.setBatchNumber(request.getBatchNumber());
        material.setBrand(request.getBrand());
        material.setModel(request.getModel());
        material.setSpecDetails(request.getSpecDetails());
        if (request.getStatus() != null) material.setStatus(request.getStatus());
        return toResponse(approvedMaterialRepository.save(material));
    }

    @Transactional
    @AuditEvent(AuditEventType.IMPORT_APPROVED_MATERIAL)
    public ImportResult batchImport(MultipartFile file) {
        List<CSVRecord> records;
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             var parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            records = parser.getRecords();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 解析失敗: " + e.getMessage());
        }

        int success = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        String tenantId = TenantContext.getCurrentTenantId();

        for (int i = 0; i < records.size(); i++) {
            CSVRecord row = records.get(i);
            try {
                String materialNumber = row.get("material_number");
                if (approvedMaterialRepository.existsByTenantIdAndMaterialNumber(tenantId, materialNumber)) {
                    skipped++;
                    continue;
                }

                String specCode = row.get("spec_code");
                var spec = materialSpecRepository.findByTenantIdAndSpecCode(tenantId, specCode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_SPEC_NOT_FOUND,
                                "spec_code: " + specCode));

                ApprovedMaterial material = ApprovedMaterial.builder()
                        .materialSpecId(spec.getId())
                        .materialNumber(materialNumber)
                        .approvalDate(LocalDate.parse(row.get("approval_date")))
                        .batchNumber(row.isMapped("batch_number") ? row.get("batch_number") : null)
                        .brand(row.isMapped("brand") ? row.get("brand") : null)
                        .model(row.isMapped("model") ? row.get("model") : null)
                        .status(ApprovedMaterialStatus.ACTIVE)
                        .build();
                approvedMaterialRepository.save(material);
                success++;
            } catch (Exception e) {
                errors.add("Row " + (i + 2) + ": " + e.getMessage());
            }
        }

        return ImportResult.builder()
                .successCount(success)
                .skippedCount(skipped)
                .errors(errors)
                .build();
    }

    ApprovedMaterial findOrThrow(Long id) {
        return approvedMaterialRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVED_MATERIAL_NOT_FOUND));
    }

    private ApprovedMaterialResponse toResponse(ApprovedMaterial am) {
        return ApprovedMaterialResponse.builder()
                .id(am.getId())
                .materialSpecId(am.getMaterialSpecId())
                .specCode(am.getMaterialSpec() != null ? am.getMaterialSpec().getSpecCode() : null)
                .specName(am.getMaterialSpec() != null ? am.getMaterialSpec().getSpecName() : null)
                .contractId(am.getContractId())
                .materialNumber(am.getMaterialNumber())
                .approvalDate(am.getApprovalDate())
                .batchNumber(am.getBatchNumber())
                .brand(am.getBrand())
                .model(am.getModel())
                .specDetails(am.getSpecDetails())
                .status(am.getStatus())
                .createdAt(am.getCreatedAt())
                .build();
    }
}
