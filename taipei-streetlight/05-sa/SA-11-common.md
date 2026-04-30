# SA-11 共用功能 Function List

> **對應需求**：跨模組共用基礎設施  
> **SRS 對應**：SRS-NFR-001 ~ NFR-006, SRS-01-001 ~ 005  
> **範圍**：認證/授權核心、稽核日誌、通知引擎、檔案管理、匯出引擎、事件匯流排

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-11-01 | 認證與授權 | ALL | JWT 認證 + RBAC 授權 |
| UC-11-02 | 稽核日誌 | AUDITOR, GOV_ADMIN | 操作紀錄查閱 |
| UC-11-03 | 通知發送 | 系統 | 多管道通知引擎 |
| UC-11-04 | 檔案管理 | ALL | 上傳/下載/病毒掃描 |
| UC-11-05 | 匯出引擎 | GOV_ADMIN | 通用 CSV/XLSX/ODS 匯出 |
| UC-11-06 | 事件匯流排 | 系統 | 跨模組事件發佈/訂閱 |

---

## Function List

### 認證核心 (§2-6)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-00-001 | JWT 核發 | P | 系統 | 帳號/密碼+驗證碼 | JWT (access+refresh) | bcrypt 驗密；登入失敗 5 次鎖定 30 分鐘 | SRS-01-001 | §2-6 | POST /v1/auth/login |
| FN-00-002 | JWT 驗證(Filter) | P | 系統 | Authorization Header | SecurityContext | JwtAuthFilter：Token 過期/簽名錯誤→401 | SRS-01-001 | §2-6 | (Filter) |
| FN-00-003 | Token 刷新 | P | 系統 | refreshToken | 新 access Token | — | SRS-01-001 | §2-6 | POST /v1/auth/refresh |
| FN-00-004 | 登出 | P | ALL | — | Token 失效 | Token 加入黑名單(Redis/DB) | SRS-01-001 | §2-6 | POST /v1/auth/logout |
| FN-00-005 | 驗證碼產生 | P | 系統 | — | Base64 圖片 + captchaId | — | SRS-01-001 | §2-6 | GET /v1/auth/captcha |
| FN-00-006 | 驗證碼驗證 | P | 系統 | captchaId + answer | true/false | 5 分鐘有效 | SRS-01-001 | §2-6 | (內部) |

### 授權核心 (§2-1~2-3)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-00-007 | 權限檢查(AOP) | P | 系統 | @PreAuthorize + SecurityContext | 通過/403 | RBAC：角色→權限→資源 | SRS-01-002 | §2-1 | (AOP) |
| FN-00-008 | 多租戶過濾(JPA Filter) | P | 系統 | @TenantAware + tenantId | 自動 WHERE | Hibernate @Filter(name="tenantFilter") | SRS-01-002 | §2-1 | (JPA Filter) |
| FN-00-009 | 目前使用者取得 | R | 系統 | SecurityContext | UserInfo DTO | SecurityContextUtils.getCurrentUser() | SRS-01-002 | §2-1 | (內部) |

### 稽核日誌 (§2-7)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-00-010 | 操作日誌紀錄(AOP) | C | 系統 | @AuditLog + HttpRequest | audit_log 紀錄 | @Async 寫入；PayloadSanitizer 脫敏 | SRS-01-004 | §2-7 | (AOP) |
| FN-00-011 | 稽核日誌查詢 | R | AUDITOR, SUPER_ADMIN | 使用者/模組/時間/分頁 | 日誌清單 | — | SRS-01-004 | §2-7 | GET /v1/auth/audit/logs |
| FN-00-012 | 稽核日誌匯出 | E | AUDITOR | 篩選條件 | ODS/XLS/CSV | — | SRS-01-004 | §2-7 | GET /v1/auth/audit/logs/export |
| FN-00-013 | 日誌自動清理(Job) | D | 系統 | 保留天數設定 | 刪除過期紀錄 | AuditPurgeJob：排程清理 | SRS-01-004 | §2-7 | (排程) |

### 通知引擎 (跨模組)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-00-014 | 站內通知發送 | C | 系統 | 接收者、標題、內容、類型 | notification 紀錄 | — | SRS-01-005 | §2-9 | (內部) |
| FN-00-015 | 通知列表查詢 | R | ALL | 分頁、已讀篩選 | 通知清單 | — | SRS-01-005 | §2-9 | GET /v1/auth/notifications |
| FN-00-016 | 標記已讀 | U | ALL | 通知 ID / 全部已讀 | 更新結果 | — | SRS-01-005 | §2-9 | PUT /v1/auth/notifications/{id}/read |
| FN-00-017 | WebSocket 即時推送 | N | 系統 | 通知事件 | WS 訊息 | SockJS/STOMP | SRS-01-005 | §2-9 | WebSocket /ws/notifications |
| FN-00-018 | E-mail 通知 | N | 系統 | 收件者、主旨、HTML 內容 | 寄送結果 | SMTP；模板引擎(Thymeleaf) | SRS-01-005 | §2-9 | (內部) |
| FN-00-019 | SMS 通知 | N | 系統 | 手機號碼、內容 | 寄送結果 | 簡訊閘道(EXT-03) | SRS-01-005 | §2-9 | (內部) |

### 檔案管理 (跨模組)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-00-020 | 檔案上傳 | C | ALL | multipart/form-data | fileId + metadata | 病毒掃描(ClamAV)；大小限制；副檔名白名單 | SRS-NFR-002 | §5-3 | POST /v1/auth/files/upload |
| FN-00-021 | 檔案下載 | R | ALL | fileId | 檔案串流 | 權限檢查 | SRS-NFR-002 | §5-3 | GET /v1/auth/files/{id}/download |
| FN-00-022 | 檔案預覽 | R | ALL | fileId | 預覽 URL | 圖片/PDF 預覽 | SRS-NFR-002 | §5-3 | GET /v1/auth/files/{id}/preview |
| FN-00-023 | 檔案刪除 | D | ALL | fileId | 刪除結果 | 軟刪除 | SRS-NFR-002 | §5-3 | DELETE /v1/auth/files/{id} |

### 匯出引擎 (跨模組)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-00-024 | CSV 匯出 | E | GOV_ADMIN | 資料+欄位定義 | CSV 檔案 | UTF-8 BOM | SRS-NFR-003 | §1-3 | (共用 Service) |
| FN-00-025 | XLSX 匯出 | E | GOV_ADMIN | 資料+欄位定義 | XLSX 檔案 | Apache POI | SRS-NFR-003 | §1-3 | (共用 Service) |
| FN-00-026 | ODS 匯出 | E | GOV_ADMIN | 資料+欄位定義 | ODS 檔案 | ODF 開放格式 | SRS-NFR-003 | §1-3 | (共用 Service) |
| FN-00-027 | Excel 匯入引擎 | I | GOV_ADMIN | Excel 檔案+mapping | 匯入結果 | 驗證+錯誤回報 | SRS-NFR-003 | §1-3 | (共用 Service) |

### 事件匯流排 (跨模組 E1~E13)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-00-028 | 事件發佈 | P | 系統 | 事件類型+payload | — | Spring ApplicationEventPublisher | — | — | (內部) |
| FN-00-029 | 事件訂閱處理 | P | 系統 | @EventListener | 業務處理 | @Async + @TransactionalEventListener | — | — | (內部) |
| FN-00-030 | 事件紀錄(可選) | C | 系統 | 事件 | event_log | 供除錯+重送 | — | — | (內部) |

---

## 事件對應表

| 事件 ID | 事件名稱 | 觸發模組 | 訂閱模組 | 說明 |
|---------|---------|---------|---------|------|
| E1 | AssetChangeEvent | 資產 | 審批 | 資產異動→建立審批 |
| E2 | RepairStatusEvent | 報修 | 通知 | 報修狀態變更→通知 |
| E3 | WorkflowApprovedEvent | 審批 | 資產/報修/換裝 | 審批通過→目標模組回寫 |
| E4 | WorkflowRejectedEvent | 審批 | 資產/報修/換裝 | 審批退回 |
| E5 | FaultDetectedEvent | IoT/報修 | 報修 | 故障偵測→自動建報修 |
| E6 | ReplacementMaterialEvent | 換裝 | 材料 | 換裝→領料申請 |
| E7 | InventoryLowEvent | 材料 | 通知 | 庫存低→通知 |
| E8 | RepairCompletedEvent | 報修 | 資產 | 維修完→更新資產狀態 |
| E9 | ReplacementCompletedEvent | 換裝 | 資產 | 換裝完→更新資產 |
| E10 | DeviceOnlineEvent | IoT | 儀表板 | 設備上線 |
| E11 | DeviceOfflineEvent | IoT | 儀表板/告警 | 設備離線 |
| E12 | LowStockAlertEvent | 材料 | 通知 | 安全庫存預警 |
| E13 | InspectionAnomalyEvent | 巡查 | 報修 | 巡查異常→維修派工 |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 登入 | /login | 驗證碼+帳密 | FN-00-001, 005 |
| 通知中心 | /admin/notifications | 列表+已讀 | FN-00-015~016 |
| 稽核日誌 | /admin/audit | 查詢+匯出 | FN-00-011~012 |
