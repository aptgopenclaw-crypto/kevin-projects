# SA-01 系統管理 Function List

> **對應需求**：§2-(1) ~ §2-(10)  
> **SRS 對應**：SRS-02-001 ~ SRS-02-010  
> **Spec 來源**：`/02-spec/02-system-management.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-01-01 | 帳號管理 | SUPER_ADMIN, DEPT_ADMIN | 帳號 CRUD、停用/復用、密碼重設 |
| UC-01-02 | 角色權限管理 | SUPER_ADMIN | 角色 CRUD、權限指派、層級排序、權限複製 |
| UC-01-03 | 選單管理 | SUPER_ADMIN | 選單 CRUD、排序、圖示、路由 |
| UC-01-04 | 帳號密碼登入 | 所有使用者 | 帳密+驗證碼登入 |
| UC-01-05 | 臺北通登入 | GOV_*, OPERATOR, CONTRACTOR | 掃描 QRCode 驗證 |
| UC-01-06 | Taipeion 登入 | GOV_* | 機關人員驗證 |
| UC-01-07 | 密碼重設 | 所有使用者 | 簡訊/Email Token 重設 |
| UC-01-08 | 密碼變更 | 所有使用者 | 自行修改密碼 |
| UC-01-09 | 登入逾時 | 系統 | 閒置自動登出 |
| UC-01-10 | 登入登出紀錄查詢 | SUPER_ADMIN, GOV_MGR | 查看帳號登入登出歷程 |
| UC-01-11 | 公告管理 | SUPER_ADMIN, GOV_ADMIN | 公告 CRUD、排程發佈、受眾設定 |
| UC-01-12 | 公告查看 | 所有使用者 | 登入後查看公告、已讀追蹤 |
| UC-01-13 | 通知/待辦查看 | 所有使用者 | 即時通知、待辦案件列表 |
| UC-01-14 | 部門管理 | SUPER_ADMIN | 部門 CRUD、樹狀結構 |
| UC-01-15 | 系統參數設定 | SUPER_ADMIN | 閒置時間、密碼策略、前端 URL 等 |

---

## Function List

### 帳號管理 (§2-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-001 | 帳號列表查詢 | R | SUPER_ADMIN, DEPT_ADMIN | 關鍵字、角色、狀態、部門、分頁 | 帳號清單 (分頁) | DataScope 限制：DEPT_ADMIN 僅見所屬部門 | SRS-02-001 | §2-1 | GET /v1/auth/admin/users |
| FN-01-002 | 新增帳號 | C | SUPER_ADMIN, DEPT_ADMIN | 帳號、姓名、Email、電話、部門、角色、職務、使用對象 | 帳號資料 | 帳號不重複；密碼加密(bcrypt)；不得明文傳輸；DEPT_ADMIN 只能建本部門 | SRS-02-001 | §2-1 | POST /v1/auth/admin/users |
| FN-01-003 | 編輯帳號 | U | SUPER_ADMIN, DEPT_ADMIN | 帳號 ID、修改欄位 | 更新結果 | 帳號異動不影響既有資料（歷史紀錄保留原帳號名） | SRS-02-001 | §2-1 | PUT /v1/auth/admin/users/{id} |
| FN-01-004 | 停用帳號 | U | SUPER_ADMIN, DEPT_ADMIN | 帳號 ID | 更新結果 | 軟刪除（status=DISABLED）；不刪除資料 | SRS-02-001 | §2-1 | PUT /v1/auth/admin/users/{id}/disable |
| FN-01-005 | 復用帳號 | U | SUPER_ADMIN | 帳號 ID | 更新結果 | status=ACTIVE | SRS-02-001 | §2-1 | PUT /v1/auth/admin/users/{id}/enable |
| FN-01-006 | 重設帳號密碼（管理員） | U | SUPER_ADMIN | 帳號 ID、新密碼 | 更新結果 | 符合密碼規則；通知使用者 | SRS-02-001 | §2-1 | PUT /v1/auth/admin/users/{id}/reset-password |
| FN-01-007 | 指派角色 | U | SUPER_ADMIN, DEPT_ADMIN | 帳號 ID、角色 ID 清單 | 更新結果 | 一人可多角色 | SRS-02-001 | §2-1 | PUT /v1/auth/admin/users/{id}/roles |
| FN-01-008 | 查看帳號詳情 | R | SUPER_ADMIN, DEPT_ADMIN | 帳號 ID | 帳號完整資料 | DataScope 限制 | SRS-02-001 | §2-1 | GET /v1/auth/admin/users/{id} |
| FN-01-009 | 匯出帳號清單 | E | SUPER_ADMIN | 篩選條件 | ODS/XLS/CSV | DataScope 限制 | SRS-02-001 | §2-1 | GET /v1/auth/admin/users/export |

### 角色權限管理 (§2-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-010 | 角色列表查詢 | R | SUPER_ADMIN | 分頁 | 角色清單 | — | SRS-02-002 | §2-2 | GET /v1/auth/admin/roles |
| FN-01-011 | 新增角色 | C | SUPER_ADMIN | 角色名稱、描述、層級、DataScope | 角色資料 | 角色名稱不重複 | SRS-02-002 | §2-2 | POST /v1/auth/admin/roles |
| FN-01-012 | 編輯角色 | U | SUPER_ADMIN | 角色 ID、修改欄位 | 更新結果 | 內建角色不可刪除 | SRS-02-002 | §2-2 | PUT /v1/auth/admin/roles/{id} |
| FN-01-013 | 刪除角色 | D | SUPER_ADMIN | 角色 ID | 刪除結果 | 有使用者綁定時不可刪除 | SRS-02-002 | §2-2 | DELETE /v1/auth/admin/roles/{id} |
| FN-01-014 | 指派角色權限 | U | SUPER_ADMIN | 角色 ID、權限 ID 清單 | 更新結果 | — | SRS-02-002 | §2-2 | PUT /v1/auth/admin/roles/{id}/permissions |
| FN-01-015 | 查看角色權限 | R | SUPER_ADMIN | 角色 ID | 權限樹 | — | SRS-02-002 | §2-2 | GET /v1/auth/admin/roles/{id}/permissions |
| FN-01-016 | 移動角色層級 | U | SUPER_ADMIN | 角色 ID、新排序 | 更新結果 | 影響權限繼承層級 | SRS-02-002 | §2-2 | PUT /v1/auth/admin/roles/{id}/sort |
| FN-01-017 | 複製角色權限 | C | SUPER_ADMIN | 來源角色 ID、新角色名稱 | 新角色資料 | 複製所有權限到新角色 | SRS-02-002 | §2-2 | POST /v1/auth/admin/roles/{id}/copy |

### 選單管理

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-018 | 選單樹查詢 | R | SUPER_ADMIN | — | 選單樹 | — | SRS-02-002 | §2-2 | GET /v1/auth/admin/menus/tree |
| FN-01-019 | 新增選單 | C | SUPER_ADMIN | 名稱、路徑、元件、圖示、父選單、排序、類型 | 選單資料 | 支援 DIRECTORY/PAGE/BUTTON 三種類型 | SRS-02-002 | §2-2 | POST /v1/auth/admin/menus |
| FN-01-020 | 編輯選單 | U | SUPER_ADMIN | 選單 ID、修改欄位 | 更新結果 | — | SRS-02-002 | §2-2 | PUT /v1/auth/admin/menus/{id} |
| FN-01-021 | 刪除選單 | D | SUPER_ADMIN | 選單 ID | 刪除結果 | 有子選單時不可刪除 | SRS-02-002 | §2-2 | DELETE /v1/auth/admin/menus/{id} |

### 登入管理 (§2-3, §2-4)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-022 | 取得驗證碼 | R | 匿名 | — | 驗證碼圖片 + captchaKey | Base64 圖片 + UUID key | SRS-02-003 | §2-3 | GET /v1/public/captcha |
| FN-01-023 | 帳密登入 | P | 匿名 | 帳號、密碼、captchaKey、captchaCode | JWT Token + UserInfo | 驗證碼正確；密碼 bcrypt 比對；登入失敗 ≥5 次鎖定 30 分鐘；寫稽核日誌 | SRS-02-003 | §2-3 | POST /v1/public/auth/login |
| FN-01-024 | 臺北通 QRCode 登入 | P | 匿名 | QRCode 回傳之 authCode | JWT Token + UserInfo | 向臺北通 OAuth 取 user profile → 比對本系統帳號 → 簽發 JWT | SRS-02-003 | §2-3 | POST /v1/public/auth/taipei-pass |
| FN-01-025 | Taipeion 登入 | P | 匿名 | Taipeion SAML assertion | JWT Token + UserInfo | 解析 SAML → 比對帳號 → 簽發 JWT | SRS-02-003 | §2-3 | POST /v1/public/auth/taipeion |
| FN-01-026 | Token 刷新 | P | 已認證 | refreshToken | 新 JWT Token | refreshToken 有效期內 | SRS-02-003 | §2-3 | POST /v1/auth/refresh |
| FN-01-027 | 登出 | P | 已認證 | — | — | 作廢 Token；寫稽核日誌 | SRS-02-003 | §2-3 | POST /v1/auth/logout |

### 身分驗證 — 密碼規則 (§2-4)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-028 | 密碼強度驗證 | P | 系統 | 密碼字串 | 驗證結果 | ≥12 字元；含大小寫+數字+特殊字元；不可與前 3 次密碼相同 | SRS-02-004 | §2-4 | (內部呼叫) |
| FN-01-029 | 變更密碼 | U | 已認證 | 舊密碼、新密碼 | 更新結果 | 舊密碼正確；新密碼符合規則；寫稽核日誌 | SRS-02-004 | §2-4 | PUT /v1/auth/password |
| FN-01-030 | 定期換密碼提醒 | N | 系統 | — | 通知 | 密碼超過 90 天未更換 → 登入後強制提醒 | SRS-02-004 | §2-4 | (登入時觸發) |

### 密碼重設 (§2-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-031 | 申請密碼重設 | P | 匿名 | Email 或手機號碼 | 成功訊息 | 發送一次性 Token（有效 30 分鐘）至 Email/簡訊；防暴力（1 分鐘限 1 次） | SRS-02-005 | §2-5 | POST /v1/public/auth/forgot-password |
| FN-01-032 | 驗證重設 Token | P | 匿名 | Token | 驗證結果 | Token 未過期 + 未使用 | SRS-02-005 | §2-5 | GET /v1/public/auth/reset-password/verify |
| FN-01-033 | 執行密碼重設 | U | 匿名 | Token、新密碼 | 更新結果 | Token 有效；新密碼符合規則；Token 用後即毀 | SRS-02-005 | §2-5 | POST /v1/public/auth/reset-password |

### 登入逾時 (§2-6)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-034 | 閒置偵測 | P | 前端 | 滑鼠/鍵盤事件 | 倒數警告 / 自動登出 | 閒置時間由系統參數設定（預設 30 分鐘）；最後 60 秒彈窗警告 | SRS-02-006 | §2-6 | (前端邏輯) |
| FN-01-035 | 閒置時間設定 | U | SUPER_ADMIN | 閒置分鐘數 | 更新結果 | 最小 5 分鐘，最大 480 分鐘 | SRS-02-006 | §2-6 | PUT /v1/auth/admin/settings/idle-timeout |

### 登入登出紀錄 (§2-7)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-036 | 登入登出紀錄查詢 | R | SUPER_ADMIN, GOV_MGR | 帳號、時間範圍、事件類型、分頁 | 稽核紀錄清單 | 查詢 AuditCategory=USER_AUTH 的日誌 | SRS-02-007 | §2-7 | GET /v1/auth/audit |
| FN-01-037 | 登入登出紀錄匯出 | E | SUPER_ADMIN | 篩選條件 | ODS/XLS/CSV | — | SRS-02-007 | §2-7 | GET /v1/auth/audit/export |

### 公告管理 (§2-9)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-038 | 公告列表查詢 | R | SUPER_ADMIN, GOV_ADMIN | 狀態、關鍵字、分頁 | 公告清單 | — | SRS-02-009 | §2-9 | GET /v1/auth/announcements |
| FN-01-039 | 新增公告 | C | SUPER_ADMIN, GOV_ADMIN | 標題、內容、受眾（全體/指定部門）、置頂、排程時間 | 公告資料 | 預設狀態=DRAFT；排程時間 > 現在 | SRS-02-009 | §2-9 | POST /v1/auth/announcements |
| FN-01-040 | 編輯公告 | U | SUPER_ADMIN, GOV_ADMIN | 公告 ID、修改欄位 | 更新結果 | DRAFT 狀態才可編輯 | SRS-02-009 | §2-9 | PUT /v1/auth/announcements/{id} |
| FN-01-041 | 發佈公告 | U | SUPER_ADMIN, GOV_ADMIN | 公告 ID | 更新結果 | 狀態 → PUBLISHED；或設定排程自動發佈 | SRS-02-009 | §2-9 | PUT /v1/auth/announcements/{id}/publish |
| FN-01-042 | 刪除公告 | D | SUPER_ADMIN | 公告 ID | 刪除結果 | 軟刪除 | SRS-02-009 | §2-9 | DELETE /v1/auth/announcements/{id} |
| FN-01-043 | 公告欄（使用者端） | R | 所有使用者 | 分頁 | 已發佈公告 | 僅顯示受眾包含當前使用者的公告 | SRS-02-009 | §2-9 | GET /v1/auth/announcements/board |
| FN-01-044 | 標記公告已讀 | U | 所有使用者 | 公告 ID | — | 記錄已讀時間 | SRS-02-009 | §2-9 | PUT /v1/auth/announcements/{id}/read |

### 通知/待辦 (§2-10)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-045 | 通知列表查詢 | R | 所有使用者 | 已讀/未讀、類型、分頁 | 通知清單 | 僅顯示當前使用者的通知 | SRS-02-010 | §2-10 | GET /v1/auth/notifications |
| FN-01-046 | 未讀通知數量 | R | 所有使用者 | — | 未讀數量 | — | SRS-02-010 | §2-10 | GET /v1/auth/notifications/unread-count |
| FN-01-047 | 標記通知已讀 | U | 所有使用者 | 通知 ID 或全部 | — | — | SRS-02-010 | §2-10 | PUT /v1/auth/notifications/{id}/read |
| FN-01-048 | 待辦案件列表 | R | 所有使用者 | 類型、分頁 | 待辦清單 | 含案件類型、直接連結到案件 | SRS-02-010 | §2-10 | GET /v1/auth/notifications/todos |
| FN-01-049 | 即時通知推送 | N | 系統 | 事件觸發 | WebSocket 訊息 | 案件狀態變更、新待辦、新公告 → 即時推送 | SRS-02-010 | §2-10 | WebSocket /ws/notifications |

### 部門管理

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-050 | 部門樹查詢 | R | SUPER_ADMIN | — | 部門樹 | — | SRS-02-001 | §2-1 | GET /v1/auth/admin/depts/tree |
| FN-01-051 | 新增部門 | C | SUPER_ADMIN | 名稱、父部門、排序 | 部門資料 | 名稱不重複（同層級） | SRS-02-001 | §2-1 | POST /v1/auth/admin/depts |
| FN-01-052 | 編輯部門 | U | SUPER_ADMIN | 部門 ID、修改欄位 | 更新結果 | — | SRS-02-001 | §2-1 | PUT /v1/auth/admin/depts/{id} |
| FN-01-053 | 刪除部門 | D | SUPER_ADMIN | 部門 ID | 刪除結果 | 有子部門或使用者時不可刪除 | SRS-02-001 | §2-1 | DELETE /v1/auth/admin/depts/{id} |

### 系統參數設定

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-01-054 | 查詢系統參數 | R | SUPER_ADMIN | — | 參數列表 | — | SRS-02-006 | §2-6 | GET /v1/auth/admin/settings |
| FN-01-055 | 更新系統參數 | U | SUPER_ADMIN | key、value | 更新結果 | 支援：IDLE_TIMEOUT, PASSWORD_MIN_LENGTH, PASSWORD_EXPIRY_DAYS, FRONTEND_BASE_URL | SRS-02-006 | §2-6 | PUT /v1/auth/admin/settings/{key} |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 帳號管理 | /admin/users | 帳號 CRUD + 角色指派 | FN-01-001~009 |
| 角色管理 | /admin/roles | 角色 CRUD + 權限指派 | FN-01-010~017 |
| 選單管理 | /admin/menus | 選單樹 CRUD | FN-01-018~021 |
| 部門管理 | /admin/depts | 部門樹 CRUD | FN-01-050~053 |
| 登入頁 | /login | 帳密/臺北通/Taipeion | FN-01-022~025 |
| 忘記密碼 | /forgot-password | 申請重設 | FN-01-031~033 |
| 變更密碼 | /change-password | 自行修改 | FN-01-029 |
| 公告管理 | /admin/announcements | 公告 CRUD | FN-01-038~042 |
| 公告欄 | /announcements | 查看公告 | FN-01-043~044 |
| 通知中心 | /notifications | 通知+待辦 | FN-01-045~049 |
| 稽核紀錄 | /admin/audit | 登入登出查詢 | FN-01-036~037 |
| 系統設定 | /admin/settings | 參數設定 | FN-01-054~055 |
