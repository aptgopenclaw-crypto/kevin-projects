package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.AgencyFilterRequest;
import com.taipei.iot.tender.dto.AgencyFilterResponse;
import com.taipei.iot.tender.service.AgencyFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tender/announcement-agency-filters")
@RequiredArgsConstructor
public class AgencyFilterController {

    private final AgencyFilterService service;

    @GetMapping
    @PreAuthorize("hasAuthority('tender:config:view')")
    public BaseResponse<List<AgencyFilterResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<AgencyFilterResponse> result = includeInactive
                ? service.listAllIncludeInactive()
                : service.listAll();
        return BaseResponse.success(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<AgencyFilterResponse> create(@Valid @RequestBody AgencyFilterRequest req) {
        return BaseResponse.success(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<AgencyFilterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AgencyFilterRequest req) {
        return BaseResponse.success(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:config:edit')")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return BaseResponse.success(null);
    }
}
