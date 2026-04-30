package com.taipei.iot.auth.service;

import com.taipei.iot.auth.dto.request.ChangePasswordRequest;
import com.taipei.iot.auth.dto.request.ForgotPasswordRequest;
import com.taipei.iot.auth.dto.request.LoginRequest;
import com.taipei.iot.auth.dto.request.ResetPasswordRequest;
import com.taipei.iot.auth.dto.request.SelectTenantRequest;
import com.taipei.iot.auth.dto.request.SwitchTenantRequest;
import com.taipei.iot.auth.dto.response.LoginResult;
import com.taipei.iot.auth.dto.response.TokenResult;
import com.taipei.iot.auth.dto.response.UserInfoDto;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    LoginResult login(LoginRequest request, HttpServletRequest httpRequest);
    TokenResult selectTenant(String userId, SelectTenantRequest request, HttpServletRequest httpRequest);
    TokenResult switchTenant(String userId, SwitchTenantRequest request, HttpServletRequest httpRequest);
    TokenResult refreshToken(String refreshToken);
    void logout(String refreshToken);
    UserInfoDto getCurrentUser(String userId, String tenantId);
    void changePassword(String userId, ChangePasswordRequest request, HttpServletRequest httpRequest);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest);
}
