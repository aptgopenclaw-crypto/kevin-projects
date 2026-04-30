package com.taipei.iot.repair.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.service.FileStorageService;
import com.taipei.iot.common.service.FileValidationService;
import com.taipei.iot.common.service.ImageSanitizer;
import com.taipei.iot.common.service.VirusScanService;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.repair.dto.AttachmentResponse;
import com.taipei.iot.repair.dto.AttachmentUploadRequest;
import com.taipei.iot.repair.entity.TicketAttachment;
import com.taipei.iot.repair.enums.ScanStatus;
import com.taipei.iot.repair.enums.TicketType;
import com.taipei.iot.repair.repository.TicketAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketAttachmentService {

    private final TicketAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final ImageSanitizer imageSanitizer;
    private final VirusScanService virusScanService;

    public List<AttachmentResponse> getByTicket(TicketType ticketType, Long ticketId) {
        return attachmentRepository.findByTicketTypeAndTicketIdOrderByUploadedAtDesc(ticketType, ticketId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AttachmentResponse upload(TicketType ticketType, Long ticketId,
                                     MultipartFile file, AttachmentUploadRequest meta) {
        // 1. 驗證：副檔名白名單 + Magic bytes + 大小
        fileValidationService.validate(file);
        String ext = fileValidationService.validateExtension(file.getOriginalFilename());

        // 2. 圖片消毒（僅 image 類型）
        String subDir = ticketType.name().toLowerCase() + "/" + ticketId;
        String fileUrl;
        if (fileValidationService.isImage(ext)) {
            try {
                byte[] sanitized = imageSanitizer.sanitize(file.getInputStream(), ext);
                if (sanitized != null) {
                    fileUrl = fileStorageService.store(subDir, file.getOriginalFilename(),
                            new ByteArrayInputStream(sanitized));
                } else {
                    // 消毒失敗 fallback：存原檔
                    fileUrl = fileStorageService.store(subDir, file);
                }
            } catch (Exception e) {
                log.warn("圖片消毒失敗，存原檔: {}", e.getMessage());
                fileUrl = fileStorageService.store(subDir, file);
            }
        } else {
            fileUrl = fileStorageService.store(subDir, file);
        }

        // 3. 病毒掃描
        String absPath = fileStorageService.resolveAbsolutePath(fileUrl);
        VirusScanService.ScanResult scanResult = virusScanService.scan(absPath);

        ScanStatus scanStatus = switch (scanResult) {
            case CLEAN -> ScanStatus.CLEAN;
            case INFECTED -> {
                // 刪除受感染檔案
                fileStorageService.delete(fileUrl);
                throw new BusinessException(ErrorCode.FILE_VIRUS_DETECTED);
            }
            case ERROR -> ScanStatus.PENDING; // ClamAV 不可用，允許上傳但標記待掃描
        };

        // 4. 建立附件紀錄
        TicketAttachment attachment = TicketAttachment.builder()
                .ticketType(ticketType)
                .ticketId(ticketId)
                .fileType(meta.getFileType() != null ? meta.getFileType() : detectFileType(file))
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .description(meta.getDescription())
                .gpsLat(meta.getGpsLat())
                .gpsLng(meta.getGpsLng())
                .takenAt(meta.getTakenAt())
                .phase(meta.getPhase())
                .scanStatus(scanStatus)
                .uploadedBy(SecurityContextUtils.getCurrentUserId())
                .uploadedAt(LocalDateTime.now())
                .build();

        return toResponse(attachmentRepository.save(attachment));
    }

    @Transactional
    public AttachmentResponse createFromUrl(TicketType ticketType, Long ticketId,
                                            AttachmentUploadRequest request) {
        TicketAttachment attachment = TicketAttachment.builder()
                .ticketType(ticketType)
                .ticketId(ticketId)
                .fileType(request.getFileType())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .description(request.getDescription())
                .gpsLat(request.getGpsLat())
                .gpsLng(request.getGpsLng())
                .takenAt(request.getTakenAt())
                .phase(request.getPhase())
                .scanStatus(ScanStatus.PENDING)
                .uploadedBy(SecurityContextUtils.getCurrentUserId())
                .uploadedAt(LocalDateTime.now())
                .build();

        return toResponse(attachmentRepository.save(attachment));
    }

    public InputStream download(Long attachmentId) {
        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));
        return fileStorageService.load(attachment.getFileUrl());
    }

    private String detectFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return "DOCUMENT";
        if (contentType.startsWith("image/")) return "PHOTO";
        if (contentType.startsWith("video/")) return "VIDEO";
        if (contentType.startsWith("audio/")) return "AUDIO";
        return "DOCUMENT";
    }

    private AttachmentResponse toResponse(TicketAttachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .ticketType(a.getTicketType())
                .ticketId(a.getTicketId())
                .fileType(a.getFileType())
                .fileUrl(a.getFileUrl())
                .fileName(a.getFileName())
                .fileSize(a.getFileSize())
                .description(a.getDescription())
                .gpsLat(a.getGpsLat())
                .gpsLng(a.getGpsLng())
                .takenAt(a.getTakenAt())
                .phase(a.getPhase())
                .scanStatus(a.getScanStatus())
                .uploadedBy(a.getUploadedBy())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
