package com.taipei.iot.common.util;

import com.taipei.iot.common.enums.SecurityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 統一安全事件日誌工具類。
 * <p>
 * 所有安全相關事件（登入失敗、CAPTCHA 錯誤、JWT 異常、速率限制、403 存取拒絕等）
 * 均透過此工具類輸出，統一加上 {@code [SECURITY]} prefix，便於：
 * <ul>
 *   <li>{@code grep "[SECURITY]" security.log} 快速過濾攻擊行為</li>
 *   <li>設定告警規則（例如 5 分鐘內超過 50 筆 [SECURITY] → 通知）</li>
 *   <li>區分「正常使用者操作失敗」和「疑似攻擊行為」</li>
 * </ul>
 *
 * <p>Logger name 為 {@code SECURITY}，對應 logback-spring.xml 中的獨立 appender，
 * 安全事件會同時輸出到 console 和 {@code security.log}。
 */
public final class SecurityLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY");

    private SecurityLogger() {
    }

    /**
     * 清理日誌內容中的控制字元，防止 CRLF 日誌注入攻擊（CWE-117）。
     *
     * <h3>為什麼需要這個？</h3>
     * <p>日誌檔是純文字，換行符號（{@code \r\n}）就是「下一筆日誌」的分隔符號。
     * 如果攻擊者能在輸入欄位（如 email、password）中插入 {@code %0d%0a}（URL 編碼的 \r\n），
     * 就能在日誌中偽造任意內容：</p>
     * <pre>
     *   // 攻擊者送出的 email：
     *   attacker@evil.com\r\n[SECURITY] LOGIN_FAILED ip=192.168.1.50 email=admin@company.com
     *
     *   // 日誌中會變成兩筆，第二筆是偽造的：
     *   [SECURITY] LOGIN_FAILED ip=1.2.3.4 email=attacker@evil.com
     *   [SECURITY] LOGIN_FAILED ip=192.168.1.50 email=admin@company.com  ← 假的！
     * </pre>
     * <p>這會導致：嫁禍他人、淹沒真實攻擊紀錄、誤導安全調查、污染 ELK/Grafana 索引。</p>
     *
     * @param input 任何要寫入日誌的外部輸入值
     * @return 移除 \r \n \t 後的安全字串；null 輸入回傳 "null"
     */
    private static String sanitize(String input) {
        if (input == null) {
            return "null";
        }
        // 移除所有可能造成日誌注入的控制字元：
        // \r（Carriage Return）、\n（Line Feed）→ 換行注入
        // \t（Tab）→ 可能破壞日誌欄位對齊，干擾解析器
        return input.replace("\r", "").replace("\n", "").replace("\t", "");
    }

    /**
     * 記錄安全警告事件。
     *
     * @param event   安全事件類型
     * @param ip      來源 IP
     * @param details 額外的 key=value 資訊（例如 "email=test@example.com", "reason=bad_password"）
     */
    public static void warn(SecurityEvent event, String ip, String... details) {
        log.warn("[SECURITY] {} ip={} {}", event.name(), sanitize(ip), sanitizeDetails(details));
    }

    /**
     * 記錄安全資訊事件（低風險但需記錄，例如密碼重設請求）。
     *
     * @param event   安全事件類型
     * @param ip      來源 IP
     * @param details 額外的 key=value 資訊
     */
    public static void info(SecurityEvent event, String ip, String... details) {
        log.info("[SECURITY] {} ip={} {}", event.name(), sanitize(ip), sanitizeDetails(details));
    }

    /**
     * 對所有 detail 值逐一清理後合併。
     */
    private static String sanitizeDetails(String... details) {
        if (details == null || details.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < details.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(sanitize(details[i]));
        }
        return sb.toString();
    }
}
