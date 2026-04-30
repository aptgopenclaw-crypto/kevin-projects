package com.taipei.iot.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantProperties tenantProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("single".equals(tenantProperties.getMode())) {
            // single 模式：強制覆蓋 JWT Filter 可能已寫入的 tenantId。
            // 這是預期行為：單機环境下所有操作統一指向 defaultId。
            TenantContext.setCurrentTenantId(tenantProperties.getDefaultId());
        }
        // multi 模式：由 AUTH 的 JwtAuthenticationFilter 從 JWT 設定
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
