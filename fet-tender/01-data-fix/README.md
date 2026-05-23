# 招標/決標資料補充工具（多租戶版）

補爬歷史招標公告或決標公告資料到指定租戶的資料表。

- `--type=announcement` → 寫入 `tender_announcement`
- `--type=award` → 寫入 `tender_award`

## 前置準備

```bash
# 安裝 Playwright browser（只需一次）
cd 01-data-fix
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## 使用方式

```bash
# 補招標公告資料
mvn spring-boot:run -Dspring-boot.run.arguments="--type=announcement --tenant=FET --from=2026-01-01 --to=2026-05-22"

# 補決標資料
mvn spring-boot:run -Dspring-boot.run.arguments="--type=award --tenant=FET --from=2026-01-01 --to=2026-05-22"

# fat jar 執行（推薦）
mvn package -DskipTests
java -jar target/fix-data-1.0.0.jar --type=announcement --tenant=FET --from=2026-05-01 --to=2026-05-22
```

## 參數說明

| 參數 | 必填 | 說明 |
|------|------|------|
| `--type` | ❌ | `announcement`（招標公告）或 `award`（決標公告），預設 `award` |
| `--tenant` | ✅ | 租戶 ID（對應 `tenant` 表的 `tenant_id`） |
| `--from` | ✅ | 起始日期（yyyy-MM-dd） |
| `--to` | ✅ | 結束日期（yyyy-MM-dd） |

## 注意事項

- DB schema 為 `fet_tenderdb`，需與 backend 共用同一資料庫
- 程式會讀取該租戶在 `announcement_search_keywords` / `announcement_agency_filters` 設定的關鍵字進行爬取
- 資料以 upsert 方式寫入，重複執行不會產生重複資料
- 不啟動 Web Server，執行完畢自動結束
- 中斷後可從 log 提示的日期繼續（調整 `--from`）