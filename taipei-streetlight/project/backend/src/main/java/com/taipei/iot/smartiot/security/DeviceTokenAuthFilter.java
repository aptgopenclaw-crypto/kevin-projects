package com.taipei.iot.smartiot.security;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * IoT 設備認證 Filter。
 * 從 HTTP Header {@code X-Device-Token} 取 token，查詢 devices 表驗證。
 * 僅攔截 /v1/iot/** 路徑；其他路徑不處理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(DeviceRepository.class)
public class DeviceTokenAuthFilter extends OncePerRequestFilter {

    private final DeviceRepository deviceRepository;

    @Value("${iot.device-token.header-name:X-Device-Token}")
    private String headerName;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 僅處理 /v1/iot/** 路徑
        return !request.getRequestURI().startsWith("/v1/iot/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(headerName);

        if (token != null && !token.isBlank()) {
            Optional<Device> deviceOpt = deviceRepository.findByDeviceToken(token);

            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();

                // 建立 Authentication — principal = "device:{id}", authority = IOT_DEVICE
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                "device:" + device.getId(),
                                null,
                                List.of(new SimpleGrantedAuthority("IOT_DEVICE")));

                Map<String, Object> details = new HashMap<>();
                details.put("deviceId", device.getId());
                details.put("tenantId", device.getTenantId());
                auth.setDetails(details);

                SecurityContextHolder.getContext().setAuthentication(auth);
                TenantContext.setCurrentTenantId(device.getTenantId());
            } else {
                log.warn("[IoT-Auth] Invalid device token from {}", request.getRemoteAddr());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
