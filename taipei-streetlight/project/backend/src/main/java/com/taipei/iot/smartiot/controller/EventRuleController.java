package com.taipei.iot.smartiot.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.smartiot.dto.EventNotificationChannelRequest;
import com.taipei.iot.smartiot.dto.EventNotificationChannelResponse;
import com.taipei.iot.smartiot.dto.EventNotificationTargetRequest;
import com.taipei.iot.smartiot.dto.EventNotificationTargetResponse;
import com.taipei.iot.smartiot.dto.EventRuleConditionRequest;
import com.taipei.iot.smartiot.dto.EventRuleConditionResponse;
import com.taipei.iot.smartiot.dto.EventRuleRequest;
import com.taipei.iot.smartiot.dto.EventRuleResponse;
import com.taipei.iot.smartiot.service.EventRuleService;
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

@RestController
@RequestMapping("/v1/auth/iot/event-rules")
@RequiredArgsConstructor
public class EventRuleController {

    private final EventRuleService eventRuleService;

    /**
     * POST /v1/auth/iot/event-rules — 新增規則 (FN-07-013)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_EVENT_RULE)
    public BaseResponse<EventRuleResponse> create(@Valid @RequestBody EventRuleRequest request) {
        return BaseResponse.success(eventRuleService.create(request));
    }

    /**
     * GET /v1/auth/iot/event-rules — 列表 (FN-07-013)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<PageResponse<EventRuleResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EventRuleResponse> result = eventRuleService.list(PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    /**
     * PUT /v1/auth/iot/event-rules/{id} — 編輯規則 (FN-07-013)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_EVENT_RULE)
    public BaseResponse<EventRuleResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody EventRuleRequest request) {
        return BaseResponse.success(eventRuleService.update(id, request));
    }

    /**
     * DELETE /v1/auth/iot/event-rules/{id} — 刪除規則 (CASCADE)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.DELETE_EVENT_RULE)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        eventRuleService.delete(id);
        return BaseResponse.success(null);
    }

    /**
     * GET /v1/auth/iot/event-rules/{id}/conditions — 條件群組列表 (FN-07-048)
     */
    @GetMapping("/{id}/conditions")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<List<EventRuleConditionResponse>> getConditions(@PathVariable Long id) {
        return BaseResponse.success(eventRuleService.getConditions(id));
    }

    /**
     * PUT /v1/auth/iot/event-rules/{id}/conditions — 批次更新條件群組 (FN-07-048)
     */
    @PutMapping("/{id}/conditions")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_EVENT_RULE)
    public BaseResponse<List<EventRuleConditionResponse>> updateConditions(
            @PathVariable Long id,
            @Valid @RequestBody List<EventRuleConditionRequest> requests) {
        return BaseResponse.success(eventRuleService.updateConditions(id, requests));
    }

    // ── 通知對象 (recipients) sub-resource ──

    /**
     * GET /v1/auth/iot/event-rules/{id}/recipients — 取得通知對象
     */
    @GetMapping("/{id}/recipients")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<List<EventNotificationTargetResponse>> getRecipients(@PathVariable Long id) {
        return BaseResponse.success(eventRuleService.getRecipients(id));
    }

    /**
     * PUT /v1/auth/iot/event-rules/{id}/recipients — 全量替換通知對象
     */
    @PutMapping("/{id}/recipients")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_EVENT_RULE)
    public BaseResponse<List<EventNotificationTargetResponse>> updateRecipients(
            @PathVariable Long id,
            @Valid @RequestBody List<EventNotificationTargetRequest> requests) {
        return BaseResponse.success(eventRuleService.updateRecipients(id, requests));
    }

    // ── 通知管道 (channels) sub-resource ──

    /**
     * GET /v1/auth/iot/event-rules/{id}/channels — 取得通知管道
     */
    @GetMapping("/{id}/channels")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<List<EventNotificationChannelResponse>> getChannels(@PathVariable Long id) {
        return BaseResponse.success(eventRuleService.getChannels(id));
    }

    /**
     * PUT /v1/auth/iot/event-rules/{id}/channels — 全量替換通知管道
     */
    @PutMapping("/{id}/channels")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_EVENT_RULE)
    public BaseResponse<List<EventNotificationChannelResponse>> updateChannels(
            @PathVariable Long id,
            @Valid @RequestBody List<EventNotificationChannelRequest> requests) {
        return BaseResponse.success(eventRuleService.updateChannels(id, requests));
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
