package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.IssueRecordRequest;
import com.taipei.iot.material.dto.IssueRequestRequest;
import com.taipei.iot.material.dto.IssueRequestResponse;
import com.taipei.iot.material.enums.IssueRequestStatus;
import com.taipei.iot.material.service.IssueService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/material/issue-requests")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PageResponse<IssueRequestResponse>> list(
            @RequestParam(required = false) IssueRequestStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<IssueRequestResponse> result = issueService.list(status, keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<IssueRequestResponse> create(@Valid @RequestBody IssueRequestRequest request) {
        return BaseResponse.success(issueService.createManual(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<IssueRequestResponse> approve(@PathVariable Long id) {
        return BaseResponse.success(issueService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<IssueRequestResponse> reject(@PathVariable Long id) {
        return BaseResponse.success(issueService.reject(id));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<Void> issue(@PathVariable Long id,
                                     @Valid @RequestBody List<IssueRecordRequest> items) {
        issueService.issue(id, items);
        return BaseResponse.success(null);
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
