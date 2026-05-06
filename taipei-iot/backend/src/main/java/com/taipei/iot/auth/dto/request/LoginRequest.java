package com.taipei.iot.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid address")
    private String email;

    @NotBlank(message = "password is required")
    private String password;

    /** 圖片驗證碼 — 使用圖片驗證時必填 */
    private String captcha;

    /** 圖片驗證碼 key — 使用圖片驗證時必填 */
    private String captchaKey;

    /** Cloudflare Turnstile token — 使用 Turnstile 驗證時必填 */
    private String turnstileToken;
}
