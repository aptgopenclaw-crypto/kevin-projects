package com.taipei.iot.repair.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.repair.dto.AttachmentResponse;
import com.taipei.iot.repair.dto.AttachmentUploadRequest;
import com.taipei.iot.repair.enums.AttachmentPhase;
import com.taipei.iot.repair.enums.TicketType;
import com.taipei.iot.repair.service.TicketAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/auth/repair")
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;

    @GetMapping("/tickets/{ticketId}/attachments")
    @PreAuthorize("hasAuthority('REPAIR_VIEW')")
    public BaseResponse<List<AttachmentResponse>> listAttachments(@PathVariable Long ticketId) {
        return BaseResponse.success(
                attachmentService.getByTicket(TicketType.REPAIR_TICKET, ticketId));
    }

    @PostMapping("/tickets/{ticketId}/attachments")
    @PreAuthorize("hasAuthority('REPAIR_MANAGE')")
    public BaseResponse<AttachmentResponse> uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal gpsLat,
            @RequestParam(required = false) BigDecimal gpsLng,
            @RequestParam(required = false) LocalDateTime takenAt,
            @RequestParam(required = false) AttachmentPhase phase) {

        AttachmentUploadRequest meta = AttachmentUploadRequest.builder()
                .fileType(fileType)
                .description(description)
                .gpsLat(gpsLat)
                .gpsLng(gpsLng)
                .takenAt(takenAt)
                .phase(phase)
                .build();

        return BaseResponse.success(
                attachmentService.upload(TicketType.REPAIR_TICKET, ticketId, file, meta));
    }

    @GetMapping("/attachments/{id}/download")
    @PreAuthorize("hasAuthority('REPAIR_VIEW')")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        InputStream inputStream = attachmentService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }
}
