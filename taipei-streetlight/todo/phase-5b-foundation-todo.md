# Phase 5B — 基礎缺口補齊 TODO

> **建立日期**: 2026-04-25  
> **最後更新**: 2026-04-25  
> **甘特圖**: 05/05 – 06/01 (4 週, 與 5A/5C 並行)  
> **前置**: Phase 1~4 全部完成 ✅  
> **執行計畫**: 99-plan/2026-04-24-execution-plan.md §Phase 5B  
> **關鍵路徑**: 非關鍵路徑，但 5b5 ClamAV 影響所有附件上傳安全

### 進度總覽

| 區塊 | 進度 | 說明 |
|------|------|------|
| 5b1 密碼重設 | ✅ 完成 | Email 發送 + 前端忘記/重設密碼頁 + 密碼驗證強化 |
| 5b2 待辦通知強化 | ✅ 完成 | 事件驅動通知 + WebSocket 即時推送 + 前端 STOMP |
| 5b3 QR Code 產生 | ✅ 完成 | ZXing 生成 + PDFBox 批次 PDF + 前端預覽/匯出 |
| 5b4 公開報修網頁 | ✅ 完成 | 公開 API + CAPTCHA + 前端表單 + 進度查詢 |
| 5b5 ClamAV 整合 | ✅ 完成 | 檔案安全 + Tika + 病毒掃描 + 圖片消毒 |
| 5b6 動態欄位 UI | 🔄 進行中 | Schema Designer + DynamicFieldEditor + 驗證整合 |

### 甘特依賴

```
5b1 密碼重設 (4d) ──→ 5b4 公開報修 (10d) ──→ 5b6 動態欄位 UI (8d)
5b2 通知強化 (3d) ──→ 5b3 QR Code (3d) ──→ 5b5 ClamAV (5d)
```

---

## 5b1 — 密碼重設 §2-(5) (4 天)

> **Spec**: 02-spec/02-system-management.md §2-(5)  
> **SRS**: SRS-02-005 (AC-02-005-1~5)  
> **SA**: FN-01-031 申請密碼重設、FN-01-032 驗證重設 Token、FN-01-033 執行密碼重設  
> **SD**: SD-01-system-mgmt.md  
> **Test**: TS-01-system-mgmt.md (TC-01-031~033)

### 現有實作 (70%)

| 項目 | 檔案 | 狀態 |
|------|------|------|
| Entity | `UserResetPasswordTokenEntity` | ✅ 完成 |
| Repository | `UserResetPasswordTokenRepository` | ✅ 完成 |
| DTO | `ForgotPasswordRequest`, `ResetPasswordRequest` | ✅ 完成 |
| API | `POST /v1/noauth/user/forgot-password` (RateLimit 5/300s) | ✅ 端點存在 |
| API | `PUT /v1/noauth/user/reset-password` (RateLimit 5/300s) | ✅ 端點存在 |
| Service | `AuthServiceImpl.forgotPassword()` | ✅ Token 產生邏輯 |
| Service | `AuthServiceImpl.resetPassword()` | ✅ Token 驗證 + 密碼更新 |
| DB | `user_reset_password_token` 表 (V1) | ✅ 30 分鐘過期、一次性使用 |
| 密碼驗證 | `PasswordValidator` (≥12 字元, 大小寫+數字+特殊) | ✅ 完成 |

### 已完成 (2026-04-25)

- [x] **Email 發送服務** — `PasswordResetMailService` (HTML 模板, MimeMessage)
  - SMTP 設定 (`application-dev.yml` Gmail SMTP)
  - `app.frontend-base-url` 設定 (`application.yml`)
  - `forgotPassword()` 呼叫 `PasswordResetMailService.send()` 發送 Token 連結
- [ ] **SMS 備援** (選配) — 第三方簡訊 API 整合 (暫不實作)
- [x] **前端頁面**
  - `ForgotPasswordView.vue` (`/forgot-password`) — 輸入 Email → 顯示已寄出訊息
  - `ResetPasswordView.vue` (`/reset-password?token=xxx`) — 新密碼 × 2 + 即時強度檢查
  - `LoginView.vue` — 新增「忘記密碼？」連結
  - `router/index.ts` — 新增 `/forgot-password`、`/reset-password` 路由
  - i18n: zh-TW、zh-CN、en 三語系翻譯鍵
- [x] **防暴力加強** — RateLimit 已存在 (`@RateLimit` forgot-pwd 5/300s, reset-pwd 5/300s)
- [x] **密碼歷史檢查** — `resetPassword()` 呼叫 `PasswordValidator.checkNotRecentlyUsed()` + 寫入 `password_history`
- [x] **密碼強度驗證** — `resetPassword()` 呼叫 `PasswordValidator.validate()`
- [x] **單元測試** (AuthServiceTest.java 新增 4 個測試)
  - TC-01-031: forgotPassword 正常寄信 + 不存在 email 靜默成功
  - TC-01-032~033: resetPassword 正常重設 + 過期/已使用 token 拒絕

---

## 5b2 — 待辦通知強化 §2-(10) (3 天)

> **Spec**: 02-spec/02-system-management.md §2-(10)  
> **SRS**: SRS-02-010 (AC-02-010-1~5)  
> **SA**: FN-01-045 通知列表、FN-01-046 未讀計數、FN-01-047 標記已讀  
>        FN-00-014 站內通知發送、FN-00-015 通知列表查詢、FN-00-016 標記已讀  
> **SD**: SD-01-system-mgmt.md, SD-11-common.md §通知  
> **Test**: TS-01-system-mgmt.md, TS-00-common.md (TC-00-014~016)

### 現有實作 (65%)

| 項目 | 檔案 | 狀態 |
|------|------|------|
| Entity | `Notification` | ✅ 完成 |
| Controller | `NotificationController` | ✅ 5 個端點 |
| API | `GET /v1/auth/notifications` (分頁 + 已讀篩選) | ✅ |
| API | `GET /v1/auth/notifications/todos` | ✅ |
| API | `GET /v1/auth/notifications/unread-count` | ✅ |
| API | `PATCH /v1/auth/notifications/{id}/read` | ✅ |
| API | `PATCH /v1/auth/notifications/read-all` | ✅ |
| Service | `NotificationService` | ✅ 核心邏輯 |
| Channel | `EmailNotificationChannel`, `InAppNotificationChannel` | ✅ 多通道 |
| WebSocket | `WebSocketConfig` (STOMP `/ws`) | ✅ 骨架 |
| Auth | `StompAuthInterceptor` (JWT) | ✅ |
| DB | `notifications` 表 (V48) | ✅ |

### 設計決議 (2026-04-25)

| 議題 | 決議 | 說明 |
|------|------|------|
| WebSocket vs Polling | 選 A: InAppChannel 加 WebSocket 推送 | STOMP 基礎設施已就緒，改動小。保留 60s Polling 作為 fallback |
| 事件通知架構 | 選 B: 獨立 NotificationListener | 職責分離 — 狀態變更 Listener vs 通知 Listener 互不影響，用 `@Order(100+)` 確保在業務邏輯之後執行 |
| 通知接收者解析 | 依 WorkflowInstance 欄位 | `assignedTo` = 被指派人、`creatorId` = 發起人、`ticket.createdBy` = 建單人、角色查詢 = 倉管人員 |
| 面板內簽核 | 選 B: 先做跳轉 | AC-02-010-4 面板內簽核留後續迭代，本次先完成點選跳轉到案件詳情頁 |

#### 通知接收者解析策略

| 事件 | 通知對象 | 取值來源 |
|------|---------|---------|
| REPAIR_DISPATCH (報修派工) | 被指派外勤 | `workflowInstance.assignedTo` |
| COMPLETION_REPORTED (完工回報) | 流程發起人 | `workflowInstance.creatorId` |
| REPAIR_CLOSED (結案) | 原建單人 | `repairTicket.createdBy` |
| REPLACEMENT_DISPATCHED (換裝派工) | 被指派人員 | `workflowInstance.assignedTo` |
| ISSUE_REQUESTED (領料需求) | ADMIN + OPERATOR 角色 | `UserTenantMappingRepository.findByTenantIdAndRoleIdAndEnabledTrue()` |
| LOW_STOCK (低庫存) | ADMIN 角色 | 已實作 ✅ (LowStockAlertListener) |

### 已完成 (2026-04-25)

- [x] **WebSocket 即時推送** — `InAppChannel` 加入 `SimpMessagingTemplate.convertAndSendToUser()`
  - persist → WebSocket push `/user/queue/notifications`
  - 推送失敗不影響 DB 存儲（try-catch 容錯）
- [x] **事件驅動通知** — 5 個獨立 NotificationListener
  - `RepairDispatchNotificationListener` — 報修派工 → TODO 通知 assignedTo
  - `CompletionReportedNotificationListener` — 完工回報 → TODO 通知 creatorId
  - `RepairClosedNotificationListener` — 報修結案 → INFO 通知 ticket.createdBy
  - `ReplacementDispatchNotificationListener` — 換裝派工 → TODO 通知 assignedTo
  - `IssueRequestedNotificationListener` — 領料需求 → TODO 通知 ADMIN+OPERATOR 角色
- [x] **角色查詢** — `UserTenantMappingRepository.findByTenantIdAndRoleIdAndEnabledTrue()` 新增
- [x] **前端 STOMP client** — `@stomp/stompjs` 套件
  - `notificationStore` 加入 `connectWebSocket(token)` / `disconnectWebSocket()`
  - STOMP CONNECT 帶 JWT、subscribe `/user/queue/notifications`
  - 收到推送 → 即時更新 items + unreadCount
  - 保留 60s Polling 作為 fallback
  - `NotificationBell.vue` 傳入 accessToken 啟動 WebSocket
- [x] **前端通知面板** — 已存在 ✅ (NotificationBell.vue: bell icon + badge + popover + tabs + resolveRoute 跳轉)
- [x] **HTTP Polling fallback** — 已存在 ✅ (60s interval)
- [x] **單元測試**
  - `InAppChannelTest` — 更新加入 SimpMessagingTemplate mock + WebSocket push 驗證
  - `WorkflowNotificationListenerTest` — 新增 7 個測試 (5 個 happy path + 2 個 edge case)

---

## 5b3 — QR Code 產生 §6-(1) (3 天, after 5b2)

> **Spec**: 02-spec/06-replacement-maintenance.md §6-(1) 路燈號碼牌編號管理  
> **SRS**: SRS-04 (燈桿號碼牌編號)  
> **SA**: FN-03-008 設備動態欄位管理  
> **SD**: SD-03-asset.md  
> **Test**: TS-03-asset.md

### 現有實作 (45%)

| 項目 | 檔案 | 狀態 |
|------|------|------|
| Entity | `LightPoleNumber` (含 `qrCodeUrl` 欄位) | ✅ |
| Service | `LightPoleNumberService.generate()` | ✅ 號碼產生邏輯 |
| Controller | `LightPoleNumberController` | ✅ CRUD |
| DB | `light_pole_numbers.qr_code_url` (V45) | ✅ |

### 設計決議 (2026-04-25)

| 議題 | 決議 | 說明 |
|------|------|------|
| QR Code 內容 URL | `https://{domain}/public/repair?pole={poleNumber}` | 前瞻設計，與 5b4 公開報修銜接。`{domain}` 取自 `SystemSettingService.getSetting("frontend_base_url")` |
| PDF 生成函式庫 | Apache PDFBox 3.0.4 | Apache 2.0 授權，無商用風險。iText 為 AGPL |
| QR Code 生成策略 | 即時生成，不存檔 | ZXing 即時算出 PNG < 50ms。`qrCodeUrl` 存 QR Code 編碼的內容 URL |
| PDF 中文字型 | 內嵌 Noto Sans TC | `src/main/resources/fonts/NotoSansTC-Regular.ttf` (~12MB)，確保任何環境正確顯示 |

### 已完成 (2026-04-25)

- [x] **新增依賴** — `pom.xml` 加入 ZXing (core + javase) 3.5.3 + PDFBox 3.0.4
- [x] **QrCodeService** — 即時 QR Code 生成
  - `generatePng(String content, int size)` / `generatePng(String content)` → PNG bytes
  - 糾錯等級 Level M (15%)、margin=1、UTF-8 編碼
  - 預設尺寸 300×300px
- [x] **PoleNumberPdfExportService** — 批次 PDF 匯出
  - A4 版面 4×5 排版 (每頁 20 個)
  - 每個 QR Code 100×100pt + 下方燈桿編號文字
  - 頁首標題「臺北市路燈號碼牌 QR Code — 第 X/Y 頁」
  - 內嵌 Noto Sans TC 字型
- [x] **LightPoleNumberService 強化**
  - `generate()` — 建立時填入 `qrCodeUrl`（內容 URL）
  - `getQrCodePng(id)` — 即時生成 QR Code PNG
  - `exportQrCodesPdf(ids)` — 批次匯出 PDF
- [x] **API 端點**
  - `GET /v1/auth/replacement/pole-numbers/{id}/qr-code` → image/png (Cache-Control: 1d)
  - `POST /v1/auth/replacement/pole-numbers/qr-codes/batch-pdf` → application/pdf
- [x] **ErrorCode** — 新增 `POLE_NUMBER_NOT_FOUND("80021", 404)`
- [x] **前端**
  - `PoleNumberView.vue` — 新增勾選列、QR Code 按鈕欄、批次匯出 PDF 按鈕
  - QR Code 預覽 Modal（載入 blob → ObjectURL → img）
  - API: `getPoleNumberQrCode(id)` + `batchExportQrCodePdf(ids)` (blob responseType)
  - i18n: zh-TW、zh-CN、en 新增 6 個翻譯鍵
- [x] **單元測試** (9 tests, 0 failures)
  - `QrCodeServiceTest` — PNG magic bytes 驗證 + 不同尺寸 + 中文內容 (3 tests)
  - `LightPoleNumberServiceTest` — generate 填入 qrCodeUrl + getQrCodePng + exportPdf + not found (6 tests)

---

## 5b4 — 公開報修網頁 §5-(1) (10 天, after 5b1)

> **Spec**: 02-spec/05-repair-maintenance.md §5-(1) 民眾線上報修  
> **SRS**: SRS-05-001 (AC-05-001-1~6)  
> **SA**: FN-04-001 民眾報修頁面、FN-04-002 提交報修、FN-04-003 進度查詢  
> **SD**: SD-04-repair.md  
> **Test**: TS-04-repair.md (TC-04-001~003)

### 現有實作 (10%)

| 項目 | 檔案 | 狀態 |
|------|------|------|
| Enum | `RepairTicketSource.CITIZEN_WEB` | ✅ |
| Entity | `RepairTicket.source` 欄位 | ✅ |
| Security | `SecurityConfig` permits `/v1/noauth/**` | ✅ |

### 設計決議 (2026-04-25)

| 議題 | 決議 | 說明 |
|------|------|------|
| 多租戶處理 | QR Code 攜帶 tenantId | 透過 poleNumber 反查 `LightPoleNumber.tenantId`，自動帶入。無 QR Code 時用 default tenant。Controller 手動 setTenantId |
| 公開 API 路徑 | `/v1/noauth/public/repair` | 沿用現有 `/v1/noauth/**` permitAll 規則，無需改 SecurityConfig |
| Workflow 匿名使用者 | 新增 `createPublicTicket()` | 不修改 `createDirect()`，獨立方法。createdBy = `"CITIZEN"` |
| 附件上傳 | 先做基礎上傳，後續加掃描 | 副檔名白名單 + 大小限制，scanStatus=PENDING。5b5 完成後補 ClamAV |
| CAPTCHA | 複用現有 CaptchaService | 登入畫面已有完整 CAPTCHA 基礎設施，公開報修直接複用 |

### 已完成 (2026-04-25)

- [x] **PublicRepairRequest DTO** — reporterName, phone, email(選填), description, address, poleNumber(選填), captchaKey, captchaValue, privacyAgreed
- [x] **PublicRepairStatusResponse DTO** — ticketNo, status, statusLabel, createdAt, updatedAt（不含內部資訊）
- [x] **RepairTicketService.createPublicTicket()** — 獨立方法
  - poleNumber → LightPoleNumber 反查 tenantId + deviceId
  - 無 poleNumber 時用 default tenant (`TENANT_A`)
  - createdBy = `"CITIZEN"`，source = CITIZEN_WEB
  - 建立 workflow instance (creatorId = "CITIZEN")
- [x] **PublicRepairController** — `/v1/noauth/public/repair`
  - `POST /` — 提交報修 (CAPTCHA 驗證 + @RateLimit)
  - `GET /{ticketNo}/status?phone=xxx` — 查詢進度 (ticketNo + phone 雙因子)
- [x] **RepairTicketRepository** — 新增 `findByTicketNumberAndReporterPhone()` 查詢
- [x] **前端 — 公開報修表單** (`/public/repair`)
  - RWD 手機優先設計，獨立佈局（不在 admin 框架內）
  - QR Code 參數自動帶入 (query param `?pole=xxx`)
  - CAPTCHA 圖形驗證碼整合
  - 個資聲明 checkbox
  - 提交成功顯示案件編號
- [x] **前端 — 進度查詢頁** (`/public/repair/status`)
  - 案件編號 + 電話雙因子查詢
  - 狀態顯示 + 時間資訊
- [x] **API + 路由 + i18n**
  - `submitPublicRepair()` + `queryRepairStatus()` API
  - noauthRoutes 新增 `/public/repair` + `/public/repair/status`
  - zh-TW、zh-CN、en 三語系翻譯鍵
- [x] **單元測試** — 全數通過
  - PublicRepairControllerTest: 5 tests (submit 成功/CAPTCHA 失敗/隱私未勾/缺欄位/查詢成功)
  - RepairTicketServiceTest: +4 tests (createPublicTicket with/without pole, getPublicStatus 成功/not found)

---

## 5b5 — ClamAV 整合 §5-(3) (5 天, after 5b3)

> **Spec**: 02-spec/05-repair-maintenance.md §5-(3)  
> **SRS**: SRS-05-003 附件防毒掃描, SRS-NFR-003-11  
> **SA**: FN-00-020 檔案上傳、FN-04-006 上傳附件  
> **SD**: SD-11-common.md  
> **KB**: 99-adr/KB-001-file-upload-security.md  
> **Test**: TS-00-common.md (TC-00-020-01~03)

### 現有實作 (20%)

| 項目 | 檔案 | 狀態 |
|------|------|------|
| Enum | `ScanStatus` (PENDING/CLEAN/INFECTED) | ✅ |
| Entity | `TicketAttachment.scanStatus` | ✅ |
| DB | `ticket_attachments.scan_status` (V37, DEFAULT 'PENDING') | ✅ |
| Interface | `FileStorageService` (store/load/delete) | ✅ |
| Impl | `LocalFileStorageService` (UUID prefix, path traversal 防護) | ✅ |
| Service | `TicketAttachmentService` (upload/download/getByTicket) | ✅ |
| Controller | `TicketAttachmentController` (upload/download/list) | ✅ |

### 設計決議 (2026-04-25)

| 議題 | 決議 | 說明 |
|------|------|------|
| ClamAV client library | 自行 Socket 實作 | clamd INSTREAM 協定簡單 (~30 行)，不加 `xyz.capybara:clamav-client` 依賴 |
| 掃描時機 | 同步掃描 | 上傳時立即掃描，1~3 秒延遲可接受；ClamAV 不可用時 fallback → PENDING |
| 檔案大小限制 | application.yml 可配置 | 預設 10MB，移除硬編碼 20MB |
| P2 圖片消毒 | 本次一起做 | ImageIO re-encode ~20 行，消除 EXIF/Polyglot payload，CP 值高 |
| ClamAV Docker | 暫不建 docker-compose | 用 NoOp mock 實作切換，本地開發不需 ClamAV daemon |
| 切換機制 | `@ConditionalOnProperty` | `virus-scan.enabled=false` → NoOp (回 CLEAN)，`true` → ClamAV Socket |

### 架構設計

```
VirusScanService (interface)
├── ClamAvVirusScanService    — @ConditionalOnProperty(virus-scan.enabled=true)
│   └── Socket → clamd INSTREAM → CLEAN / INFECTED / ERROR
└── NoOpVirusScanService      — @ConditionalOnProperty(virus-scan.enabled=false, 預設)
    └── 直接回 CLEAN

FileValidationService (新增)
├── validateExtension()       — 副檔名白名單
├── validateMagicBytes()      — Tika detect() vs 宣稱 MIME
└── validateSize()            — 可配置上限

ImageSanitizer (新增)
└── sanitize(InputStream)     — ImageIO re-encode，消除 EXIF/Polyglot

上傳流程 (TicketAttachmentService.upload 改造):
1. FileValidationService.validate() — 副檔名 + Magic bytes + 大小
2. ImageSanitizer.sanitize() — 圖片類型才執行
3. FileStorageService.store() — UUID 檔名存檔
4. VirusScanService.scan() — CLEAN/INFECTED/PENDING
5. 存 DB → scanStatus = 掃描結果
```

### 已完成 (2026-04-25)

#### P0 — 基礎檔案安全

- [x] **副檔名白名單** — jpg, jpeg, png, gif, bmp, webp, pdf, xlsx, docx, csv, mp4, wav, mp3
- [x] **Magic bytes 驗證** — Apache Tika `detect()` + MIME 交叉比對
  - 新增依賴: `org.apache.tika:tika-core:2.9.2`
- [x] **檔名 UUID 重新產生** — ✅ 已有 (LocalFileStorageService UUID prefix)
- [x] **上傳目錄隔離** — ✅ 已有 (`${FILE_STORAGE_ROOT}/attachments/`)
- [x] **Response Header**
  - `Content-Disposition: attachment` ✅ 已有
  - `X-Content-Type-Options: nosniff` ✅ 已加入
- [x] **檔案大小限制** — application.yml 可配置 (`file.validation.max-size`)，預設 10MB
  - 移除 LocalFileStorageService 硬編碼 20MB
  - Spring multipart limit 同步設定

#### P1 — ClamAV 病毒掃描

- [x] **VirusScanService interface** — `ScanResult scan(String filePath)` → CLEAN/INFECTED/ERROR
- [x] **NoOpVirusScanService** — `virus-scan.enabled=false`（預設）時啟用，回 CLEAN
- [x] **ClamAvVirusScanService** — 自行 Socket 實作 clamd INSTREAM 協定（~30行，零外部依賴）
- [x] **application.yml 配置** — `virus-scan.enabled/clamav.host/port/timeout` 全部 env 可覆蓋
- [x] **上傳流程整合 (TicketAttachmentService.upload 改造)**
  1. FileValidationService.validate() — 副檔名 + Magic bytes + 大小
  2. ImageSanitizer.sanitize() — 圖片類型才執行
  3. FileStorageService.store() — UUID 檔名存檔
  4. VirusScanService.scan() — CLEAN/INFECTED/PENDING
  5. INFECTED → 刪除檔案 + 拋 FILE_VIRUS_DETECTED
  6. ERROR → scanStatus=PENDING（ClamAV 不可用時不阻擋上傳）

#### P2 — 圖片消毒

- [x] **ImageSanitizer** — `ImageIO.read()` → `ImageIO.write()` 消除 EXIF/Polyglot payload
- [x] 僅對 image/* 類型執行，消毒失敗 fallback 存原檔

#### 新增檔案

| 檔案 | 說明 |
|------|------|
| `common/service/VirusScanService.java` | 介面 (ScanResult enum + scan method) |
| `common/service/NoOpVirusScanService.java` | NoOp 實作 (virus-scan.enabled=false) |
| `common/service/ClamAvVirusScanService.java` | ClamAV Socket 實作 |
| `common/service/FileValidationService.java` | 副檔名白名單 + Tika Magic bytes + 大小限制 |
| `common/service/ImageSanitizer.java` | ImageIO re-encode 圖片消毒 |

#### 修改檔案

| 檔案 | 變更 |
|------|------|
| `pom.xml` | 新增 tika-core 2.9.2 |
| `ErrorCode.java` | +4: FILE_EXTENSION_NOT_ALLOWED, FILE_TYPE_MISMATCH, FILE_SIZE_EXCEEDED, FILE_VIRUS_DETECTED |
| `FileStorageService.java` | +resolveAbsolutePath() |
| `LocalFileStorageService.java` | +resolveAbsolutePath(), 移除硬編碼 20MB |
| `TicketAttachmentService.java` | upload() 整合完整安全管線 |
| `TicketAttachmentController.java` | download() +X-Content-Type-Options: nosniff |
| `application.yml` | +file.validation.max-size, +virus-scan.*, +spring.servlet.multipart.* |

#### 單元測試 — 全數通過 (33 tests)

- [x] FileValidationServiceTest: 19 tests (副檔名白名單/黑名單/大小/Magic bytes)
- [x] ImageSanitizerTest: 5 tests (JPEG/PNG re-encode/invalid/unsupported/EXIF strip)
- [x] NoOpVirusScanServiceTest: 2 tests (always CLEAN)
- [x] TicketAttachmentServiceTest: 7 tests (image sanitize+clean, non-image, virus detected, scan error fallback, list, download, not found)

---

## 5b6 — 動態欄位 UI §1-(5) (8 天, after 5b4)

> **Spec**: 02-spec/01-basic-requirements.md §1-(5) 擴充性設計  
> **SRS**: SRS-04-007 設備擴充欄位, SRS-04-013~015  
> **SA**: FN-03-008 設備 JSONB 動態欄位管理  
> **SD**: SD-03-asset.md  
> **Test**: TS-03-asset.md (TC-03-008-01)

### 現有實作 (15%)

| 項目 | 檔案 | 狀態 |
|------|------|------|
| Entity | `Device.attributes` (JSONB) | ✅ |
| DB | `devices.attributes` (jsonb) | ✅ |
| CRUD | 設備 API 已支援 JSONB 序列化/反序列化 | ✅ |

### JSONB 資料結構範例

```json
// DeviceType = POLE（燈桿）
{
  "height": 10,
  "material": "鋼管",
  "armCount": 2,
  "formFactor": "直桿",
  "baseType": "法蘭"
}

// DeviceType = LUMINAIRE（燈具）
{
  "wattage": 150,
  "colorTemp": 4000,
  "brand": "飛利浦",
  "ratedLumens": 18000,
  "controlType": "電子"
}
```

### 設計決議 (2026-04-25)

| 議題 | 決議 | 說明 |
|------|------|------|
| Schema Designer UI | A 方案：完整 Designer | vuedraggable 拖拉卡片式欄位編輯 + 即時預覽 |
| 後端 Schema 驗證 | 自行簡易驗證 | 只驗 required + type (string/number/boolean/date)，不引入 json-schema-validator |
| 向後相容 | Schema 不存在 → 不驗證 | 有 schema 時 open schema：只驗已定義的 key，額外 key 保留 |
| 初始種子資料 | Flyway migration | 6 種 DeviceType 預設 schema，租戶可自行修改 |
| Dialog 位置 | 固定欄位下方 | 依 deviceType 動態載入 schema 渲染表單，切換時重載 |

### 架構設計

```
Schema Designer (ADMIN 管理頁)
├── DeviceType 選擇器 → 載入對應 schema
├── SchemaFieldList.vue (vuedraggable 欄位列表)
│   ├── SchemaFieldCard.vue (每個欄位一張卡片)
│   │   ├── 拖曳手柄 (GripVertical icon)
│   │   ├── fieldKey / title / type / required / enum
│   │   └── 展開區：min/max / placeholder / enum 選項
│   └── + 新增欄位按鈕
├── 儲存按鈕 → PUT API
└── 預覽區 (即時渲染 DynamicFieldEditor)

DynamicFieldEditor (設備新增/編輯用)
├── 依 schema.fields 動態產生 el-form-item
├── 支援: text / number / date / select / checkbox
├── required 標記 + min/max 驗證
└── v-model: attributes (Map<string, unknown>)
```

### 待實作

#### 後端 — Schema 定義管理

- [ ] **V55 Flyway migration** — device_templates 表 + 6 種預設 schema
- [ ] **DeviceTemplate entity** — id, tenantId, deviceType, schema(JSONB), version
- [ ] **DeviceTemplateRepository**
- [ ] **DeviceTemplateService** — getSchema / updateSchema / validateAttributes
- [ ] **DeviceTemplateController** — GET + PUT API
- [ ] **DeviceService 整合** — create/update 時呼叫 schema 驗證

#### 前端 — 動態表單 + Schema Designer

- [ ] **npm install vuedraggable@next**
- [ ] **DynamicFieldEditor.vue** — 根據 schema 動態產生表單
- [ ] **SchemaDesignerView.vue** — ADMIN 頁：拖拉排序 + 卡片式欄位編輯 + 預覽
- [ ] **SchemaFieldCard.vue** — 單一欄位卡片元件
- [ ] **DeviceManagementView.vue** — dialog 嵌入 DynamicFieldEditor
- [ ] **API + i18n + routes**

#### 測試

- [ ] TC-03-008-01: Error — JSONB 超過 10KB → 400
- [ ] 驗證: schema 不存在時，attributes 自由儲存（向後相容）
- [ ] 驗證: schema 存在時，不符 required 欄位 → 400
- [ ] 驗證: open schema — 額外 key 保留不報錯

---

## 文件追溯

| 分類 | 文件 |
|------|------|
| **Spec** | 02-spec/01-basic-requirements.md §1-(5), 02-system-management.md §2-(5)/(10), 05-repair-maintenance.md §5-(1)/(3), 06-replacement-maintenance.md §6-(1) |
| **SRS** | SRS-02-005 (密碼重設), SRS-02-010 (通知), SRS-04-007 (動態欄位), SRS-05-001 (民眾報修), SRS-05-003 (防毒), SRS-NFR-003-11 |
| **SA** | SA-01 (FN-01-031~033, 045~047), SA-03 (FN-03-008), SA-04 (FN-04-001~003, 006), SA-11 (FN-00-014~016, 020~021) |
| **SD** | SD-01 (密碼重設), SD-03 (設備+QR), SD-04 (報修), SD-11 (通知+檔案) |
| **Test** | TS-00 (TC-00-014~016, 020), TS-01 (TC-01-031~033), TS-03 (TC-03-008), TS-04 (TC-04-001~003) |
| **KB** | 99-adr/KB-001-file-upload-security.md (P0/P1 安全清單) |
| **Plan** | 99-plan/2026-04-24-execution-plan.md §Phase 5B |
| **Gantt** | 99-plan/2026-04-24-gantt.md (5b1~5b6) |
