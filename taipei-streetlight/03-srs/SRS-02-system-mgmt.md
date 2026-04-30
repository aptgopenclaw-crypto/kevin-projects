# SRS-02 系統管理

> **對應需求**：§2-(1) ~ §2-(10)  
> **設計參照**：`/02-spec/02-system-management.md`、`/_archive/x-plan/2-6-登入預期機制.md`、`/_archive/x-plan/2-9-系統公告發佈.md`  
> **狀態**：⚠️ 7/10 完成

---

## SRS-02-001 帳號管理

**來源**：§2-(1)

### User Story

> 身為 **GOV_ADMIN**，我需要新增/編輯/刪除帳號，設定使用者基本資料與權限，以管控系統存取。

### 主要流程

1. 管理員新增帳號，填入：使用對象區分、姓名、帳號、職務（角色）、在職/離職
2. 密碼加密儲存（bcrypt），不以明文傳輸（HTTPS + JWT）
3. 帳號異動（如離職）不影響該帳號歷史操作紀錄的正確性
4. 帳號停用後無法登入，但相關歷史案件保留原承辦人資訊

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-001-1 | 可新增/編輯/停用帳號，密碼以 bcrypt 雜湊儲存 |
| AC-02-001-2 | 所有 API 傳輸走 HTTPS，JWT Token 不含密碼明文 |
| AC-02-001-3 | 帳號停用/離職後，歷史案件仍顯示原承辦人姓名 |
| AC-02-001-4 | 使用對象含：機關、專案管理、監造、施工維護廠商、一般民眾 |

### 資料模型

```
users: id, username, password_hash, display_name, user_type, role_id, 
       dept_id, employment_status(ACTIVE/RESIGNED), tenant_id, ...
```

### API 端點

| Method | Path | 說明 |
|--------|------|------|
| GET | `/v1/auth/users` | 列表（分頁篩選） |
| POST | `/v1/auth/users` | 新增 |
| PUT | `/v1/auth/users/{id}` | 編輯 |
| DELETE | `/v1/auth/users/{id}` | 停用（軟刪除） |
| PUT | `/v1/auth/users/{id}/password` | 修改密碼 |

### 狀態：✅ 已完成

---

## SRS-02-002 人員層級及權限設定管理

**來源**：§2-(2)

### User Story

> 身為 **GOV_ADMIN**，我需要依不同使用對象設定至少 3 層角色權限，並可彈性新增/修改/刪除角色、調整順序、複製權限。

### 主要流程

1. 每種使用對象至少 3 層角色（如機關：承辦→股長→主管→首長→管理員）
2. 管理員可新增角色、設定該角色可操作的權限項目
3. 可調整角色排列順序（sort_order）
4. 可將某角色的權限複製到另一角色
5. 可停用角色（不影響已指派使用者的歷史紀錄）

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-002-1 | 每種使用對象至少 3 種角色層級 |
| AC-02-002-2 | 可新增/修改/刪除角色，可調整排列順序 |
| AC-02-002-3 | 可設定角色可操作之權限清單 |
| AC-02-002-4 | 可複製某角色的權限到另一角色（一鍵複製） |
| AC-02-002-5 | 停用角色後，已指派該角色的使用者歷史紀錄不受影響 |

### 資料模型

```
roles: id, code, name, user_type, sort_order, status, tenant_id
permissions: id, code, name, group_name, sort_order
role_permissions: role_id, permission_id
```

### API 端點

| Method | Path | 說明 |
|--------|------|------|
| GET | `/v1/auth/roles` | 角色列表 |
| POST | `/v1/auth/roles` | 新增角色 |
| PUT | `/v1/auth/roles/{id}` | 編輯角色 |
| PUT | `/v1/auth/roles/{id}/permissions` | 設定權限 |
| POST | `/v1/auth/roles/{id}/copy-permissions` | 複製權限 |

### 狀態：⚠️ RBAC 已完成，缺「權限複製」功能

---

## SRS-02-003 登入管理

**來源**：§2-(3)

### User Story

> 身為**使用者**，我可透過台北通掃描 QR Code 登入，或以帳號密碼+驗證碼登入；機關人員亦可透過 Taipeion 驗證。

### 主要流程

**路徑 A — 台北通 QR Code 登入（機關/廠商）**
1. 系統顯示 QR Code（含 session nonce）
2. 使用者以台北通 APP 掃描
3. 台北通回傳身分驗證 Token
4. 系統驗證 Token → 比對本地帳號 → 核發 JWT

**路徑 B — 帳號密碼+驗證碼登入**
1. 使用者輸入帳號、密碼、圖形驗證碼
2. 系統驗證驗證碼 → 驗證帳號密碼 → 核發 JWT

**路徑 C — Taipeion 登入（機關）**
1. 重導至 Taipeion SSO 頁面
2. 驗證成功回傳至平台 → 比對本地帳號 → 核發 JWT

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-003-1 | 帳號密碼登入時，必須通過圖形驗證碼或 reCAPTCHA 驗證 |
| AC-02-003-2 | 支援台北通 QR Code 掃描登入（OAuth2 流程） |
| AC-02-003-3 | 支援 Taipeion SSO 登入 |
| AC-02-003-4 | 登入成功核發 JWT，Token 有效期可系統設定 |
| AC-02-003-5 | 連續登入失敗 N 次鎖定帳號（N 可設定） |

### 外部介接

| 系統 | 協議 | 狀態 |
|------|------|------|
| 台北通 | OAuth2 / QR Code | EXT-01，待申請 |
| Taipeion | SAML / OAuth2 | EXT-02，待申請 |

### 狀態：⚠️ JWT + 驗證碼已完成，台北通/Taipeion 待介接

---

## SRS-02-004 身分驗證（密碼政策）

**來源**：§2-(4)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-004-1 | 密碼長度 ≥ 12 字元 |
| AC-02-004-2 | 必須包含英文大寫、小寫、數字及特殊字元 |
| AC-02-004-3 | 不可與前 N 次密碼相同（N 可設定，預設 3） |
| AC-02-004-4 | 系統可設定密碼到期天數（預設 90 天），到期提醒使用者更換 |
| AC-02-004-5 | 密碼到期後強制變更，否則無法進入系統 |

### 狀態：⚠️ 密碼複雜度已實作，需調整最小長度 8→12，需增加定期更換提醒

---

## SRS-02-005 密碼重設機制

**來源**：§2-(5)

### User Story

> 身為**使用者**，忘記密碼時可透過 SMS 或 Email 收到一次性時效令牌，驗證後重設密碼。

### 主要流程

1. 使用者點選「忘記密碼」，輸入帳號及手機或 Email
2. 系統產生一次性 Token（UUID），記錄至 `password_reset_tokens`
3. 透過 SMS 或 Email 發送 Token（有效期 15 分鐘）
4. 使用者點擊連結或輸入驗證碼
5. 系統驗證 Token 有效性（未過期、未使用）
6. 使用者輸入新密碼（須符合密碼政策）
7. 更新密碼，Token 標記為已使用

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-005-1 | 可透過手機簡訊收到重設驗證碼 |
| AC-02-005-2 | 可透過 Email 收到重設連結 |
| AC-02-005-3 | Token 為一次性使用，過期自動失效 |
| AC-02-005-4 | Token 有效時間可系統設定（預設 15 分鐘） |
| AC-02-005-5 | 重設密碼須符合密碼政策（SRS-02-004） |

### 資料模型

```
password_reset_tokens: id, user_id, token, channel(SMS/EMAIL), 
                       expires_at, used_at, created_at
```

### 狀態：❌ 未實作

---

## SRS-02-006 登入逾時機制

**來源**：§2-(6)

### User Story

> 身為 **GOV_ADMIN**，我可設定系統閒置逾時時間；使用者閒置超過設定時間後自動登出。

### 主要流程

1. 管理員於系統設定調整閒置超時時間（1–480 分鐘，預設 15）
2. 前端偵測使用者操作事件（mouse/keyboard/scroll/touch），30 秒 throttle
3. 閒置時間到達前 2 分鐘顯示警告 Dialog（不可關閉）
4. 使用者操作即重設計時器
5. 逾時自動呼叫 `/v1/auth/idle-logout` 並清除本地狀態
6. 跨分頁透過 BroadcastChannel 同步閒置狀態

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-006-1 | 管理員可設定閒置超時時間（1–480 分鐘） |
| AC-02-006-2 | 閒置超時後自動登出，清除 JWT Token |
| AC-02-006-3 | 登出前 2 分鐘顯示不可關閉的倒數警告 |
| AC-02-006-4 | 多分頁間閒置狀態同步 |
| AC-02-006-5 | 閒置登出事件記錄於稽核日誌 |

### API 端點

| Method | Path | 說明 |
|--------|------|------|
| GET | `/v1/auth/system-settings/idle-timeout` | 取得閒置超時設定 |
| PUT | `/v1/auth/system-settings/idle-timeout` | 修改閒置超時 |
| POST | `/v1/auth/idle-logout` | 閒置登出（稽核記錄） |

### 狀態：✅ 已完成（含 220+ 測試）

---

## SRS-02-007 登入和登出紀錄

**來源**：§2-(7)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-007-1 | 每次登入/登出自動記錄（時間、IP、裝置資訊） |
| AC-02-007-2 | 管理員可依帳號、時間範圍查詢登入登出紀錄 |
| AC-02-007-3 | 包含正常登出、閒置登出、Token 過期等不同登出類型 |

### 資料模型

- 使用 `audit_logs` 表，`event_type` = `LOGIN` / `LOGOUT` / `IDLE_TIMEOUT_LOGOUT`

### 狀態：✅ 已完成

---

## SRS-02-008 資安合規

**來源**：§2-(8)

> 符合「資通安全責任等級分級辦法」附表十「資通系統防護基準」之「普級」規定。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-008-1 | 存取控制：RBAC 角色權限、最小權限原則 |
| AC-02-008-2 | 識別與鑑別：強密碼政策、多因子驗證（台北通） |
| AC-02-008-3 | 稽核與可歸責性：所有關鍵操作留稽核紀錄 |
| AC-02-008-4 | 通訊安全：HTTPS/TLS 1.2+、JWT 簽章 |
| AC-02-008-5 | 系統完整性：輸入驗證、SQL Injection/XSS 防護 |
| AC-02-008-6 | 可用性：定期備份、故障恢復機制 |
| AC-02-008-7 | 通過第三方資安檢測（弱點掃描、滲透測試） |

### 狀態：⚠️ 大部分已實作，需逐項對照「普級」清單稽核

---

## SRS-02-009 系統公告發佈

**來源**：§2-(9)

### User Story

> 身為 **GOV_STAFF**（具公告權限），我可刊登公告；所有使用者登入後可查看公告。

### 主要流程

1. 有權限人員建立公告（標題、內容、對象範圍、發布時間、到期時間）
2. 公告可為「草稿」或「已發布」，支援排程發布
3. 對象範圍：ALL（全體）或 DEPT（指定部門）
4. 使用者登入後首頁顯示未讀公告提示（Header Bell）
5. 可標記已讀 / 一鍵全讀
6. 置頂公告優先顯示

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-009-1 | 具權限者可建立/編輯/刪除公告，支援草稿與發布 |
| AC-02-009-2 | 公告可指定對象（全體或指定部門） |
| AC-02-009-3 | 支援排程發布（publish_at）與自動到期（expire_at） |
| AC-02-009-4 | 登入後顯示未讀公告數量（Badge），可展開查看 |
| AC-02-009-5 | 可標記已讀、一鍵全讀 |

### 資料模型

```
announcements: id, tenant_id, title, content, status(DRAFT/PUBLISHED), 
               scope(ALL/DEPT), pinned, publish_at, expire_at, created_by
announcement_depts: announcement_id, dept_id
announcement_reads: announcement_id, user_id, read_at
```

### API 端點

| Method | Path | 說明 |
|--------|------|------|
| GET | `/v1/auth/announcements` | 列表（使用者/管理端） |
| POST | `/v1/auth/announcements` | 新增 |
| PUT | `/v1/auth/announcements/{id}` | 編輯 |
| DELETE | `/v1/auth/announcements/{id}` | 刪除 |
| GET | `/v1/auth/announcements/unread-count` | 未讀數量 |
| POST | `/v1/auth/announcements/{id}/read` | 標記已讀 |
| POST | `/v1/auth/announcements/read-all` | 一鍵全讀 |

### 狀態：✅ 已完成

---

## SRS-02-010 通知提示（待辦/列管）

**來源**：§2-(10)

### User Story

> 身為**使用者**，登入後即時看到我的待辦案件與列管案件提示，點選可直接進入案件操作簽核。

### 主要流程

1. 使用者登入後，系統自動查詢其待辦案件（依角色/代理）
2. 首頁或 Header 顯示待辦數量 Badge
3. 展開後列出各案件類型、狀態、剩餘時效
4. 點選案件直接跳轉至該案件詳情頁面
5. 可直接在通知面板執行簽核（核准/退回）或抽回

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-02-010-1 | 登入後即時顯示待辦案件數量 |
| AC-02-010-2 | 待辦列表含案件類型、編號、狀態、剩餘時效 |
| AC-02-010-3 | 點選案件可直接跳轉至詳情頁面 |
| AC-02-010-4 | 可在通知面板直接執行簽核（核准/退回）或抽回 |
| AC-02-010-5 | 支援即時推送（WebSocket 或 5 分鐘輪詢） |

### 資料模型

```
notifications: id, user_id, type(TODO/ALERT/INFO), title, content, 
               ref_type(REPAIR/REPLACEMENT/WORKFLOW), ref_id, read, created_at
```

### 狀態：❌ 未實作（依賴工作流模組完成後銜接）
