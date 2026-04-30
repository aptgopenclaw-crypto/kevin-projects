package com.taipei.iot.device.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.device.dto.CircuitRequest;
import com.taipei.iot.device.dto.CircuitResponse;
import com.taipei.iot.device.service.CircuitService;
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
@RequestMapping("/v1/auth/circuits")
@RequiredArgsConstructor
public class CircuitController {

    private final CircuitService circuitService;

    @GetMapping
    @PreAuthorize("hasAuthority('CIRCUIT_VIEW')")
    public BaseResponse<PageResponse<CircuitResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CircuitResponse> result = circuitService.list(keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CIRCUIT_VIEW')")
    public BaseResponse<CircuitResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(circuitService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CIRCUIT_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_CIRCUIT)
    public BaseResponse<CircuitResponse> create(@Valid @RequestBody CircuitRequest request) {
        return BaseResponse.success(circuitService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CIRCUIT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_CIRCUIT)
    public BaseResponse<CircuitResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody CircuitRequest request) {
        return BaseResponse.success(circuitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CIRCUIT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_CIRCUIT)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        circuitService.delete(id);
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
