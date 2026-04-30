package com.taipei.iot.workflow.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.workflow.dto.DelegateCandidateDto;
import com.taipei.iot.workflow.dto.DelegateSettingRequest;
import com.taipei.iot.workflow.dto.DelegateSettingResponse;
import com.taipei.iot.workflow.service.DelegateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/delegates")
@RequiredArgsConstructor
public class DelegateController {

    private final DelegateService delegateService;

    @GetMapping("/candidates")
    @PreAuthorize("hasAuthority('DELEGATE_MANAGE')")
    public BaseResponse<List<DelegateCandidateDto>> candidates() {
        return BaseResponse.success(delegateService.getCandidates());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DELEGATE_MANAGE')")
    public BaseResponse<List<DelegateSettingResponse>> list() {
        return BaseResponse.success(delegateService.getMyDelegates());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DELEGATE_MANAGE')")
    public BaseResponse<DelegateSettingResponse> create(@Valid @RequestBody DelegateSettingRequest request) {
        return BaseResponse.success(delegateService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELEGATE_MANAGE')")
    public BaseResponse<Void> deactivate(@PathVariable Long id) {
        delegateService.deactivate(id);
        return BaseResponse.success(null);
    }
}
