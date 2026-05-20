package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.TenderAnnouncementQueryRequest;
import com.taipei.iot.tender.dto.TenderAnnouncementResponse;
import com.taipei.iot.tender.service.TenderAnnouncementService;
import com.taipei.iot.user.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tender/announcements")
@RequiredArgsConstructor
public class TenderAnnouncementController {

    private final TenderAnnouncementService service;

    @GetMapping
    @PreAuthorize("hasAuthority('tender:announcement:view')")
    public BaseResponse<PageResponse<TenderAnnouncementResponse>> search(
            TenderAnnouncementQueryRequest req) {
        return BaseResponse.success(service.search(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:announcement:view')")
    public BaseResponse<TenderAnnouncementResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(service.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:announcement:delete')")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return BaseResponse.success(null);
    }
}
