package com.taipei.iot.user.controller;

import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.common.annotation.RateLimit;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.user.dto.request.ChangePasswordRequest;
import com.taipei.iot.user.dto.request.UpdateOwnProfileRequest;
import com.taipei.iot.user.service.UserSelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/user")
@RequiredArgsConstructor
public class UserSelfController {

    private final UserSelfService userSelfService;

    @PutMapping("/my")
    public BaseResponse<UserEntity> updateOwnProfile(Authentication authentication,
                                                      @RequestBody UpdateOwnProfileRequest req) {
        String userId = (String) authentication.getPrincipal();
        UserEntity updated = userSelfService.updateOwnProfile(userId, req);
        return BaseResponse.success(updated);
    }

    @PostMapping("/change-password")
    @RateLimit(key = "change-password", limit = 5, period = 300)
    public BaseResponse<Void> changePassword(Authentication authentication,
                                              @Valid @RequestBody ChangePasswordRequest req) {
        String userId = (String) authentication.getPrincipal();
        userSelfService.changePassword(userId, req);
        return BaseResponse.success(null);
    }
}
