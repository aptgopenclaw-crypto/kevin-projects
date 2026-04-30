package com.taipei.iot.smartiot.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * IoT 設備認證 Security 配置。
 * 獨立於主 SecurityConfig，僅在 DeviceTokenAuthFilter bean 存在時啟用。
 * 這確保 @WebMvcTest 不會因為缺少 DeviceRepository 而失敗。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(DeviceTokenAuthFilter.class)
public class IoTSecurityConfig {

    private final DeviceTokenAuthFilter deviceTokenAuthFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain iotFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/v1/iot/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .addFilterBefore(deviceTokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
