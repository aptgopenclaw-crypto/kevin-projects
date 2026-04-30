package com.taipei.iot.repair.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.repair.dto.CompletionReportRequest;
import com.taipei.iot.repair.dto.DispatchRequest;
import com.taipei.iot.repair.dto.DispatchResponse;
import com.taipei.iot.repair.dto.RepairTicketQueryParams;
import com.taipei.iot.repair.dto.RepairTicketRequest;
import com.taipei.iot.repair.dto.RepairTicketResponse;
import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.service.RepairDispatchService;
import com.taipei.iot.repair.service.RepairTicketService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/repair/tickets")
@RequiredArgsConstructor
public class RepairTicketController {

    private final RepairTicketService repairTicketService;
    private final RepairDispatchService repairDispatchService;

    @GetMapping
    @PreAuthorize("hasAuthority('REPAIR_VIEW')")
    public BaseResponse<PageResponse<RepairTicketResponse>> list(
            @RequestParam(required = false) RepairTicketStatus status,
            @RequestParam(required = false) RepairTicketSource source,
            @RequestParam(required = false) RepairTicketPriority priority,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RepairTicketQueryParams params = RepairTicketQueryParams.builder()
                .status(status).source(source).priority(priority)
                .deptId(deptId).keyword(keyword).build();
        Page<RepairTicketResponse> result = repairTicketService.list(params, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REPAIR_VIEW')")
    public BaseResponse<RepairTicketResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(repairTicketService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REPAIR_MANAGE')")
    public BaseResponse<RepairTicketResponse> create(@Valid @RequestBody RepairTicketRequest request) {
        return BaseResponse.success(repairTicketService.createDirect(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REPAIR_MANAGE')")
    public BaseResponse<RepairTicketResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody RepairTicketRequest request) {
        return BaseResponse.success(repairTicketService.update(id, request));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('REPAIR_MANAGE')")
    public BaseResponse<RepairTicketResponse> accept(@PathVariable Long id) {
        return BaseResponse.success(repairTicketService.accept(id));
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('REPAIR_DISPATCH')")
    public BaseResponse<DispatchResponse> dispatch(@PathVariable Long id,
                                                    @Valid @RequestBody DispatchRequest request) {
        return BaseResponse.success(repairDispatchService.dispatch(id, request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('REPAIR_MANAGE')")
    public BaseResponse<Void> complete(@PathVariable Long id,
                                        @Valid @RequestBody CompletionReportRequest request) {
        repairTicketService.reportCompletion(id, request);
        return BaseResponse.success(null);
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('REPAIR_MANAGE')")
    public BaseResponse<RepairTicketResponse> transfer(@PathVariable Long id) {
        return BaseResponse.success(repairTicketService.transfer(id));
    }

    @GetMapping("/{id}/dispatches")
    @PreAuthorize("hasAuthority('REPAIR_VIEW')")
    public BaseResponse<List<DispatchResponse>> getDispatches(@PathVariable Long id) {
        return BaseResponse.success(repairDispatchService.getByTicketId(id));
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
