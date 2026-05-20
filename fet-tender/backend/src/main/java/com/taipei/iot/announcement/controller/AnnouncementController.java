package com.taipei.iot.announcement.controller;

import com.taipei.iot.announcement.dto.AnnouncementRequest;
import com.taipei.iot.announcement.dto.AnnouncementResponse;
import com.taipei.iot.announcement.dto.UnreadCountResponse;
import com.taipei.iot.announcement.service.AnnouncementReadService;
import com.taipei.iot.announcement.service.AnnouncementService;
import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AnnouncementReadService announcementReadService;

    /**
     * 查詢公告（一般使用者：前台可見公告；管理員帶 admin=true：管理頁面全部公告）
     */
    @GetMapping
    public BaseResponse<PageResponse<AnnouncementResponse>> list(
            @RequestParam(defaultValue = "false") boolean admin,
            @RequestParam(defaultValue = "ALL") String statusFilter,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<AnnouncementResponse> result;
        if (admin) {
            // admin=true 時，嚴格驗證權限；無權限直接回 403，不做靜默降級
            if (!hasManagePermission()) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED);
            }
            // ADMIN/SUPER_ADMIN：看全部公告（依 statusFilter / keyword 篩選）
            // DEPT_ADMIN：看自己建立的 + 受眾含自己部門的 DEPT 公告
            result = announcementService.listAdmin(statusFilter, keyword, page, size);
        } else {
            // 前台查詢：status=PUBLISHED + publishAt<=now + 未過期 + scope=ALL 或部門符合
            result = announcementService.listVisible(page, size);
        }
        return BaseResponse.success(result);
    }

    private boolean hasManagePermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ROLE_DEPT_ADMIN")
                        || a.getAuthority().equals("ANNOUNCEMENT_MANAGE"));
    }

    /**
     * 取得單筆公告詳情
     */
    @GetMapping("/{id}")
    public BaseResponse<AnnouncementResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(announcementService.getById(id));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN') or hasAuthority('ANNOUNCEMENT_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_ANNOUNCEMENT)
    public BaseResponse<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
        return BaseResponse.success(announcementService.create(request));
    }

    /**
     * 編輯公告
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN') or hasAuthority('ANNOUNCEMENT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_ANNOUNCEMENT)
    public BaseResponse<AnnouncementResponse> update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return BaseResponse.success(announcementService.update(id, request));
    }

    /**
     * 刪除公告
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN') or hasAuthority('ANNOUNCEMENT_MANAGE')")
    @AuditEvent(AuditEventType.DELETE_ANNOUNCEMENT)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return BaseResponse.success(null);
    }
}
