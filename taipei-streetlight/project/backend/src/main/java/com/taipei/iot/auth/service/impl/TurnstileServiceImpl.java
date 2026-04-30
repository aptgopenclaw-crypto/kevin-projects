package com.taipei.iot.auth.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taipei.iot.auth.service.TurnstileService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cloudflare Turnstile 驗證服務實作。
 *
 * <p>驗證流程：
 * <ol>
 *   <li>前端載入 Turnstile JS widget，使用者完成驗證後取得 token</li>
 *   <li>前端在登入請求中附上 token（turnstileToken 欄位）</li>
 *   <li>後端收到 token 後，呼叫 Cloudflare siteverify API 驗證</li>
 *   <li>Cloudflare 回傳 success: true/false</li>
 * </ol>
 *
 * <p>設定方式（application.yml）：
 * <pre>
 * captcha:
 *   turnstile:
 *     secret-key: ${TURNSTILE_SECRET_KEY:}   # Cloudflare Dashboard 取得
 *     site-key: ${TURNSTILE_SITE_KEY:}       # 給前端用
 * </pre>
 *
 * <p>如果未設定 secret-key，{@link #isEnabled()} 回傳 false，系統退回使用圖片驗證碼。
 */
@Slf4j
@Service
public class TurnstileServiceImpl implements TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Value("${captcha.turnstile.secret-key:}")
    private String secretKey;

    private final RestClient restClient;

    public TurnstileServiceImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean verify(String token, String remoteIp) {
        if (!isEnabled()) {
            log.warn("Turnstile 未啟用（未設定 secret-key），拒絕驗證");
            return false;
        }

        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secretKey);
            form.add("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                form.add("remoteip", remoteIp);
            }

            TurnstileResponse result = restClient.post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileResponse.class);

            if (result == null) {
                log.error("Turnstile API 回傳 null");
                return false;
            }

            if (!result.isSuccess()) {
                log.warn("Turnstile 驗證失敗 — errorCodes: {}", result.getErrorCodes());
            }

            return result.isSuccess();
        } catch (Exception e) {
            log.error("Turnstile API 呼叫失敗", e);
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return secretKey != null && !secretKey.isBlank();
    }

    /**
     * Cloudflare siteverify API 的回應格式。
     *
     * @see <a href="https://developers.cloudflare.com/turnstile/get-started/server-side-validation/">Turnstile Server-side Validation</a>
     */
    @Data
    static class TurnstileResponse {
        private boolean success;

        @JsonProperty("error-codes")
        private List<String> errorCodes;

        @JsonProperty("challenge_ts")
        private String challengeTs;

        private String hostname;
    }
}
