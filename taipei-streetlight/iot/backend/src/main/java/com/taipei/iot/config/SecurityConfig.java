package com.taipei.iot.config;

import com.taipei.iot.auth.security.JwtAuthenticationFilter;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.enums.SecurityEvent;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.common.util.SecurityLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())   // 委派給 WebMvcConfig.addCorsMappings()；確保 preflight OPTIONS 不被 Security 攔截
                .csrf(csrf -> csrf.disable())
                // ====== 安全 Headers 設定 ======
                .headers(headers -> headers
                        // HSTS (HTTP Strict Transport Security)
                        // 沒設 → 攻擊者可用中間人攻擊（MITM）將 HTTPS 降級為 HTTP，竊聽或篡改傳輸中的資料
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))  // 1 年
                        // CSP (Content Security Policy)
                        // 沒設 → 攻擊者可注入外部惡意 script/style/iframe，執行 XSS 攻擊竊取使用者 cookie 或操作頁面
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://wmts.nlsc.gov.tw; font-src 'self' data:"))
                        // Referrer-Policy
                        // 沒設 → 瀏覽器跳轉到外部網站時，完整的 URL（可能包含 token、session ID）會透過 Referer header 洩漏給第三方
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Permissions-Policy
                        // 沒設 → 惡意第三方 iframe 或 script 可以偷偷啟用攝影機、麥克風、定位等敏感裝置 API
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(), payment=()"))
                        // X-Frame-Options（已有 DENY，保持不變）
                        // 沒設 → 攻擊者可用 iframe 嵌入你的頁面，誘導使用者點擊隱藏按鈕（Clickjacking 點擊劫持）
                        // X-Content-Type-Options（Spring Security 預設已加 nosniff，保持不變）
                        // 沒設 → 瀏覽器可能「猜測」檔案類型，把惡意上傳的檔案當成 JS 執行（MIME Sniffing 攻擊）
                )
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            String body = objectMapper.writeValueAsString(
                                    BaseResponse.fail(ErrorCode.ACCESS_TOKEN_INVALID));
                            response.getWriter().write(body);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            SecurityLogger.warn(SecurityEvent.ACCESS_DENIED, request.getRemoteAddr(),
                                    "path=" + request.getRequestURI(),
                                    "user=" + (request.getUserPrincipal() != null
                                            ? request.getUserPrincipal().getName() : "anonymous"));
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            String body = objectMapper.writeValueAsString(
                                    BaseResponse.fail(ErrorCode.PERMISSION_DENIED));
                            response.getWriter().write(body);
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/noauth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // WebSocket handshake (auth via STOMP headers)
                        .requestMatchers("/ws/**").permitAll()
                        // USER: self-service (authenticated)
                        .requestMatchers("/v1/auth/user/my", "/v1/auth/user/change-password").authenticated()
                        // USER: admin — role or permission-based
                        // 細部權限由各方法的 @PreAuthorize 控制（如 tenant-roles 限 SUPER_ADMIN）
                        .requestMatchers("/v1/auth/users/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_DEPT_ADMIN", "USER_LIST")
                        // RBAC: roles/permissions query (ADMIN, SUPER_ADMIN, or DEPT_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/v1/auth/roles/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/auth/permissions/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // RBAC: menus — user visible menus (authenticated)
                        .requestMatchers(HttpMethod.GET, "/v1/auth/menus/my").authenticated()
                        // RBAC: menus — admin tree view (ADMIN or SUPER_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/v1/auth/menus/tree").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // RBAC: menus CRUD (SUPER_ADMIN only)
                        .requestMatchers(HttpMethod.POST, "/v1/auth/menus/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/v1/auth/menus/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/auth/menus/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/v1/auth/menus/**").hasRole("SUPER_ADMIN")
                        // AUDIT: personal log (authenticated)
                        .requestMatchers("/v1/auth/audit/user/login/my").authenticated()
                        // AUDIT: admin queries — role or permission-based
                        .requestMatchers("/v1/auth/audit/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "AUDIT_LIST")
                        // DEPT: tree list requires DEPT_LIST or admin role
                        .requestMatchers(HttpMethod.GET, "/v1/auth/dept/list").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_DEPT_ADMIN", "DEPT_LIST")
                        // DEPT: options / single dept (authenticated — used by dropdowns)
                        .requestMatchers(HttpMethod.GET, "/v1/auth/dept/options").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/auth/dept/scope-options").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/auth/dept/{deptId}").authenticated()
                        // DEPT: CRUD is controlled by @PreAuthorize on controller
                        // LOG-SUMMARY: all GET endpoints (authenticated)
                        .requestMatchers(HttpMethod.GET, "/v1/log/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
