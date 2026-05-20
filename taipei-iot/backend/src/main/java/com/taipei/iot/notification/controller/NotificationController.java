package com.taipei.iot.notification.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.notification.dto.NotificationResponse;
import com.taipei.iot.notification.dto.UnreadCountResponse;
import com.taipei.iot.notification.service.NotificationService;
import com.taipei.iot.common.dto.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public BaseResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        String userId = SecurityContextUtils.getCurrentUserId();
        return BaseResponse.success(notificationService.list(userId, page, size));
    }

    @GetMapping("/todos")
    public BaseResponse<PageResponse<NotificationResponse>> listTodos(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        String userId = SecurityContextUtils.getCurrentUserId();
        return BaseResponse.success(notificationService.listTodos(userId, page, size));
    }

    @GetMapping("/unread-count")
    public BaseResponse<UnreadCountResponse> unreadCount() {
        String userId = SecurityContextUtils.getCurrentUserId();
        return BaseResponse.success(notificationService.unreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public BaseResponse<Void> markRead(@PathVariable Long id) {
        String userId = SecurityContextUtils.getCurrentUserId();
        notificationService.markRead(userId, id);
        return BaseResponse.success(null);
    }

    @PatchMapping("/read-all")
    public BaseResponse<Void> markAllRead() {
        String userId = SecurityContextUtils.getCurrentUserId();
        notificationService.markAllRead(userId);
        return BaseResponse.success(null);
    }
}
