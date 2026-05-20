package com.taipei.iot.fixdata;

import com.taipei.iot.tender.dto.AwardScrapeResult;
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

/**
 * 決標歷史資料補充工具。
 *
 * 用法：
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--from=2026-01-01 --to=2026-05-13"
 * 或直接執行 fat jar：
 *   java -jar fix-data.jar --from=2026-01-01 --to=2026-05-13
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

    private final TenderAwardScraperService scraperService;

    public static void main(String[] args) {
        SpringApplication.run(FixDataApplication.class, args);
    }

    @Override
    public void run(String... args) {
        LocalDate from = null;
        LocalDate to   = null;

        for (String arg : args) {
            if (arg.startsWith("--from=")) {
                from = LocalDate.parse(arg.substring("--from=".length()));
            } else if (arg.startsWith("--to=")) {
                to = LocalDate.parse(arg.substring("--to=".length()));
            }
        }

        if (from == null || to == null) {
            log.error("用法：--from=yyyy-MM-dd --to=yyyy-MM-dd");
            log.error("範例：--from=2026-01-01 --to=2026-05-13");
            System.exit(1);
        }

        if (from.isAfter(to)) {
            log.error("--from ({}) 不能晚於 --to ({})", from, to);
            System.exit(1);
        }

        log.info("==================================================");
        log.info("  決標資料補充工具  {}  ~  {}", from, to);
        log.info("==================================================");

        AwardScrapeResult result = scraperService.runAndImport(from, to);

        log.info("==================================================");
        log.info("  完成：共 upsert {} 筆廠商記錄", result.total());
        log.info("==================================================");
    }
}
