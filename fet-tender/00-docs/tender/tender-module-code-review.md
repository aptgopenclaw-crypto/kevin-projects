# Tender 模組 Code Review & Security Review

> 審核日期：2026-05-22（第二次審查）  
> 涵蓋範圍：`backend/src/main/java/com/taipei/iot/tender/**` + `frontend/src/views/tender/**` + `frontend/src/api/tender/`

---

## 一、模組架構總覽

```
tender/
├── ai/                         # AI 聊天（NVIDIA DeepSeek / Llama）
│   ├── NvidiaAiConfig.java
│   ├── NvidiaAiProperties.java
│   ├── TenderChatService.java
│   ├── TenderFunctionExecutor.java
│   └── TenderFunctionSchemas.java
├── config/
│   ├── TenderAwardScraperProperties.java
│   ├── TenderMailProperties.java
│   └── TenderScraperProperties.java
├── controller/
│   ├── AgencyFilterController.java
│   ├── MailRecipientController.java          ← 新增
│   ├── SearchKeywordController.java
│   ├── SolutionCompetitorController.java
│   ├── TenderAnnouncementController.java
│   ├── TenderAwardController.java
│   ├── TenderAwardScrapeController.java
│   ├── TenderChatController.java
│   ├── TenderScrapeController.java
│   └── VendorDashboardController.java
├── dto/                        # 請求/回應 DTO（23 個）
│   ├── MailRecipientRequest.java             ← 新增
│   ├── MailRecipientResponse.java            ← 新增
│   └── ... (21 其他 DTO)
├── entity/
│   ├── AnnouncementAgencyFilter.java
│   ├── AnnouncementSearchKeyword.java
│   ├── TenderAnnouncement.java
│   ├── TenderAward.java
│   └── TenderMailRecipient.java              ← 新增
├── job/
│   ├── TenderAwardScrapeJob.java
│   └── TenderScrapeJob.java
├── repository/
│   ├── AnnouncementAgencyFilterRepository.java
│   ├── AnnouncementSearchKeywordRepository.java
│   ├── TenderAnnouncementRepository.java
│   ├── TenderAwardRepository.java
│   └── TenderMailRecipientRepository.java    ← 新增
├── scraper/
│   ├── PccAwardDetailParser.java
│   ├── PccAwardListRow.java
│   ├── PccBrowserService.java
│   ├── PccDetailParser.java
│   └── PccListRow.java
└── service/
    ├── AgencyFilterService.java
    ├── MailRecipientService.java             ← 新增
    ├── SearchKeywordService.java
    ├── SolutionCompetitorService.java
    ├── TenderAnnouncementService.java
    ├── TenderAwardExcelExporter.java
    ├── TenderAwardScraperService.java
    ├── TenderAwardService.java
    ├── TenderExcelExporter.java
    ├── TenderMailService.java                ← 修改（DB 收件人）
    ├── TenderScraperService.java
    └── VendorDashboardService.java
```

前端：

```
frontend/src/
├── api/tender/index.ts                       （新增 mail-recipients API）
├── types/tender.ts                           （新增 MailRecipient 型別）
└── views/tender/
    ├── AgencyFilterView.vue                  （已加分頁 + solution dropdown）
    ├── MailRecipientView.vue                 ← 新增
    ├── SearchKeywordView.vue                 （已加分頁 + solution dropdown）
    ├── SolutionCompetitorView.vue
    ├── TenderAiChatView.vue
    ├── TenderAnnouncementView.vue
    ├── TenderAwardView.vue
    └── VendorDashboardView.vue
```

---

## 二、本次變更摘要（自上次 Review 以來）

| 變更項目 | 說明 |
|----------|------|
| **郵件收件人管理** | 新增 `tender_mail_recipient` 表 + Entity + Repo + Service + Controller + 前端 CRUD 頁面 |
| **TenderMailService 重構** | 從 `application.yml` 靜態設定改為 DB 優先 + yml fallback |
| **前端分頁** | SearchKeywordView、AgencyFilterView 均加入前端分頁（el-pagination） |
| **方案下拉** | SearchKeywordView、AgencyFilterView 改用 el-select（filterable + allow-create）取代文字輸入 |
| **選單整合** | V53 migration 新增「郵件收件人管理」選單於招標管理之下 |

---

## 三、Code Review 結果

### 3.1 優點

| 項目 | 說明 |
|------|------|
| **分層清晰** | Controller → Service → Repository 三層分明，無循環依賴 |
| **權限控制** | 所有 Controller 均使用 `@PreAuthorize` 細粒度權限 |
| **資源釋放** | `PccBrowserService` 實作 `AutoCloseable`，搭配 try-with-resources 確保 Playwright 資源釋放 |
| **去重設計** | 招標/決標均有 unique constraint + upsert 邏輯，不會因重複爬取產生髒資料 |
| **API 限制** | AI function executor 對 `size` 參數做了 `Math.min(size, 20)` 防止大量查詢；`days` 限制 90 天 |
| **頁面大小上限** | `VendorDashboardController.topAgencies()` 和 `SolutionCompetitorController.vendorRank()` 均有 `safeLimit/safeSize` 限制 |
| **DTO 隔離** | Entity 不直接暴露到 API 回應，統一透過 Response DTO 轉換 |
| **配置外部化** | cron、延遲、timeout 等均可透過 `application.yml` 覆蓋；API Key 正確使用環境變數 `${NVIDIA_AI_API_KEY:}` |
| **收件人動態化** | 郵件收件人從靜態 yml 改為 DB 管理，支援 tenant scope，保留 yml fallback 向下相容 |
| **輸入驗證** | `MailRecipientRequest` 使用 `@NotBlank` + `@Email`；email 正規化（trim + lowercase） |
| **前端 history 限制** | AI Chat 前端已做 `.slice(-20)` 限制最多帶 20 則歷史 |
| **Autocomplete debounce** | `VendorDashboardView.vue` 使用 `el-autocomplete` 內建 debounce（300ms） |
| **無 v-html** | AI Chat 渲染使用 `{{ }}` 插值，無 XSS 風險 |
| **Transaction 標註** | 新增的 `MailRecipientService` 正確標註 `@Transactional(readOnly = true)` |

### 3.2 問題與建議

#### P1 - 高優先（已全部修復 ✅）

| # | 問題 | 位置 | 修復方式 |
|---|------|------|----------|
| 1 | 分頁參數未校驗 | `TenderAnnouncementQueryRequest`, `TenderAwardQueryRequest` | ✅ 已加 `@Min(0)` page、`@Min(1) @Max(100)` size，Controller 加 `@Valid` |
| 2 | SolutionCompetitorController 缺少 @PreAuthorize | `SolutionCompetitorController.java` | ✅ 已加 `@PreAuthorize("hasAuthority('tender:award:view')")` 類別層級 |
| 3 | 爬蟲失敗回傳 success | `TenderScrapeController`, `TenderAwardScrapeController` | ✅ 改為 `BaseResponse.fail(ErrorCode.UNKNOWN_ERROR, ...)` |
| 4 | AI Chat history 後端無長度限制 | `TenderChatRequest` | ✅ message 加 `@Size(max=500)`，history 加 `@Size(max=20)` |

#### P2 - 中優先

| # | 問題 | 位置 | 狀態 | 說明 |
|---|------|------|------|------|
| 5 | **Native Query 可讀性差** | `TenderAwardRepository.java` | ⚪ 保留 | 各 query 實際 10-25 行，搭配 CTE 結構清晰。整份 490 行屬可接受範圍，暫不搬移 |
| 6 | **Excel Exporter 無串流輸出** | `TenderExcelExporter`, `TenderAwardExcelExporter` | ✅ 已修正 | 改用 `SXSSFWorkbook(100)` 串流模式，僅保留 100 列在記憶體，結束後 `dispose()` 清理暫存 |
| 7 | **重複的 Scraper 結構** | `TenderScraperService` vs `TenderAwardScraperService` | ⚪ 保留 | 兩服務迭代策略不同（直接 vs 日期區間）、回傳型別不同（單筆 vs 多廠商），抽象化風險大於收益 |
| 8 | **日期字串型別查詢** | `TenderAwardRepository.search()` | ⚪ 保留 | 程式碼已有註解說明原因：PostgreSQL 對 null LocalDate 參數無法推斷型別。CAST 為已知最佳解 |
| 9 | **Random 未最佳化** | `PccBrowserService.java` | ✅ 已修正 | 改用 `ThreadLocalRandom.current()` |
| 10 | **MailRecipientService 缺少 email 重複檢查提示** | `MailRecipientService.create()` | ✅ 已修正 | catch `DataIntegrityViolationException` 拋出 `BusinessException(ErrorCode.MAIL_RECIPIENT_EMAIL_DUPLICATE)` |

#### P3 - 低優先

| # | 問題 | 位置 | 狀態 | 說明 |
|---|------|------|------|------|
| 11 | **Job 缺少 distributed lock** | `TenderScrapeJob`, `TenderAwardScrapeJob` | ✅ 已修正 | 加入 ShedLock + Redis provider，`@SchedulerLock(lockAtLeastFor="5m", lockAtMostFor="60m")` |
| 12 | **缺少 Integration Test** | 整體 | ✅ 已修正 | 新增 `MailRecipientServiceTest`（8 tests）+ `TenderAnnouncementServiceTest`（5 tests），Mockito 單元測試 |
| 13 | **前端無 form validation** | `SearchKeywordView`, `AgencyFilterView` | ✅ 已修正 | 改用 `el-form :rules` + `formRef.validate()`，加入 required + maxlength 規則 |

---

## 四、Security Review 結果

### 4.1 OWASP Top 10 對照

| OWASP 類別 | 風險等級 | 狀態 | 說明 |
|------------|----------|------|------|
| **A01 - Broken Access Control** | � 低 | 已改善 | SolutionCompetitorController 已加入 `@PreAuthorize('tender:award:view')`；VendorDashboard 的 `suggest` 僅限有權限用戶 |
| **A02 - Cryptographic Failures** | 🟢 低 | 合格 | API Key 透過環境變數 `${NVIDIA_AI_API_KEY:}` 注入，未寫死在 yml |
| **A03 - Injection** | 🟢 低 | 合格 | 所有 DB 查詢使用 JPA 參數化查詢（`:param`），無 SQL 拼接。`MailRecipientRequest.email` 經 `@Email` 驗證 |
| **A04 - Insecure Design** | 🟢 低 | 已修正 | 分頁 size 已加 `@Max(100)` 限制；AI message 加 `@Size(max=500)` + sanitize；爬蟲手動觸發已加 10 分鐘 cooldown |
| **A05 - Security Misconfiguration** | 🟢 低 | 合格 | Security filter chain 有完整配置，未開放多餘端點 |
| **A06 - Vulnerable Components** | ⚪ 未驗證 | 需掃描 | Playwright、POI 等第三方依賴需定期 CVE 掃描 |
| **A07 - Auth Failures** | 🟢 低 | 合格 | JWT 驗證在 filter 層統一處理 |
| **A08 - Data Integrity** | 🟢 低 | 合格 | 爬蟲資料有 unique constraint 防重複；`tender_mail_recipient` 有 `(tenant_id, email)` unique |
| **A09 - Logging & Monitoring** | 🟢 低 | 合格 | 排程與爬蟲均有 Slf4j 日誌，失敗有 error log |
| **A10 - SSRF** | � 低 | 已修正 | 爬蟲只連固定 URL（pcc.gov.tw），`fetchDetailPageHtml()` 已加入 `ALLOWED_URL_PREFIX` 白名單驗證 |

### 4.2 安全性問題清單

| # | 嚴重度 | 問題 | 描述 | 狀態 | 說明 |
|---|--------|------|------|------|------|
| S1 | 🟠 中 | **AI Prompt Injection** | 使用者輸入的 `message` 直接送入 LLM system prompt。已加 `@Size(max=500)` 限制長度，降低風險。但仍未做 sanitization（移除控制字元） | 已修正 | `TenderChatService.buildMessages()` 加入 `sanitize()` 方法，移除 U+0000~U+001F 控制字元（保留 \n \r \t） |
| S2 | 🟠 中 | **爬蟲 detail URL 無驗證** | `PccListRow.detailUrl()` 來自爬取的 HTML，直接用 Playwright 導航。若 HTML 被注入惡意 URL，可導致 SSRF 或存取內部服務 | 已修正 | `PccBrowserService.fetchDetailPageHtml()` 加入 `ALLOWED_URL_PREFIX` 白名單驗證，非 `https://web.pcc.gov.tw/` 開頭直接拒絕 |
| S3 | 🟠 中 | **手動爬蟲 endpoint 無 Rate Limit** | `POST /v1/tender/scrape` 與 `POST /v1/tender/award-scrape` 無呼叫頻率限制。雖有 `@PreAuthorize`，惡意管理員可不斷觸發造成對外網站 DDoS | 已修正 | 兩個 Controller 加入 `AtomicReference<Instant>` 10 分鐘冷卻機制，冷卻期內回傳 `RATE_LIMIT_EXCEEDED` |
| S4 | 🟡 低 | **廠商資料可被列舉** | `GET /v1/tender/vendor-dashboard/suggest?q=` 空字串查詢回傳前 20 大廠商，有 `tender:award:view` 權限的攻擊者可逐步列舉廠商名稱和統編 | 已修正 | `VendorDashboardController.suggest()` 加入 `q.trim().length() < 2` 檢查，不足 2 字直接回空 |
| S5 | 🟡 低 | **AI 回應資料可能含敏感資訊** | `TenderChatResponse.data` 欄位直接將 function 查詢結果（含廠商統編等）暴露給前端 | 已確認 | `TenderAiChatView.vue` 無任何 `console.log`，不會洩露 raw data 到開發者工具或第三方 |

### 4.3 已修復項目（相比上次 Review）

| # | 原始問題 | 修復狀態 | 說明 |
|---|----------|----------|------|
| 舊 P1#4 | NVIDIA API Key 寫在 properties | ✅ 已修復 | 已改用 `${NVIDIA_AI_API_KEY:}` 環境變數 |
| 舊 P3#14 | 郵件收件人硬編碼 | ✅ 已修復 | 改為 DB 管理 + UI 界面，保留 yml fallback |
| 舊 P3#12 | 前端未做 debounce | ✅ 非議題 | `el-autocomplete` 內建 300ms debounce |
| 舊 F1 | XSS 防護（v-html） | ✅ 確認安全 | 確認未使用 `v-html`，Vue 3 預設 escape |
| 舊 P1#5 | AI Chat history 無長度限制 | ✅ 已修復 | 前端 `.slice(-20)` + 後端 `@Size(max=20)` 雙重限制 |

### 4.4 前端安全

| # | 問題 | 狀態 | 說明 |
|---|------|------|------|
| F1 | **XSS 防護** | 已確認 | 所有 view 未使用 `v-html`，安全 |
| F2 | **API 超時設定** | 已修正 | `tenderChat` 支援 `AbortSignal`，前端加入取消按鈕 + `AbortController`，使用者可中斷等待 |
| F3 | **Email 前端驗證** | 已確認 | MailRecipientView 使用正則 `/^[^\s@]+@[^\s@]+\.[^\s@]+$/` 前端驗證 email，搭配後端 `@Email` 雙重防護 |

---

## 五、效能觀察

| 項目 | 風險 | 建議 |
|------|------|------|
| **VendorDashboard 多次聚合查詢** | 中 | 單一 overview 頁面連續呼叫 7 個 endpoint，每個都是獨立 native query。考慮合併為單一端點減少 N+1 網路延遲 |
| **Native Query 無索引提示** | 中 | `ILIKE CONCAT('%', :vendorName, '%')` 無法使用 B-tree index。當 `tender_award` 超過百萬筆時可能變慢。加入 `pg_trgm` GIN index 或 full-text search |
| **Playwright 記憶體** | 中 | 每次爬蟲啟動完整 Chromium instance，高峰時若同時手動 + 排程觸發可能 OOM。加入互斥鎖或排隊機制 |
| **前端分頁為 client-side** | 低 | SearchKeywordView / AgencyFilterView / MailRecipientView 一次載入所有資料再前端切頁。資料量通常不多（< 100 筆），暫可接受 |

---

## 六、建議優先修復項目

1. **S1** — AI Prompt Injection 防護 ✅ 已加 `@Size(max=500)` 長度限制（sanitization 為可選加強）
2. **P1#1** — 分頁參數上限校驗 ✅ 已加 `@Max(100)` + `@Valid`
3. **P1#4** — AI Chat history 後端加 `@Size(max=20)` ✅ 已修復
4. ⬆️ **S2** — 爬蟲 URL 白名單驗證
5. **P1#3** — 爬蟲失敗應回傳錯誤碼 ✅ 已改為 `BaseResponse.fail()`
6. ⬆️ **S3** — 手動爬蟲 rate limit / cooldown

---

## 七、新增功能 Review：郵件收件人管理

### 架構評估

| 面向 | 評分 | 說明 |
|------|------|------|
| **設計** | ⭐⭐⭐⭐ | DB table + Entity + Service + Controller + Frontend 全棧實作，架構一致 |
| **多租戶** | ⭐⭐⭐⭐⭐ | 正確實作 TenantAware + @Filter + TenantEntityListener，tenant-scoped |
| **權限** | ⭐⭐⭐⭐⭐ | 複用現有 `tender:config:view` / `tender:config:edit` 權限碼 |
| **向下相容** | ⭐⭐⭐⭐⭐ | DB 無資料時 fallback 至 yml 設定，不影響現有環境 |
| **輸入驗證** | ⭐⭐⭐⭐⭐ | `@NotBlank` + `@Email` + 前端正則 + DB 重複 catch `DataIntegrityViolationException` 回傳 409 |
| **前端** | ⭐⭐⭐⭐ | 完整 CRUD + 分頁 + i18n，與現有 view 風格一致 |

### 待改善

- ✅ `MailRecipientService.create()` 已 catch `DataIntegrityViolationException` 轉為 409 回應（`MAIL_RECIPIENT_EMAIL_DUPLICATE`）
- ✅ 已新增 batch import 功能（`POST /v1/tender/mail-recipients/batch`），前端支援批次匯入 Dialog，重複/格式錯誤自動略過並顯示明細

---

## 八、總結

Tender 模組整體架構合理，分層清晰。與上次 Review 相比：

**已改善：**
- ✅ 郵件收件人從寫死 yml 改為 DB + UI 管理
- ✅ API Key 正確使用環境變數
- ✅ 前端 AI Chat 已限制 history 20 則
- ✅ 前端搜尋使用 el-autocomplete 內建 debounce
- ✅ 無 v-html XSS 風險

**主要殘留風險集中在：**
- **爬蟲**：detailUrl 無白名單驗證（S2）、手動觸發無 rate limit（S3）
- **AI 整合**：已有長度限制，可進一步加 sanitization 移除控制字元

所有 P1 高優先項目已全部修復。建議接下來處理 S2（爬蟲 URL 白名單）和 S3（rate limit）。
