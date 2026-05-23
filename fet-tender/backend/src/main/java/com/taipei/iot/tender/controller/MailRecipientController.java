package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.MailRecipientBatchRequest;
import com.taipei.iot.tender.dto.MailRecipientBatchResult;
import com.taipei.iot.tender.dto.MailRecipientRequest;
import com.taipei.iot.tender.dto.MailRecipientResponse;
import com.taipei.iot.tender.service.MailRecipientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tender/mail-recipients")
@RequiredArgsConstructor
public class MailRecipientController {

    private final MailRecipientService service;

    @GetMapping
    @PreAuthorize("hasAuthority('tender:config:view')")
    public BaseResponse<List<MailRecipientResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<MailRecipientResponse> result = includeInactive
                ? service.listAllIncludeInactive()
                : service.listAll();
        return BaseResponse.success(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<MailRecipientResponse> create(@Valid @RequestBody MailRecipientRequest req) {
        return BaseResponse.success(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<MailRecipientResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MailRecipientRequest req) {
        return BaseResponse.success(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return BaseResponse.success(null);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<MailRecipientBatchResult> batchImport(
            @Valid @RequestBody MailRecipientBatchRequest req) {
        return BaseResponse.success(service.batchImport(req.getEmails()));
    }
}
