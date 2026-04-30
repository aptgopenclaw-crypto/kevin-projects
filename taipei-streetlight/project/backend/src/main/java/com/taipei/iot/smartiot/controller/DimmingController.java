package com.taipei.iot.smartiot.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.smartiot.dto.DimmingGroupRequest;
import com.taipei.iot.smartiot.dto.DimmingGroupResponse;
import com.taipei.iot.smartiot.dto.DimmingLogResponse;
import com.taipei.iot.smartiot.dto.DimmingScheduleRequest;
import com.taipei.iot.smartiot.dto.DimmingScheduleResponse;
import com.taipei.iot.smartiot.dto.GroupDimRequest;
import com.taipei.iot.smartiot.dto.InstantDimRequest;
import com.taipei.iot.smartiot.service.DimmingService;
import com.taipei.iot.smartiot.service.DimmingSyncService;
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

import java.util.List;

/**
 * 調光控制 API (FN-07-023~029)。
 */
@RestController
@RequestMapping("/v1/auth/iot/dimming")
@RequiredArgsConstructor
public class DimmingController {

    private final DimmingService dimmingService;
    private final DimmingSyncService dimmingSyncService;

    // ────────── 即時調光 ──────────

    /** POST /v1/auth/iot/dimming/instant — 單燈即時調光 (FN-07-023) */
    @PostMapping("/instant")
    @PreAuthorize("hasAuthority('IOT_DIMMING')")
    @AuditEvent(AuditEventType.SEND_DIMMING_COMMAND)
    public BaseResponse<DimmingLogResponse> instantDim(@Valid @RequestBody InstantDimRequest req) {
        return BaseResponse.success(dimmingService.instantDim(req));
    }

    /** POST /v1/auth/iot/dimming/group — 群組即時調光 (FN-07-024) */
    @PostMapping("/group")
    @PreAuthorize("hasAuthority('IOT_DIMMING')")
    @AuditEvent(AuditEventType.SEND_DIMMING_COMMAND)
    public BaseResponse<List<DimmingLogResponse>> groupDim(@Valid @RequestBody GroupDimRequest req) {
        return BaseResponse.success(dimmingService.groupDim(req));
    }

    // ────────── 調光群組 CRUD (FN-07-028) ──────────

    /** GET /v1/auth/iot/dimming/groups — 群組列表 */
    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<PageResponse<DimmingGroupResponse>> listGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DimmingGroupResponse> result = dimmingService.listGroups(PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    /** POST /v1/auth/iot/dimming/groups — 新增群組 */
    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_DIMMING_GROUP)
    public BaseResponse<DimmingGroupResponse> createGroup(@Valid @RequestBody DimmingGroupRequest req) {
        return BaseResponse.success(dimmingService.createGroup(req));
    }

    /** PUT /v1/auth/iot/dimming/groups/{id} — 編輯群組 */
    @PutMapping("/groups/{id}")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_DIMMING_GROUP)
    public BaseResponse<DimmingGroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody DimmingGroupRequest req) {
        return BaseResponse.success(dimmingService.updateGroup(id, req));
    }

    /** DELETE /v1/auth/iot/dimming/groups/{id} — 刪除群組 */
    @DeleteMapping("/groups/{id}")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.DELETE_DIMMING_GROUP)
    public BaseResponse<Void> deleteGroup(@PathVariable Long id) {
        dimmingService.deleteGroup(id);
        return BaseResponse.success(null);
    }

    // ────────── 調光排程 CRUD (FN-07-025) ──────────

    /** GET /v1/auth/iot/dimming/schedules — 排程列表 */
    @GetMapping("/schedules")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<PageResponse<DimmingScheduleResponse>> listSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DimmingScheduleResponse> result = dimmingService.listSchedules(PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    /** POST /v1/auth/iot/dimming/schedules — 新增排程 */
    @PostMapping("/schedules")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_DIMMING_SCHEDULE)
    public BaseResponse<DimmingScheduleResponse> createSchedule(
            @Valid @RequestBody DimmingScheduleRequest req) {
        return BaseResponse.success(dimmingService.createSchedule(req));
    }

    /** PUT /v1/auth/iot/dimming/schedules/{id} — 編輯排程 */
    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_DIMMING_SCHEDULE)
    public BaseResponse<DimmingScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody DimmingScheduleRequest req) {
        return BaseResponse.success(dimmingService.updateSchedule(id, req));
    }

    /** DELETE /v1/auth/iot/dimming/schedules/{id} — 刪除排程 */
    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.DELETE_DIMMING_SCHEDULE)
    public BaseResponse<Void> deleteSchedule(@PathVariable Long id) {
        dimmingService.deleteSchedule(id);
        return BaseResponse.success(null);
    }

    // ────────── Fail-Safe 同步 (FN-07-027, D28) ──────────

    /** POST /v1/auth/iot/dimming/sync/{deviceId} — 手動同步調光指令 */
    @PostMapping("/sync/{deviceId}")
    @PreAuthorize("hasAuthority('IOT_DIMMING')")
    @AuditEvent(AuditEventType.SEND_DIMMING_COMMAND)
    public BaseResponse<Void> syncDevice(@PathVariable Long deviceId) {
        dimmingSyncService.syncDevice(deviceId);
        return BaseResponse.success(null);
    }

    // ────────── 調光指令紀錄 (FN-07-029) ──────────

    /** GET /v1/auth/iot/dimming/logs — 指令歷史 */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<PageResponse<DimmingLogResponse>> listLogs(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DimmingLogResponse> result = dimmingService.listLogs(deviceId, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    // ────────── Helper ──────────

    private <T> PageResponse<T> toPageResponse(Page<T> p) {
        return PageResponse.<T>builder()
                .content(p.getContent())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize())
                .build();
    }
}
