package com.taipei.iot.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 將 Vue Router（HTML5 history mode）路徑轉發至 /index.html。
 *
 * <p>實作策略：
 * <ul>
 *     <li>單段、不含副檔名的路徑（如 /login、/dashboard）由 {@link #forwardSingleSegment()} 直接攔截。</li>
 *     <li>多段路徑（如 /admin/users、/tender/detail/123）會走到 Spring Boot 的 /error 流程，
 *         由 {@link #handleError(HttpServletRequest)} 判斷：若為 404、非 API 路徑、且不含副檔名，
 *         則 forward 至 /index.html，交給前端 Router 解析。</li>
 *     <li>靜態資源（含 "."）由 ResourceHandler 從 classpath:/static/ 直接提供。</li>
 *     <li>API 路徑（/v1/**、/ws/**、/v3/api-docs、/swagger-ui、/actuator）維持原 JSON 錯誤回應。</li>
 * </ul>
 *
 * <p>注意：Spring 6 的 PathPattern parser 不允許 {@code /**} 之後接 {@code {...}}，
 * 因此不能直接用 {@code /**\/{path:[^\\.]*}} 攔截多段路徑，改採 ErrorController 方案。
 */
@Controller
public class SpaForwardingController implements ErrorController {

    /** 單段、不含副檔名的路徑。 */
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String forwardSingleSegment() {
        return "forward:/index.html";
    }

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        int status = statusObj instanceof Integer ? (Integer) statusObj : 0;
        String uri = uriObj != null ? uriObj.toString() : "";

        if (status == HttpStatus.NOT_FOUND.value()
                && !uri.contains(".")
                && !isApiPath(uri)) {
            return "forward:/index.html";
        }
        // 其餘錯誤（API 4xx/5xx、靜態資源 404）維持 Spring Boot 預設行為
        return new org.springframework.http.ResponseEntity<>(
                java.util.Map.of(
                        "status", status,
                        "path", uri,
                        "error", HttpStatus.resolve(status) != null
                                ? HttpStatus.resolve(status).getReasonPhrase()
                                : "Error"
                ),
                HttpStatus.resolve(status) != null ? HttpStatus.resolve(status) : HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private boolean isApiPath(String uri) {
        return uri.startsWith("/v1/")
                || uri.startsWith("/ws/")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/actuator");
    }
}
