package com.taipei.iot.repair.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.service.FileStorageService;
import com.taipei.iot.common.service.FileValidationService;
import com.taipei.iot.common.service.ImageSanitizer;
import com.taipei.iot.common.service.VirusScanService;
import com.taipei.iot.repair.dto.AttachmentResponse;
import com.taipei.iot.repair.dto.AttachmentUploadRequest;
import com.taipei.iot.repair.entity.TicketAttachment;
import com.taipei.iot.repair.enums.AttachmentPhase;
import com.taipei.iot.repair.enums.ScanStatus;
import com.taipei.iot.repair.enums.TicketType;
import com.taipei.iot.repair.repository.TicketAttachmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketAttachmentServiceTest {

    @InjectMocks private TicketAttachmentService ticketAttachmentService;
    @Mock private TicketAttachmentRepository attachmentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileValidationService fileValidationService;
    @Mock private ImageSanitizer imageSanitizer;
    @Mock private VirusScanService virusScanService;

    @Test
    void upload_image_sanitizedAndClean() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        when(fileValidationService.validateExtension("photo.jpg")).thenReturn("jpg");
        when(fileValidationService.isImage("jpg")).thenReturn(true);
        when(imageSanitizer.sanitize(any(), eq("jpg"))).thenReturn(new byte[]{4, 5, 6});
        when(fileStorageService.store(anyString(), eq("photo.jpg"), any(InputStream.class)))
                .thenReturn("repair_ticket/1/abc_photo.jpg");
        when(fileStorageService.resolveAbsolutePath("repair_ticket/1/abc_photo.jpg"))
                .thenReturn("/uploads/repair_ticket/1/abc_photo.jpg");
        when(virusScanService.scan("/uploads/repair_ticket/1/abc_photo.jpg"))
                .thenReturn(VirusScanService.ScanResult.CLEAN);

        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            TicketAttachment a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AttachmentUploadRequest meta = AttachmentUploadRequest.builder()
                .gpsLat(BigDecimal.valueOf(25.04))
                .gpsLng(BigDecimal.valueOf(121.52))
                .phase(AttachmentPhase.BEFORE)
                .build();

        AttachmentResponse result = ticketAttachmentService.upload(
                TicketType.REPAIR_TICKET, 1L, file, meta);

        assertNotNull(result);
        assertEquals(ScanStatus.CLEAN, result.getScanStatus());
        assertEquals(AttachmentPhase.BEFORE, result.getPhase());
        verify(imageSanitizer).sanitize(any(), eq("jpg"));
        verify(virusScanService).scan(anyString());
    }

    @Test
    void upload_nonImage_skipsSanitization() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("report.pdf");
        when(file.getSize()).thenReturn(2048L);
        when(file.getContentType()).thenReturn("application/pdf");

        when(fileValidationService.validateExtension("report.pdf")).thenReturn("pdf");
        when(fileValidationService.isImage("pdf")).thenReturn(false);
        when(fileStorageService.store(anyString(), eq(file)))
                .thenReturn("repair_ticket/1/abc_report.pdf");
        when(fileStorageService.resolveAbsolutePath(anyString()))
                .thenReturn("/uploads/repair_ticket/1/abc_report.pdf");
        when(virusScanService.scan(anyString())).thenReturn(VirusScanService.ScanResult.CLEAN);

        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            TicketAttachment a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        AttachmentUploadRequest meta = AttachmentUploadRequest.builder().build();

        AttachmentResponse result = ticketAttachmentService.upload(
                TicketType.REPAIR_TICKET, 1L, file, meta);

        assertEquals(ScanStatus.CLEAN, result.getScanStatus());
        verify(imageSanitizer, never()).sanitize(any(), anyString());
    }

    @Test
    void upload_virusDetected_deletesFileAndThrows() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("malware.pdf");

        lenient().when(fileValidationService.validateExtension("malware.pdf")).thenReturn("pdf");
        lenient().when(fileValidationService.isImage("pdf")).thenReturn(false);
        when(fileStorageService.store(anyString(), eq(file))).thenReturn("repair_ticket/1/abc_malware.pdf");
        when(fileStorageService.resolveAbsolutePath(anyString())).thenReturn("/uploads/repair_ticket/1/abc_malware.pdf");
        when(virusScanService.scan(anyString())).thenReturn(VirusScanService.ScanResult.INFECTED);

        AttachmentUploadRequest meta = AttachmentUploadRequest.builder().build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketAttachmentService.upload(TicketType.REPAIR_TICKET, 1L, file, meta));

        assertEquals(ErrorCode.FILE_VIRUS_DETECTED, ex.getErrorCode());
        verify(fileStorageService).delete("repair_ticket/1/abc_malware.pdf");
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void upload_scanError_fallbackToPending() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");

        when(fileValidationService.validateExtension("doc.pdf")).thenReturn("pdf");
        when(fileValidationService.isImage("pdf")).thenReturn(false);
        when(fileStorageService.store(anyString(), eq(file))).thenReturn("repair_ticket/1/abc_doc.pdf");
        when(fileStorageService.resolveAbsolutePath(anyString())).thenReturn("/uploads/repair_ticket/1/abc_doc.pdf");
        when(virusScanService.scan(anyString())).thenReturn(VirusScanService.ScanResult.ERROR);

        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            TicketAttachment a = inv.getArgument(0);
            a.setId(3L);
            return a;
        });

        AttachmentUploadRequest meta = AttachmentUploadRequest.builder().build();

        AttachmentResponse result = ticketAttachmentService.upload(
                TicketType.REPAIR_TICKET, 1L, file, meta);

        assertEquals(ScanStatus.PENDING, result.getScanStatus());
    }

    @Test
    void getByTicket_returnsList() {
        TicketAttachment att = TicketAttachment.builder()
                .id(1L).ticketType(TicketType.REPAIR_TICKET).ticketId(10L)
                .fileType("PHOTO").fileUrl("path/to/file.jpg")
                .scanStatus(ScanStatus.CLEAN).build();
        when(attachmentRepository.findByTicketTypeAndTicketIdOrderByUploadedAtDesc(
                TicketType.REPAIR_TICKET, 10L)).thenReturn(List.of(att));

        List<AttachmentResponse> result = ticketAttachmentService.getByTicket(
                TicketType.REPAIR_TICKET, 10L);

        assertEquals(1, result.size());
        assertEquals("PHOTO", result.get(0).getFileType());
    }

    @Test
    void download_success() {
        TicketAttachment att = TicketAttachment.builder()
                .id(1L).fileUrl("repair_ticket/1/file.jpg").build();
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(att));
        when(fileStorageService.load("repair_ticket/1/file.jpg"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        InputStream result = ticketAttachmentService.download(1L);

        assertNotNull(result);
    }

    @Test
    void download_notFound_throwsException() {
        when(attachmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> ticketAttachmentService.download(99L));
    }
}
