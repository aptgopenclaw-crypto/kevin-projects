# 06 — 稽核模組（Audit Module）

## 1. 模組概述

稽核模組提供台北市路燈管理系統的操作紀錄追蹤能力，包含兩個層面：

1. **使用者事件日誌（UserEventLog）**：透過 `@AuditEvent` 註解 + AOP 自動攔截 API 操作，非同步寫入 `user_event_log` 表。記錄操作者、事件類型、API 端點、請求參數（脫敏）、IP 位址、User-Agent、執行耗時等。
2. **Hibernate Envers 版本追蹤**：透過 `AuditRevisionEntity` + `@RevisionEntity` 自動記錄實體變更歷程至 `rev_info` 表，每次版本記錄操作者 ID。

模組特點：
- **非同步寫入**：使用專屬執行緒池 `auditExecutor`（核心 2、最大 8、佇列 500），不影響業務回應時間
- **最佳努力（Best-effort）**：寫入失敗僅記錄 log，不拋出例外
- **敏感資料脫敏**：`PayloadSanitizer` 自動遮蔽 password/token 等欄位
- **自動清理**：排程任務每日凌晨 2 點清除超過 7 天的舊紀錄

---

## 2. 資料表結構

### 2.1 user_event_log

| 欄位 | 型別 | 約束 | 說明 |
|---|---|---|---|
| user_event_log_pk | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | | 租戶 ID（選租戶前可能為 NULL） |
| user_id | VARCHAR(50) | NOT NULL | 操作者 ID |
| username | VARCHAR(100) | | 帳號（email 優先） |
| user_label | VARCHAR(100) | | 顯示名稱 |
| email | VARCHAR(200) | | 電子郵件 |
| event_type | VARCHAR(50) | NOT NULL | 事件類型代碼 |
| event_desc | VARCHAR(50) | | 事件分類（AuditCategory 值） |
| api_endpoint | VARCHAR(100) | | API 請求 URI |
| payload | VARCHAR(2000) | | 請求參數（已脫敏） |
| error_code | VARCHAR(50) | | 結果碼（00000=成功, 99999=系統異常） |
| message | VARCHAR(50) | | 訊息 |
| ip_address | VARCHAR(50) | | 客戶端 IP（支援 X-Forwarded-For） |
| user_agent | VARCHAR(500) | | 瀏覽器 User-Agent |
| execution_time | BIGINT | | API 執行耗時（毫秒） |
| dept_id | BIGINT | | 操作者所屬部門 ID |
| create_time | TIMESTAMP WITH TIME ZONE | NOT NULL | 紀錄建立時間 |

### 2.2 rev_info（Hibernate Envers）

| 欄位 | 型別 | 約束 | 說明 |
|---|---|---|---|
| id | INTEGER | PK | Revision ID（繼承 DefaultRevisionEntity） |
| timestamp | BIGINT | NOT NULL | Revision 時間戳（繼承） |
| action_user_id | VARCHAR(50) | | 操作者 ID |

> `rev_info` 搭配 Envers 自動產生的 `*_aud` 表使用，追蹤所有 `@Audited` 實體的歷史版本。

---

## 3. 實體關聯

```
UserEventLogEntity (N) ──── user_id ────> (1) UserEntity
UserEventLogEntity (N) ──── tenant_id ──> (1) Tenant
UserEventLogEntity (N) ──── dept_id ────> (1) DeptInfo

AuditRevisionEntity (1) ──── revision ──> (N) *_aud 表（Envers 自動管理）
```

---

## 4. API 端點摘要

### `/v1/auth/audit`

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/categories` | 無特殊限制 | 取得所有稽核事件分類 |
| GET | `/user/usage/history` | ADMIN/SUPER_ADMIN/DEPT_ADMIN 或 AUDIT_LIST | 查詢操作紀錄（分頁、篩選） |
| GET | `/user/usage/history/export` | ADMIN/SUPER_ADMIN/DEPT_ADMIN 或 AUDIT_EXPORT | 匯出操作紀錄（CSV/XLSX），附 @AuditEvent |
| GET | `/user/login/my` | 登入使用者 | 查詢個人登入紀錄 |

**查詢參數**：

| 參數 | 說明 |
|---|---|
| userName | 使用者關鍵字（模糊搜尋 username / userLabel） |
| eventDesc | 事件分類篩選 |
| startTimestamp | 起始時間（ISO 8601 OffsetDateTime） |
| endTimestamp | 結束時間 |
| sortBy | 排序欄位（預設 createTime） |
| sort | 排序方向（ASC/DESC，預設 DESC） |
| page / pageSize | 分頁參數 |

---

## 5. 業務邏輯

### 5.1 AOP 自動攔截

`BaseLoggerAspect` 以 `@Around("@annotation(auditEvent)")` 攔截所有標註 `@AuditEvent` 的方法：

1. **進入前**：在主執行緒捕獲 tenantId、userId、username、deptId、URI、IP、User-Agent
2. **執行方法**：計時並捕獲例外（BusinessException 取 errorCode，其他為 99999）
3. **結束後**（finally）：呼叫 `AuditAsyncWriter.saveAsync()` 非同步寫入

### 5.2 非同步寫入

`AuditAsyncWriter.saveAsync()` 標註 `@Async("auditExecutor")`：
- 設定 SYSTEM context 繞過租戶過濾
- 查詢使用者 displayName 與 email（best-effort）
- 組裝並儲存 `UserEventLogEntity`
- 異常時僅 log.error，不影響業務

### 5.3 敏感資料脫敏

`PayloadSanitizer` 將以下欄位值替換為 `***`：
- `secret`, `password`, `newPassword`, `token`, `accessToken`, `refreshToken`

序列化後截斷至最大 2000 字元。

### 5.4 資料存取控制

查詢操作紀錄時依角色控制可視範圍：
- **DEPT_USER**：僅可查看自己的紀錄
- **ADMIN / SUPER_ADMIN / DEPT_ADMIN**：依 DataScope 查看部門範圍內的紀錄
- 使用 SYSTEM context 繞過 Hibernate @Filter，由 JPA Specification 手動處理 tenantId + deptId 過濾

### 5.5 匯出功能

- 支援 CSV（UTF-8 BOM）與 XLSX（Apache POI）格式
- 上限 10,000 筆
- 匯出本身也記錄稽核事件（`EXPORT_AUDIT`）

### 5.6 自動清理

`AuditPurgeJob` 每日 02:00 執行：
- 刪除 7 天前的 `user_event_log` 紀錄
- 使用 SYSTEM context 執行跨租戶操作

---

## 6. 資料流程

```
使用者呼叫 API（標註 @AuditEvent）
  │
  ▼
BaseLoggerAspect.logApiCall()
  ├─ 主執行緒：捕獲 tenantId, userId, username, deptId, URI, IP, UA
  ├─ pjp.proceed() → 執行業務邏輯
  └─ finally: AuditAsyncWriter.saveAsync()
       │
       ▼（非同步執行緒池 auditExecutor）
     AuditAsyncWriter
       ├─ TenantContext.setSystemContext()
       ├─ 查詢 UserEntity → displayName, email
       ├─ PayloadSanitizer.sanitize(args) → 脫敏參數
       ├─ 組裝 UserEventLogEntity
       ├─ userEventLogRepository.save()
       └─ TenantContext.clear()

Hibernate Envers 自動追蹤
  │
  ▼
任何 @Audited 實體的 INSERT/UPDATE/DELETE
  ├─ AuditRevisionListener.newRevision()
  │    └─ 設定 actionUserId
  └─ Envers 自動寫入 rev_info + *_aud 表

管理者查詢稽核日誌
  │
  ▼
GET /v1/auth/audit/user/usage/history
  ├─ 判斷角色（isAdmin?）
  ├─ 建立 Specification
  │    ├─ tenantId 過濾（OR NULL）
  │    ├─ DataScope 部門過濾（管理者）/ userId 過濾（一般使用者）
  │    ├─ eventDesc / userName / 時間範圍篩選
  │    └─ 排序設定
  ├─ TenantContext → SYSTEM（繞過 @Filter）
  └─ userEventLogRepository.findAll(spec, pageable)

每日 02:00 自動清理
  │
  ▼
AuditPurgeJob.purgeOldAuditLogs()
  └─ DELETE FROM user_event_log WHERE create_time < (now - 7d)
```

---

## 7. 列舉值定義

### AuditCategory（事件分類）

| 值 | 說明 |
|---|---|
| USER_AUTH | 使用者認證 |
| ACCOUNT | 帳號管理 |
| SYSTEM | 系統管理 |
| ASSET | 資產管理 |
| WORKFLOW | 工作流程 |
| MAINTENANCE | 報修維護 |
| MATERIAL | 材料管理 |
| REPLACEMENT | 換裝維護 |

### AuditEventType（事件類型，部分列舉）

| 值 | 分類 | 說明 |
|---|---|---|
| LOGIN_SUCCESS | USER_AUTH | 登入成功 |
| LOGIN_FAIL | USER_AUTH | 登入失敗 |
| LOGOUT | USER_AUTH | 登出 |
| IDLE_TIMEOUT_LOGOUT | USER_AUTH | 閒置逾時登出 |
| TENANT_SWITCH | USER_AUTH | 切換租戶 |
| CREATE_USER | ACCOUNT | 建立使用者 |
| UPDATE_USER | ACCOUNT | 更新使用者 |
| DISABLE_USER | ACCOUNT | 停用使用者 |
| SOFT_DELETE_USER | ACCOUNT | 軟刪除使用者 |
| CREATE_DEPT | SYSTEM | 建立部門 |
| UPDATE_DEPT | SYSTEM | 更新部門 |
| DELETE_DEPT | SYSTEM | 刪除部門 |
| CREATE_MENU | SYSTEM | 建立選單 |
| DELETE_MENU | SYSTEM | 刪除選單 |
| TOGGLE_VISIBLE | SYSTEM | 切換可見性 |
| CREATE_ANNOUNCEMENT | SYSTEM | 建立公告 |
| UPDATE_ANNOUNCEMENT | SYSTEM | 更新公告 |
| DELETE_ANNOUNCEMENT | SYSTEM | 刪除公告 |
| CREATE_DEVICE | ASSET | 建立設備 |
| UPDATE_DEVICE | ASSET | 更新設備 |
| DELETE_DEVICE | ASSET | 刪除設備 |
| EXPORT_DEVICE | ASSET | 匯出設備 |
| CREATE_CIRCUIT | ASSET | 建立迴路 |
| UPDATE_CIRCUIT | ASSET | 更新迴路 |
| CREATE_CONTRACT | ASSET | 建立合約 |
| UPDATE_CONTRACT | ASSET | 更新合約 |
| WORKFLOW_SUBMIT | WORKFLOW | 流程提交 |
| WORKFLOW_APPROVE | WORKFLOW | 流程核准 |
| WORKFLOW_REJECT | WORKFLOW | 流程駁回 |
| WORKFLOW_RETURN | WORKFLOW | 流程退回 |
| WORKFLOW_DISPATCH | WORKFLOW | 流程派工 |
| CREATE_REPAIR_TICKET | MAINTENANCE | 建立報修單 |
| UPDATE_REPAIR_TICKET | MAINTENANCE | 更新報修單 |
| CLOSE_REPAIR_TICKET | MAINTENANCE | 結案報修單 |
| DISPATCH_REPAIR | MAINTENANCE | 報修派工 |
| COMPLETE_REPAIR | MAINTENANCE | 報修完工 |
| CREATE_PURCHASE_ORDER | MATERIAL | 建立採購單 |
| RECEIVE_MATERIAL | MATERIAL | 材料入庫 |
| ISSUE_MATERIAL | MATERIAL | 材料出庫 |
| ADJUST_INVENTORY | MATERIAL | 庫存調整 |
| DISPOSE_MATERIAL | MATERIAL | 材料報廢 |
| IMPORT_APPROVED_MATERIAL | MATERIAL | 匯入核定材料 |
| CREATE_REPLACEMENT_ORDER | REPLACEMENT | 建立換裝單 |
| UPDATE_REPLACEMENT_ORDER | REPLACEMENT | 更新換裝單 |
| SELF_CHECK_REPLACEMENT | REPLACEMENT | 換裝自檢 |
| CLOSE_REPLACEMENT | REPLACEMENT | 換裝結案 |
| EXPORT_AUDIT | SYSTEM | 匯出稽核日誌 |

### 敏感欄位清單（PayloadSanitizer）

| 欄位名 | 脫敏處理 |
|---|---|
| secret | 替換為 `***` |
| password | 替換為 `***` |
| newPassword | 替換為 `***` |
| token | 替換為 `***` |
| accessToken | 替換為 `***` |
| refreshToken | 替換為 `***` |
