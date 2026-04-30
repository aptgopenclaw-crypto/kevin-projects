package com.taipei.iot.replacement.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.setting.enums.SettingKey;
import com.taipei.iot.setting.service.SystemSettingService;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.replacement.dto.PoleNumberRequest;
import com.taipei.iot.replacement.dto.PoleNumberResponse;
import com.taipei.iot.replacement.entity.LightPoleNumber;
import com.taipei.iot.replacement.enums.PoleNumberStatus;
import com.taipei.iot.replacement.repository.LightPoleNumberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LightPoleNumberService {

    private final LightPoleNumberRepository repo;
    private final QrCodeService qrCodeService;
    private final PoleNumberPdfExportService pdfExportService;
    private final SystemSettingService settingService;

    public Page<PoleNumberResponse> list(String keyword, Pageable pageable) {
        return repo.findByFilters(null, keyword, pageable).map(this::toResponse);
    }

    @Transactional
    public PoleNumberResponse generate(PoleNumberRequest request) {
        String tenantId = TenantContext.getCurrentTenantId();

        if (repo.existsByTenantIdAndPoleNumber(tenantId, request.getPoleNumber())) {
            throw new BusinessException(ErrorCode.POLE_NUMBER_DUPLICATE);
        }

        String baseUrl = settingService.getSetting(SettingKey.FRONTEND_BASE_URL.getKey());
        String qrContentUrl = baseUrl + "/public/repair?pole=" + request.getPoleNumber();

        LightPoleNumber entity = LightPoleNumber.builder()
                .poleNumber(request.getPoleNumber())
                .deviceId(request.getDeviceId())
                .qrCodeUrl(qrContentUrl)
                .issuedAt(LocalDate.now())
                .status(PoleNumberStatus.ACTIVE)
                .build();

        return toResponse(repo.save(entity));
    }

    public byte[] getQrCodePng(Long id) {
        LightPoleNumber pole = repo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLE_NUMBER_NOT_FOUND));

        String baseUrl = settingService.getSetting(SettingKey.FRONTEND_BASE_URL.getKey());
        String content = baseUrl + "/public/repair?pole=" + pole.getPoleNumber();
        return qrCodeService.generatePng(content);
    }

    public byte[] exportQrCodesPdf(List<Long> ids) {
        List<LightPoleNumber> poleNumbers = repo.findAllById(ids);
        if (poleNumbers.isEmpty()) {
            throw new BusinessException(ErrorCode.POLE_NUMBER_NOT_FOUND);
        }

        String baseUrl = settingService.getSetting(SettingKey.FRONTEND_BASE_URL.getKey());
        try {
            return pdfExportService.exportPdf(poleNumbers, baseUrl);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF", e);
        }
    }

    private PoleNumberResponse toResponse(LightPoleNumber entity) {
        return PoleNumberResponse.builder()
                .id(entity.getId())
                .poleNumber(entity.getPoleNumber())
                .deviceId(entity.getDeviceId())
                .qrCodeUrl(entity.getQrCodeUrl())
                .issuedAt(entity.getIssuedAt())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
