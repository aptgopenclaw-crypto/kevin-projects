package com.taipei.iot.announcement.controller;

import com.taipei.iot.announcement.dto.AnnouncementRequest;
import com.taipei.iot.announcement.dto.AnnouncementResponse;
import com.taipei.iot.announcement.dto.UnreadCountResponse;
import com.taipei.iot.announcement.service.AnnouncementReadService;
import com.taipei.iot.announcement.service.AnnouncementService;
import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.common.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/v1/auth/announcements")
@RequiredArgsConstructor
@Validated
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AnnouncementReadService announcementReadService;

    /**
     * 前台查詢：已發佈 + 未過期 + 受眾範圍符合的公告
     */
    @GetMapping
    public BaseResponse<PageResponse<AnnouncementResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return BaseResponse.success(announcementService.listVisible(page, size));
    }

    /**
     * 管理端查詢：需要 ANNOUNCEMENT_MANAGE 權限
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public BaseResponse<PageResponse<AnnouncementResponse>> listAdmin(
            @RequestParam(defaultValue = "ALL") String statusFilter,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return BaseResponse.success(announcementService.listAdmin(statusFilter, keyword, page, size));
    }

    /**
     * 取得單筆公告詳情
     */
    @GetMapping("/{id}")
    public BaseResponse<AnnouncementResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(announcementService.getById(id, hasManagePermission()));
    }

    /**
     * 取得未讀公告數量
     */
    @GetMapping("/unread-count")
    public BaseResponse<UnreadCountResponse> getUnreadCount() {
        return BaseResponse.success(announcementReadService.getUnreadCount());
    }

    /**
     * 標記某則公告為已讀
     */
    @PostMapping("/{id}/read")
    public BaseResponse<Void> markAsRead(@PathVariable Long id) {
        announcementReadService.markAsRead(id);
        return BaseResponse.success(null);
    }

    /**
     * 全部標為已讀
     */
    @PostMapping("/read-all")
    public BaseResponse<Void> markAllAsRead() {
        announcementReadService.markAllAsRead();
        return BaseResponse.success(null);
    }

    /**
     * 新增公告
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_ANNOUNCEMENT)
    public BaseResponse<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
        return BaseResponse.success(announcementService.create(request));
    }

    /**
     * 編輯公告
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_ANNOUNCEMENT)
    public BaseResponse<AnnouncementResponse> update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return BaseResponse.success(announcementService.update(id, request));
    }

    /**
     * 刪除公告
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    @AuditEvent(AuditEventType.DELETE_ANNOUNCEMENT)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return BaseResponse.success(null);
    }

    private boolean hasManagePermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ANNOUNCEMENT_MANAGE"));
    }
}
