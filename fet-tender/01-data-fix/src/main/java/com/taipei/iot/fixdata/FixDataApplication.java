package com.taipei.iot.fixdata;

import com.taipei.iot.tender.dto.AnnouncementScrapeResult;
import com.taipei.iot.tender.dto.AwardScrapeResult;
import com.taipei.iot.tender.service.TenderAnnouncementScraperService;
import com.taipei.iot.tender.service.TenderAwardScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDate;
import java.util.List;

/**
 * 招標/決標歷史資料補充工具（多租戶版）。
 *
 * 用法：
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--type=award --tenant=FET --from=2026-01-01 --to=2026-05-13"
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--type=announcement --tenant=FET --from=2026-01-01 --to=2026-05-13"
 * 或直接執行 fat jar：
 *   java -jar fix-data.jar --type=announcement --tenant=FET --from=2026-01-01 --to=2026-05-13
 *
 * 參數說明：
 *   --type    資料類型：announcement（招標公告）或 award（決標公告），預設 award
 *   --tenant  指定要補資料的租戶 ID（必填）
 *   --from    起始日期 yyyy-MM-dd（必填）
 *   --to      結束日期 yyyy-MM-dd（必填）
 *
 * 程式啟動後不開啟 Web Server，執行完畢自動結束（exit code 0）。
 */
@Slf4j
@SpringBootApplication(scanBasePackages = {"com.taipei.iot.fixdata", "com.taipei.iot.tender", "com.taipei.iot.common"})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.taipei.iot.tender.repository")
@EntityScan(basePackages = {"com.taipei.iot.tender.entity", "com.taipei.iot.common.entity"})
@RequiredArgsConstructor
public class FixDataApplication implements CommandLineRunner {

    private final TenderAwardScraperService awardScraperService;
    private final TenderAnnouncementScraperService announcementScraperService;

    public static void main(String[] args) {
        SpringApplication.run(FixDataApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String type = "award";
        String tenantId = null;
        LocalDate from = null;
        LocalDate to   = null;

        for (String arg : args) {
            if (arg.startsWith("--type=")) {
                type = arg.substring("--type=".length());
            } else if (arg.startsWith("--tenant=")) {
                tenantId = arg.substring("--tenant=".length());
            } else if (arg.startsWith("--from=")) {
                from = LocalDate.parse(arg.substring("--from=".length()));
            } else if (arg.startsWith("--to=")) {
                to = LocalDate.parse(arg.substring("--to=".length()));
            }
        }

        if (tenantId == null || tenantId.isBlank()) {
            log.error("用法：--type=announcement|award --tenant=<TENANT_ID> --from=yyyy-MM-dd --to=yyyy-MM-dd");
            log.error("範例：--type=announcement --tenant=FET --from=2026-01-01 --to=2026-05-22");
            System.exit(1);
        }

        if (from == null || to == null) {
            log.error("用法：--type=announcement|award --tenant=<TENANT_ID> --from=yyyy-MM-dd --to=yyyy-MM-dd");
            log.error("範例：--type=award --tenant=FET --from=2026-01-01 --to=2026-05-22");
            System.exit(1);
        }

        if (from.isAfter(to)) {
            log.error("--from ({}) 不能晚於 --to ({})", from, to);
            System.exit(1);
        }

        if (!List.of("announcement", "award").contains(type)) {
            log.error("--type 必須為 announcement 或 award，目前值: {}", type);
            System.exit(1);
        }

        String typeLabel = "award".equals(type) ? "決標" : "招標公告";
        log.info("==================================================");
        log.info("  {}資料補充工具（多租戶版）", typeLabel);
        log.info("  租戶: {}  日期: {} ~ {}", tenantId, from, to);
        log.info("==================================================");

        if ("announcement".equals(type)) {
            announcementScraperService.setTenantId(tenantId);
            AnnouncementScrapeResult result = announcementScraperService.runAndImport(from, to);
            log.info("==================================================");
            log.info("  完成：租戶 {} 共 upsert {} 筆招標公告", tenantId, result.total());
            log.info("==================================================");
        } else {
            awardScraperService.setTenantId(tenantId);
            AwardScrapeResult result = awardScraperService.runAndImport(from, to);
            log.info("==================================================");
            log.info("  完成：租戶 {} 共 upsert {} 筆決標廠商記錄", tenantId, result.total());
            log.info("==================================================");
        }
    }
}
