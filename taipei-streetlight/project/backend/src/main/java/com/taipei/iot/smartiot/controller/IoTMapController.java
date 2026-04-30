package com.taipei.iot.smartiot.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.gis.dto.GeoJsonResponse;
import com.taipei.iot.smartiot.service.IoTMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IoT 地圖狀態 API (FN-07-009, D17)。
 */
@RestController
@RequestMapping("/v1/auth/iot/map")
@RequiredArgsConstructor
public class IoTMapController {

    private final IoTMapService iotMapService;

    /**
     * GET /v1/auth/iot/map/status — IoT 設備地圖狀態 (GeoJSON FeatureCollection)
     */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<GeoJsonResponse> getMapStatus() {
        return BaseResponse.success(iotMapService.getMapStatus());
    }
}
