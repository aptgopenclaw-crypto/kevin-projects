package com.taipei.iot.fault.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.fault.dto.FaultTicketRequest;
import com.taipei.iot.fault.dto.FaultTicketResponse;
import com.taipei.iot.fault.enums.FaultTicketStatus;
import com.taipei.iot.fault.service.FaultTicketService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/faults")
@RequiredArgsConstructor
public class FaultTicketController {

    private final FaultTicketService faultTicketService;

    @GetMapping
    @PreAuthorize("hasAuthority('FAULT_VIEW')")
    public BaseResponse<PageResponse<FaultTicketResponse>> list(
            @RequestParam(required = false) FaultTicketStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FaultTicketResponse> result = faultTicketService.list(status, keyword,
                PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FAULT_VIEW')")
    public BaseResponse<FaultTicketResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(faultTicketService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FAULT_MANAGE')")
    public BaseResponse<FaultTicketResponse> create(@Valid @RequestBody FaultTicketRequest request) {
        return BaseResponse.success(faultTicketService.create(request));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('FAULT_MANAGE')")
    public BaseResponse<FaultTicketResponse> resolve(@PathVariable Long id,
                                                      @RequestParam(required = false) String resolutionNote) {
        return BaseResponse.success(faultTicketService.resolve(id, resolutionNote));
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
