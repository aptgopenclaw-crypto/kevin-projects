package com.taipei.iot.replacement.service;

import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.setting.service.SystemSettingService;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.replacement.dto.PoleNumberRequest;
import com.taipei.iot.replacement.dto.PoleNumberResponse;
import com.taipei.iot.replacement.entity.LightPoleNumber;
import com.taipei.iot.replacement.enums.PoleNumberStatus;
import com.taipei.iot.replacement.repository.LightPoleNumberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LightPoleNumberServiceTest {

    @InjectMocks private LightPoleNumberService service;
    @Mock private LightPoleNumberRepository repo;
    @Mock private QrCodeService qrCodeService;
    @Mock private PoleNumberPdfExportService pdfExportService;
    @Mock private SystemSettingService settingService;

    @Test
    void generate_success_populatesQrCodeUrl() {
        try (MockedStatic<TenantContext> utils = mockStatic(TenantContext.class)) {
            utils.when(TenantContext::getCurrentTenantId).thenReturn("tenant-001");
            when(repo.existsByTenantIdAndPoleNumber("tenant-001", "PN-001")).thenReturn(false);
            when(settingService.getSetting("frontend_base_url")).thenReturn("https://streetlight.taipei");
            when(repo.save(any())).thenAnswer(inv -> {
                LightPoleNumber entity = inv.getArgument(0);
                entity.setId(1L);
                return entity;
            });

            PoleNumberRequest request = PoleNumberRequest.builder()
                    .poleNumber("PN-001").deviceId(100L).build();

            PoleNumberResponse result = service.generate(request);

            assertNotNull(result);
            assertEquals(PoleNumberStatus.ACTIVE, result.getStatus());
            assertEquals("https://streetlight.taipei/public/repair?pole=PN-001", result.getQrCodeUrl());
            verify(repo).save(any());
        }
    }

    @Test
    void generate_duplicate_throwsException() {
        try (MockedStatic<TenantContext> utils = mockStatic(TenantContext.class)) {
            utils.when(TenantContext::getCurrentTenantId).thenReturn("tenant-001");
            when(repo.existsByTenantIdAndPoleNumber("tenant-001", "PN-001")).thenReturn(true);

            PoleNumberRequest request = PoleNumberRequest.builder()
                    .poleNumber("PN-001").build();

            assertThrows(BusinessException.class, () -> service.generate(request));
        }
    }

    @Test
    void getQrCodePng_success() {
        LightPoleNumber pole = LightPoleNumber.builder().id(1L).poleNumber("PN-001").build();
        when(repo.findById(1L)).thenReturn(Optional.of(pole));
        when(settingService.getSetting("frontend_base_url")).thenReturn("https://streetlight.taipei");
        when(qrCodeService.generatePng("https://streetlight.taipei/public/repair?pole=PN-001"))
                .thenReturn(new byte[]{1, 2, 3});

        byte[] result = service.getQrCodePng(1L);

        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void getQrCodePng_notFound_throwsException() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getQrCodePng(999L));
    }

    @Test
    void exportQrCodesPdf_success() throws Exception {
        LightPoleNumber p1 = LightPoleNumber.builder().id(1L).poleNumber("PN-001").build();
        LightPoleNumber p2 = LightPoleNumber.builder().id(2L).poleNumber("PN-002").build();
        when(repo.findAllById(List.of(1L, 2L))).thenReturn(List.of(p1, p2));
        when(settingService.getSetting("frontend_base_url")).thenReturn("https://streetlight.taipei");
        when(pdfExportService.exportPdf(anyList(), eq("https://streetlight.taipei")))
                .thenReturn(new byte[]{10, 20});

        byte[] result = service.exportQrCodesPdf(List.of(1L, 2L));

        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    void exportQrCodesPdf_empty_throwsException() {
        when(repo.findAllById(List.of(999L))).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.exportQrCodesPdf(List.of(999L)));
    }
}
