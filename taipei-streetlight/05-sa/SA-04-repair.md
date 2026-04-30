# SA-04 報修維護 Function List

> **對應需求**：§5-(1) ~ §5-(17)  
> **SRS 對應**：SRS-05-001 ~ SRS-05-017  
> **Spec 來源**：`/02-spec/05-repair-maintenance.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-04-01 | 民眾線上報修 | PUBLIC_USER | QR Code / 網頁報修 |
| UC-04-02 | 1999 通報接收 | 系統 | 外部系統介接立案 |
| UC-04-03 | 內部建單 | GOV_ADMIN, OPERATOR | 巡檢/電話/系統自動 建單 |
| UC-04-04 | 派工 | GOV_ADMIN, OPERATOR | 指派廠商/維護人員 |
| UC-04-05 | 施工回報 | FIELD_USER, CONTRACTOR | 進度更新+照片上傳 |
| UC-04-06 | 完工回報 | FIELD_USER, CONTRACTOR | 維修前中後照片+故障原因 |
| UC-04-07 | 結案審核 | GOV_MGR, GOV_CHIEF | 審核通過/退回 |
| UC-04-08 | 案件查詢 | GOV_ADMIN, GOV_MGR | 多條件查詢+匯出 |
| UC-04-09 | 改分轉送 | GOV_ADMIN | 改派其他單位 |
| UC-04-10 | 巡查管理 | GOV_ADMIN, OPERATOR | 巡查任務+紀錄 |
| UC-04-11 | 統計報表 | GOV_ADMIN, GOV_MGR | 維護統計匯出 |
| UC-04-12 | 開放資料匯出 | GOV_ADMIN | 資料大平台格式 |

---

## Function List

### 民眾報修 (§5-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-001 | 民眾報修頁面 | R | PUBLIC_USER | 路燈 QR Code / 直接進入 | 報修表單 | 公開頁面；個資使用聲明 | SRS-05-001 | §5-1 | GET /v1/public/repair |
| FN-04-002 | 提交民眾報修 | C | PUBLIC_USER | 姓名、電話、Email、地點描述、報修項目、附件 | 案件編號 | 實名制；附件限定件數(5)/容量(10MB/件)；自動掃碼解析設備 ID | SRS-05-001 | §5-1 | POST /v1/public/repair |
| FN-04-003 | 報修進度查詢（民眾） | R | PUBLIC_USER | 案件編號+手機 | 案件狀態 | — | SRS-05-001 | §5-1 | GET /v1/public/repair/{ticketNo}/status |

### 外部通報整合 (§5-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-004 | 1999 陳情案件接收 | I | 系統(EXT-03) | 1999 API payload | repair_ticket | 自動立案；source=EXTERNAL_1999；比對設備 ID | SRS-05-002 | §5-2 | POST /v1/integration/1999/receive |
| FN-04-005 | 1999 處理結果回覆 | E | 系統(EXT-03) | 案件結案資料 | 回覆 1999 | 結案後自動回覆處理結果 | SRS-05-002 | §5-2 | (事件驅動) |

### 附件管理 (§5-3)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-006 | 上傳附件 | C | GOV_ADMIN, FIELD_USER, PUBLIC_USER | 檔案、phase(BEFORE/DURING/AFTER)、GPS、描述 | 附件資料 | 病毒掃描(ClamAV)；PENDING→CLEAN/INFECTED；支援照片/影片/錄音 | SRS-05-003 | §5-3 | POST /v1/auth/attachments |
| FN-04-007 | 附件列表查詢 | R | GOV_ADMIN, OPERATOR | ticketType、ticketId | 附件清單(依 phase 分組) | — | SRS-05-003 | §5-3 | GET /v1/auth/attachments |
| FN-04-008 | 下載附件 | R | GOV_ADMIN, OPERATOR | 附件 ID | 檔案 | scanStatus=INFECTED 不可下載 | SRS-05-003 | §5-3 | GET /v1/auth/attachments/{id}/download |
| FN-04-009 | 刪除附件 | D | GOV_ADMIN | 附件 ID | 刪除結果 | — | SRS-05-003 | §5-3 | DELETE /v1/auth/attachments/{id} |

### 報修工單管理 (§5-4, §5-5, §5-6)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-010 | 報修工單列表 | R | GOV_ADMIN, GOV_MGR | 狀態、來源、優先度、部門、關鍵字、日期範圍、分頁 | 工單清單 | DataScope；10 種狀態篩選 | SRS-05-005 | §5-5 | GET /v1/auth/repair |
| FN-04-011 | 新增報修工單（內部） | C | GOV_ADMIN, OPERATOR | 來源、設備ID、描述、聯絡人、優先度 | 工單資料 | 雙路徑：fault_ticket 觸發 或 人工建單；自動建 REPAIR_DISPATCH 流程 | SRS-05-004 | §5-4 | POST /v1/auth/repair |
| FN-04-012 | 查看工單詳情 | R | GOV_ADMIN, GOV_MGR | 工單 ID | 完整資料+設備基本資料+歷來維護+地圖位置 | — | SRS-05-008 | §5-8 | GET /v1/auth/repair/{id} |
| FN-04-013 | 收案 | U | GOV_ADMIN | 工單 ID | 狀態→ACCEPTED | PENDING→ACCEPTED | SRS-05-006 | §5-6 | PUT /v1/auth/repair/{id}/accept |
| FN-04-014 | 派工 | U | GOV_ADMIN, OPERATOR | 工單 ID、派工單位、契約 ID、期限、備註 | 派工記錄 | ACCEPTED→DISPATCHED；建 repair_dispatch；觸發 E4(設備→UNDER_REPAIR) | SRS-05-004 | §5-4 | POST /v1/auth/repair/{id}/dispatch |
| FN-04-015 | 開始處理 | U | FIELD_USER | 工單 ID | 狀態→IN_PROGRESS | DISPATCHED→IN_PROGRESS | SRS-05-006 | §5-6 | PUT /v1/auth/repair/{id}/start |
| FN-04-016 | 完工回報 | U | FIELD_USER, CONTRACTOR | 工單 ID、維修說明、故障原因、附件 | 狀態→COMPLETION_REPORTED | IN_PROGRESS→COMPLETION_REPORTED；附件含前中後照片(含日期/時間/GPS) | SRS-05-004 | §5-4 | PUT /v1/auth/repair/{id}/complete |
| FN-04-017 | 送審 | P | GOV_ADMIN | 工單 ID | 狀態→PENDING_REVIEW | 建立 REPAIR_CLOSE 流程實例 | SRS-05-011 | §5-11 | PUT /v1/auth/repair/{id}/submit-review |
| FN-04-018 | 結案審核通過 | P | GOV_MGR, GOV_CHIEF | 流程實例 ID | 狀態→CLOSED | 觸發 E9：設備→ACTIVE + 寫 device_event | SRS-05-012 | §5-12 | (via workflow transition) |
| FN-04-019 | 退回補件 | P | GOV_MGR | 流程實例 ID、退回原因 | 狀態→RETURNED | PENDING_REVIEW→RETURNED；送審人可選是否再次同步資產 | SRS-05-011 | §5-11 | (via workflow transition) |
| FN-04-020 | 改分轉送 | U | GOV_ADMIN | 工單 ID、目標部門、原因 | 狀態→TRANSFERRED | DISPATCHED→TRANSFERRED；重新派工 | SRS-05-006 | §5-6 | PUT /v1/auth/repair/{id}/transfer |
| FN-04-021 | 匯出案件清冊 | E | GOV_ADMIN, GOV_MGR | 篩選條件 | ODS/XLS/CSV | — | SRS-05-013 | §5-13 | GET /v1/auth/repair/export |

### 維護案件記錄 (§5-7, §5-8, §5-9)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-022 | 報修資訊欄位 | R | — | — | — | 報修時間/人/聯絡/地點/項目/附件（完整欄位依需求訪談） | SRS-05-007 | §5-7 | (含於 FN-04-012) |
| FN-04-023 | 維護資訊欄位 | R | — | — | — | 故障原因(類別)/換裝資產/完成時間/前中後照片 | SRS-05-007 | §5-7 | (含於 FN-04-012) |
| FN-04-024 | 設備歷來維護履歷 | R | GOV_ADMIN | 設備 ID | 維護案件歷史清單 | 含每次維護內容+換裝資訊+剩餘保固 | SRS-05-009 | §5-9 | GET /v1/auth/devices/{id}/repair-history |
| FN-04-025 | 設備概覽（報修時） | R | GOV_ADMIN | 設備 ID | 基本資料+位置地圖+維護履歷 | — | SRS-05-008 | §5-8 | (組合 FN-03-004 + FN-04-024) |

### 自動產生通報單 (§5-10)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-026 | 產生通知機關報告單 | E | GOV_ADMIN | 工單 ID | PDF/DOCX | 自動帶入案件資訊 | SRS-05-010 | §5-10 | GET /v1/auth/repair/{id}/report/notification |
| FN-04-027 | 產生查報單 | E | GOV_ADMIN | 工單 ID | PDF/DOCX | 自動帶入 | SRS-05-010 | §5-10 | GET /v1/auth/repair/{id}/report/investigation |
| FN-04-028 | 產生設計單 | E | GOV_ADMIN | 工單 ID | PDF/DOCX | 含繪圖功能（呼叫 ArcGIS 物件）；帶入坐標 | SRS-05-010 | §5-10 | GET /v1/auth/repair/{id}/report/design |
| FN-04-029 | 結案後更新圖資 | P | 系統 | 結案事件 | 圖資更新 | 結案審核完成後匯入平台更新設備坐標/屬性 | SRS-05-010 | §5-10 | (E9 事件處理) |

### 巡查管理 (§5-14)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-030 | 巡查任務列表 | R | GOV_ADMIN, OPERATOR | 狀態、類型、分頁 | 任務清單 | — | SRS-05-014 | §5-14 | GET /v1/auth/inspection |
| FN-04-031 | 新增巡查任務 | C | GOV_ADMIN | 名稱、類型(安檢/巡檢/普查)、頻率、區域、期程 | 任務資料 | 支援單次/定期；指定區域 | SRS-05-014 | §5-14 | POST /v1/auth/inspection |
| FN-04-032 | 編輯巡查任務 | U | GOV_ADMIN | 任務 ID、修改欄位 | 更新結果 | — | SRS-05-014 | §5-14 | PUT /v1/auth/inspection/{id} |
| FN-04-033 | 停用巡查任務 | U | GOV_ADMIN | 任務 ID | 更新結果 | 軟刪除 | SRS-05-014 | §5-14 | PUT /v1/auth/inspection/{id}/disable |
| FN-04-034 | 巡查紀錄列表 | R | GOV_ADMIN, OPERATOR | 任務 ID、分頁 | 紀錄清單 | — | SRS-05-014 | §5-14 | GET /v1/auth/inspection/{taskId}/records |
| FN-04-035 | 新增巡查紀錄 | C | OPERATOR, FIELD_USER | 任務 ID、結果(正常/異常)、說明、附件、GPS | 紀錄資料 | 結果=異常 → 觸發 E13(自動建 fault_ticket) | SRS-05-014 | §5-14 | POST /v1/auth/inspection/{taskId}/records |
| FN-04-036 | 編輯巡查紀錄 | U | OPERATOR | 紀錄 ID、修改欄位 | 更新結果 | — | SRS-05-014 | §5-14 | PUT /v1/auth/inspection/records/{id} |
| FN-04-037 | 巡查派工 | U | GOV_ADMIN | 任務 ID、派工對象 | 派工結果 | 交付廠商處理 | SRS-05-014 | §5-14 | POST /v1/auth/inspection/{id}/dispatch |

### 非契約案件 / 開放資料 (§5-15, §5-16)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-038 | 非契約案件篩選 | R | GOV_ADMIN | 篩選條件 | 非契約/附約案件清單 | 快速辨識→匯出→派工 | SRS-05-015 | §5-15 | GET /v1/auth/repair?contractFilter=NONE |
| FN-04-039 | 開放資料匯出 | E | GOV_ADMIN | — | 資料大平台格式 CSV | 「臺北市路燈維修資料」 | SRS-05-016 | §5-16 | GET /v1/auth/repair/export/open-data |

### 里辦公處通知 (§5-17)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-040 | 里長通知設定 | C/U | GOV_ADMIN | 里別、Email、帳號 | 設定資料 | 可選 Email 或平台帳號 | SRS-05-017 | §5-17 | POST/PUT /v1/auth/repair/borough-notify |
| FN-04-041 | 里內故障通知推送 | N | 系統 | 障礙確認事件 | Email/平台通知 | 案件所在里 → 自動通知對應里長 | SRS-05-017 | §5-17 | (事件驅動) |

### 統計報表 (§5-13)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-04-042 | 維護案件統計 | R | GOV_ADMIN, GOV_MGR | 時間範圍、維度 | 統計數據 | 報修/完修/待修 數量+比例 | SRS-05-013 | §5-13 | GET /v1/auth/repair/statistics |
| FN-04-043 | 維護時間統計 | R | GOV_ADMIN | 時間範圍 | 平均修復時間 | — | SRS-05-013 | §5-13 | GET /v1/auth/repair/statistics/time |
| FN-04-044 | 通報來源統計 | R | GOV_ADMIN | 時間範圍 | 來源分布 | CITIZEN_WEB/1999/PATROL/PHONE/FAULT_TICKET | SRS-05-013 | §5-13 | GET /v1/auth/repair/statistics/source |
| FN-04-045 | 故障分類統計 | R | GOV_ADMIN | 時間範圍 | 故障類別分布 | — | SRS-05-013 | §5-13 | GET /v1/auth/repair/statistics/fault-category |
| FN-04-046 | 故障熱區統計 | R | GOV_ADMIN | 時間範圍、地理範圍 | 熱區 GeoJSON | 用於儀表板熱力圖 | SRS-05-013 | §5-13 | GET /v1/auth/repair/statistics/heatmap |
| FN-04-047 | 材料換修統計 | R | GOV_ADMIN | 時間範圍 | 材料使用統計 | — | SRS-05-013 | §5-13 | GET /v1/auth/repair/statistics/material |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 民眾報修（公開） | /public/repair | QR Code 報修 | FN-04-001~003 |
| 報修工單列表 | /admin/repair/tickets | 多條件篩選+新增 | FN-04-010~011 |
| 工單詳情 | /admin/repair/tickets/:id | 4 Tabs + Action Bar | FN-04-012~020 |
| 派工 Dialog | — | 派工表單 | FN-04-014 |
| 完工回報 Dialog | — | 維修說明+照片 | FN-04-016 |
| 巡查任務 | /admin/repair/inspection | 任務 CRUD | FN-04-030~033 |
| 巡查紀錄 | /admin/repair/inspection/:taskId/records | 紀錄 CRUD | FN-04-034~037 |
| 附件上傳 (共用) | — | AttachmentUploader | FN-04-006 |
| 附件瀏覽 (共用) | — | AttachmentGallery | FN-04-007~008 |
