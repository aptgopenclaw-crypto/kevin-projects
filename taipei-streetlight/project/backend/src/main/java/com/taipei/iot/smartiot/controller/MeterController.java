package com.taipei.iot.smartiot.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.smartiot.dto.MeterStatusResponse;
import com.taipei.iot.smartiot.service.MeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 電表狀態 API (FN-07-022)。
 */
@RestController
@RequestMapping("/v1/auth/iot/meters")
@RequiredArgsConstructor
public class MeterController {

    private final MeterService meterService;

    /** GET /v1/auth/iot/meters/status — 電表狀態總覽 */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<MeterStatusResponse> getStatus() {
        return BaseResponse.success(meterService.getStatus());
    }
}
