package com.taipei.iot.device.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.device.dto.ContractRequest;
import com.taipei.iot.device.dto.ContractResponse;
import com.taipei.iot.device.enums.ContractStatus;
import com.taipei.iot.device.service.ContractService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public BaseResponse<PageResponse<ContractResponse>> list(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ContractResponse> result = contractService.list(status, keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public BaseResponse<ContractResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(contractService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTRACT_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_CONTRACT)
    public BaseResponse<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        return BaseResponse.success(contractService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_CONTRACT)
    public BaseResponse<ContractResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ContractRequest request) {
        return BaseResponse.success(contractService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_CONTRACT)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
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
