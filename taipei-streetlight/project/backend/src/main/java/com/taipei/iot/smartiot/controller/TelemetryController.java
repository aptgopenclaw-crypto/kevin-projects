package com.taipei.iot.smartiot.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.smartiot.dto.TelemetryBatchRequest;
import com.taipei.iot.smartiot.dto.TelemetryIngestRequest;
import com.taipei.iot.smartiot.dto.TelemetryResponse;
import com.taipei.iot.smartiot.service.TelemetryService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    // ── IoT Device Token endpoints (/v1/iot/**) ──

    /**
     * POST /v1/iot/telemetry — REST 遙測上行 (FN-07-004)
     */
    @PostMapping("/v1/iot/telemetry")
    public BaseResponse<TelemetryResponse> ingest(@Valid @RequestBody TelemetryIngestRequest request,
                                                   Authentication authentication) {
        Long deviceId = extractDeviceId(authentication);
        return BaseResponse.success(
                telemetryService.ingest(deviceId, request.getPayload(), request.getTimestamp()));
    }

    /**
     * POST /v1/iot/telemetry/batch — 批次回補 (FN-07-008)
     */
    @PostMapping("/v1/iot/telemetry/batch")
    public BaseResponse<List<TelemetryResponse>> batchIngest(@Valid @RequestBody TelemetryBatchRequest request,
                                                              Authentication authentication) {
        Long deviceId = extractDeviceId(authentication);
        return BaseResponse.success(telemetryService.batchIngest(deviceId, request.getRecords()));
    }

    /**
     * POST /v1/iot/heartbeat — 心跳上報 (FN-07-005)
     */
    @PostMapping("/v1/iot/heartbeat")
    public BaseResponse<Void> heartbeat(Authentication authentication) {
        Long deviceId = extractDeviceId(authentication);
        telemetryService.heartbeat(deviceId);
        return BaseResponse.success(null);
    }

    // ── JWT Auth endpoints (/v1/auth/iot/**) ──

    /**
     * GET /v1/auth/iot/devices/{id}/telemetry/latest — 最新 telemetry (FN-07-011)
     */
    @GetMapping("/v1/auth/iot/devices/{id}/telemetry/latest")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<TelemetryResponse> getLatest(@PathVariable Long id) {
        return BaseResponse.success(telemetryService.getLatest(id));
    }

    /**
     * GET /v1/auth/iot/devices/{id}/telemetry/history — 歷史 telemetry (FN-07-012)
     */
    @GetMapping("/v1/auth/iot/devices/{id}/telemetry/history")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<PageResponse<TelemetryResponse>> getHistory(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<TelemetryResponse> result = telemetryService.getHistory(id, from, to, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    /**
     * 從 DeviceTokenAuthFilter 設置的 Authentication 中取出 deviceId。
     */
    @SuppressWarnings("unchecked")
    private Long extractDeviceId(Authentication authentication) {
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        return (Long) details.get("deviceId");
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
