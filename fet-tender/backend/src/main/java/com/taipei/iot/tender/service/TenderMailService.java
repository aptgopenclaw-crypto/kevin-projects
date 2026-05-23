package com.taipei.iot.tender.service;

import com.taipei.iot.tender.config.TenderMailProperties;
import com.taipei.iot.tender.dto.AwardScrapeResult;
import com.taipei.iot.tender.dto.TenderScrapeResult;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 招標日報郵件寄送服務。
 * HTML 內文格式與 Python mail_sender.py build_summary_html 一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenderMailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final JavaMailSender mailSender;
    private final TenderMailProperties mailProps;
    private final TenderExcelExporter excelExporter;
    private final TenderAwardExcelExporter awardExcelExporter;
    private final MailRecipientService mailRecipientService;

    /**
     * 寄送招標日報郵件，含 Excel 附件。
     *
     * @param result 爬蟲結果（含筆數、按 solution 分類的計數、公告清單）
     */
    public void sendReport(TenderScrapeResult result) {
        List<String> recipients = resolveRecipients();
        if (recipients.isEmpty()) {
            log.warn("[TenderMail] 未設定收件人，跳過寄信");
            return;
        }

        String today = LocalDate.now().format(DATE_FMT);
        String subject = String.format("【招標日報】%s 政府採購網招標資料 (%d 筆)", today, result.total());

        try {
            // 產生 Excel 附件
            byte[] excelBytes = excelExporter.export(result.announcements());
            String filename = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "招標資料.xlsx";

            // 取得寄件者 email
            String fromAddress = ((JavaMailSenderImpl) mailSender).getUsername();
            String fromDisplay = mailProps.getAlias();

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress, fromDisplay);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(buildHtml("招標", result.solutionKeyCounts(), result.total(), today), true);
            helper.addAttachment(filename, () -> new java.io.ByteArrayInputStream(excelBytes),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            mailSender.send(msg);
            log.info("[TenderMail] 郵件寄送成功 → {} ({})", recipients, subject);

        } catch (Exception e) {
            log.error("[TenderMail] 郵件寄送失敗", e);
        }
    }

    /**
     * 寄送決標日報郵件。
     *
     * @param result 決標爬蟲結果
     */
    public void sendAwardReport(AwardScrapeResult result) {
        List<String> recipients = resolveRecipients();
        if (recipients.isEmpty()) {
            log.warn("[TenderMail] 未設定收件人，跳過寄信");
            return;
        }

        String today = LocalDate.now().format(DATE_FMT);
        String subject = String.format("【決標日報】%s 政府採購網決標資料 (%d 筆廠商)", today, result.total());

        try {
            byte[] excelBytes = awardExcelExporter.export(result.awards());
            String filename = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "決標資料.xlsx";

            String fromAddress = ((JavaMailSenderImpl) mailSender).getUsername();
            String fromDisplay = mailProps.getAlias();

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress, fromDisplay);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(buildHtml("決標", result.solutionKeyCounts(), result.total(), today), true);
            helper.addAttachment(filename, () -> new java.io.ByteArrayInputStream(excelBytes),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            mailSender.send(msg);
            log.info("[TenderMail] 決標郵件寄送成功 → {} ({})", recipients, subject);

        } catch (Exception e) {
            log.error("[TenderMail] 決標郵件寄送失敗", e);
        }
    }

    // ── HTML 內文 ─────────────────────────────────────────────────────────────

    private String buildHtml(String type, Map<String, Map<String, Integer>> solutionKeyCounts, int total, String today) {
        StringBuilder rows = new StringBuilder();

        // 依各 solution 的總筆數降冪排序
        solutionKeyCounts.entrySet().stream()
                .sorted((a, b) -> {
                    int sumA = a.getValue().values().stream().mapToInt(Integer::intValue).sum();
                    int sumB = b.getValue().values().stream().mapToInt(Integer::intValue).sum();
                    return Integer.compare(sumB, sumA);
                })
                .forEach(entry -> {
                    String solution = entry.getKey();
                    Map<String, Integer> keyCounts = entry.getValue();
                    int solutionTotal = keyCounts.values().stream().mapToInt(Integer::intValue).sum();
                    String kwDisplay  = String.join(", ",
                            keyCounts.keySet().stream().map(k -> "\"" + k + "\"").toList());

                    String color = solutionTotal > 0 ? "#333" : "#999";
                    String bold  = solutionTotal > 0 ? "font-weight:bold;" : "";

                    rows.append(String.format("""
                            <tr>
                              <td style="padding:6px 12px;border:1px solid #ddd;%scolor:%s">%s</td>
                              <td style="padding:6px 12px;border:1px solid #ddd;%scolor:%s;font-size:12px">%s</td>
                              <td style="padding:6px 12px;border:1px solid #ddd;text-align:right;%scolor:%s">%d 筆</td>
                            </tr>
                            """, bold, color, solution,
                            bold, color, kwDisplay,
                            bold, color, solutionTotal));
                });

        return String.format("""
                <html>
                <body style="font-family:'Microsoft JhengHei',Arial,sans-serif;color:#333">
                  <h2 style="color:#2e6da4">&#x1F4CB; 政府採購網%s資料日報</h2>
                  <p>查詢日期：<b>%s</b></p>
                  <table style="border-collapse:collapse;margin:16px 0">
                    <thead>
                      <tr style="background:#2e6da4;color:#fff">
                        <th style="padding:8px 12px;border:1px solid #ddd;text-align:left">相關產品</th>
                        <th style="padding:8px 12px;border:1px solid #ddd;text-align:left">關鍵字</th>
                        <th style="padding:8px 12px;border:1px solid #ddd;text-align:right">筆數</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr style="background:#f5f5f5;font-weight:bold">
                        <td style="padding:8px 12px;border:1px solid #ddd" colspan="2">合計</td>
                        <td style="padding:8px 12px;border:1px solid #ddd;text-align:right">%d 筆</td>
                      </tr>
                      %s
                    </tbody>
                  </table>
                  <p style="color:#888;font-size:12px">此為系統自動寄送，請至系統查看完整資料。</p>
                </body>
                </html>
                """, type, today, total, rows);
    }

    // ── 收件人解析：DB 優先，yml fallback ─────────────────────────────────────

    private List<String> resolveRecipients() {
        List<String> dbRecipients = mailRecipientService.getActiveEmails();
        if (dbRecipients != null && !dbRecipients.isEmpty()) {
            return dbRecipients;
        }
        // fallback: 舊設定檔 recipients
        List<String> configRecipients = mailProps.getRecipients();
        return configRecipients != null ? configRecipients : List.of();
    }
}
