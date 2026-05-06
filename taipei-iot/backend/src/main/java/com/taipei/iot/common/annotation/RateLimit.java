package com.taipei.iot.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 速率限制註解 — 標註在 Controller 方法上，限制同一 IP 在指定時間窗口內的請求次數。
 *
 * <p>使用範例：
 * <pre>
 * // 同一 IP 每分鐘最多 10 次請求
 * &#64;RateLimit(key = "login", limit = 10, period = 60)
 * &#64;PostMapping("/login")
 * public BaseResponse&lt;?&gt; login(...) { ... }
 * </pre>
 *
 * <p>底層透過 Redis INCR + EXPIRE 實作滑動窗口計數，由 {@code RateLimitInterceptor} 攔截處理。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key 前綴（與 IP 組合成 Redis key：rate_limit:{key}:{ip}）
     */
    String key();

    /**
     * 時間窗口內允許的最大請求次數
     */
    int limit() default 10;

    /**
     * 時間窗口長度（秒）
     */
    int period() default 60;
}
