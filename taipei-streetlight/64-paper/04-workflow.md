# 04 — 工作流程模組（Workflow Module）

## 1. 模組概述

工作流程模組為台北市路燈管理系統的核心簽核引擎，採用有限狀態機（FSM）架構實現多種業務流程的狀態轉換控制。模組涵蓋：

- **流程定義**（WorkflowDefinition）：定義流程類型與名稱
- **流程實例**（WorkflowInstance）：每張工單對應一個流程實例，追蹤當前步驟與狀態
- **步驟日誌**（WorkflowStepLog）：記錄每次狀態轉換的操作者、動作、附件與快照
- **步驟範本**（WorkflowStepsTemplate）：定義每種流程的步驟順序、角色要求與逾時設定
- **代理設定**（DelegateSetting）：支援簽核代理人機制，含日期區間與重疊檢查

系統支援多租戶（Tenant Filter），所有具業務歸屬的表都透過 `tenant_id` 進行資料隔離。

---

## 2. 資料表結構

### 2.1 workflow_definitions

| 欄位 | 型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| workflow_type | VARCHAR(50) | NOT NULL, UNIQUE | 流程類型代碼 |
| workflow_name | VARCHAR(200) | NOT NULL | 流程名稱 |
| description | TEXT | | 流程說明 |
| is_active | BOOLEAN | NOT NULL | 是否啟用 |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.2 workflow_instances

| 欄位 | 型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶 ID |
| workflow_type | VARCHAR(50) | NOT NULL | 流程類型 |
| ticket_type | VARCHAR(50) | NOT NULL | 工單類型 |
| ticket_id | BIGINT | NOT NULL | 關聯工單 ID |
| current_step | VARCHAR(50) | NOT NULL | 當前步驟代碼 |
| status | VARCHAR(20) | NOT NULL, ENUM | 流程狀態 |
| assigned_to | VARCHAR(50) | | 目前簽核人 |
| creator_id | VARCHAR(50) | NOT NULL | 流程發起人 |
| started_at | TIMESTAMP | NOT NULL | 流程啟動時間 |
| completed_at | TIMESTAMP | | 流程完成時間 |
| created_at | TIMESTAMP | NOT NULL, 不可更新, @CreatedDate | 建立時間 |
| updated_at | TIMESTAMP | @LastModifiedDate | 更新時間 |

### 2.3 workflow_step_logs

| 欄位 | 型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶 ID |
| instance_id | BIGINT | NOT NULL | 關聯流程實例 ID |
| step_code | VARCHAR(50) | NOT NULL | 步驟代碼 |
| action | VARCHAR(30) | NOT NULL | 操作動作 |
| actor_id | VARCHAR(50) | NOT NULL | 操作者 ID |
| actor_name | VARCHAR(100) | | 操作者名稱 |
| original_assignee_id | VARCHAR(50) | | 原始簽核人（代理時填入） |
| is_delegated | BOOLEAN | NOT NULL | 是否為代理簽核 |
| comment | TEXT | | 簽核意見 |
| attachments | JSONB | | 附件列表 |
| before_snapshot | JSONB | | 轉換前快照 |
| after_snapshot | JSONB | | 轉換後快照 |
| acted_at | TIMESTAMP | NOT NULL | 操作時間 |

### 2.4 workflow_steps_template

| 欄位 | 型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| workflow_type | VARCHAR(50) | NOT NULL | 所屬流程類型 |
| step_order | INTEGER | NOT NULL | 步驟順序 |
| step_code | VARCHAR(50) | NOT NULL | 步驟代碼 |
| step_name | VARCHAR(200) | NOT NULL | 步驟名稱 |
| required_role | VARCHAR(50) | | 所需角色 |
| auto_action | VARCHAR(50) | | 自動動作 |
| timeout_hours | INTEGER | | 逾時小時數 |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |

### 2.5 delegate_settings

| 欄位 | 型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶 ID |
| delegator_id | VARCHAR(50) | NOT NULL | 委託人 ID |
| delegate_id | VARCHAR(50) | NOT NULL | 代理人 ID |
| start_date | DATE | NOT NULL | 代理起始日 |
| end_date | DATE | NOT NULL | 代理結束日 |
| reason | VARCHAR(200) | | 代理原因 |
| is_active | BOOLEAN | NOT NULL | 是否啟用 |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |

---

## 3. 實體關聯

```
WorkflowDefinition (1) ──── workflow_type ────> (N) WorkflowStepsTemplate
WorkflowDefinition (1) ──── workflow_type ────> (N) WorkflowInstance
WorkflowInstance   (1) ──── instance_id ──────> (N) WorkflowStepLog
WorkflowInstance   (N) ──── assigned_to ──────> (1) User
DelegateSetting    (N) ──── delegator_id ─────> (1) User（委託人）
DelegateSetting    (N) ──── delegate_id ──────> (1) User（代理人）
WorkflowInstance   (1) ──── ticket_type+ticket_id ──> (1) 各類工單（FaultTicket / RepairTicket / ReplacementOrder）
```

---

## 4. API 端點摘要

### 4.1 工作流程 `/v1/auth/workflow`

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/pending?page=&size=` | WORKFLOW_VIEW | 查詢我的待辦（含代理案件） |
| GET | `/{instanceId}/logs` | WORKFLOW_VIEW | 查詢流程歷程 |
| POST | `/{instanceId}/transition` | WORKFLOW_VIEW | 執行狀態轉換（附 @AuditEvent） |
| POST | `/{instanceId}/cancel` | WORKFLOW_VIEW | 取消流程 |

### 4.2 代理設定 `/v1/auth/delegates`

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/candidates` | DELEGATE_MANAGE | 取得可選代理人清單 |
| GET | `/` | DELEGATE_MANAGE | 查詢我的代理設定列表 |
| POST | `/` | DELEGATE_MANAGE | 新增代理設定 |
| DELETE | `/{id}` | DELEGATE_MANAGE | 停用代理設定 |

---

## 5. 業務邏輯

### 5.1 有限狀態機（FSM）

系統內建五種流程，每種流程定義合法的步驟轉換路徑：

**FAULT_REVIEW（通報審查）**
```
OPEN → REVIEW → CONFIRMED | REJECTED | MERGED
```

**REPAIR_DISPATCH（報修派工）**
```
PENDING → ACCEPTED → DISPATCHED → IN_PROGRESS | TRANSFERRED
IN_PROGRESS → COMPLETION_REPORTED
```

**REPAIR_CLOSE（報修結案）**
```
COMPLETION_REPORTED → PENDING_REVIEW → CLOSED | RETURNED
RETURNED → COMPLETION_REPORTED
```

**REPLACEMENT_REVIEW（換裝審查）**
```
DRAFT → DISPATCHED → IN_PROGRESS → SELF_CHECKED → PENDING_REVIEW → CLOSED | RETURNED
RETURNED → PENDING_REVIEW
```

**ASSET_CHANGE（資產異動）**
```
DRAFT → PENDING_REVIEW → APPROVED | RETURNED
APPROVED → APPLIED
RETURNED → PENDING_REVIEW
```

**終態步驟**：`CONFIRMED`, `REJECTED`, `MERGED`, `CLOSED`, `APPLIED`, `TRANSFERRED` — 到達終態後流程狀態設為 COMPLETED。

### 5.2 狀態轉換規則

1. 驗證目標步驟是否為當前步驟的合法後續
2. **自審防護**：`creator_id == actor_id` 時拒絕操作
3. **代理人判斷**：若操作者非 `assigned_to`，檢查是否為有效代理人（日期區間內且 `is_active = true`）
4. 寫入 `workflow_step_logs`（代理時自動加註 `[代理簽核]` 前綴）
5. 更新 `workflow_instances` 的 `current_step` 與 `status`
6. 發布 `WorkflowTransitionEvent`（Spring ApplicationEvent），由通知模組等下游監聽

### 5.3 代理設定規則

- 不允許自我代理
- 日期區間不可與既有生效代理重疊
- 查詢待辦時自動納入「他人委託給我」的案件

---

## 6. 資料流程

```
使用者建立工單
  │
  ▼
WorkflowService.createInstance()
  ├─ 依 workflowType 查表取得 initialStep
  ├─ 建立 WorkflowInstance (status=ACTIVE)
  └─ 回傳 instanceId
  
使用者執行簽核操作
  │
  ▼
POST /v1/auth/workflow/{id}/transition
  │
  ▼
WorkflowServiceImpl.transition()
  ├─ 1. 查詢 WorkflowInstance
  ├─ 2. 驗證 status == ACTIVE
  ├─ 3. FSM 轉換表驗證合法性
  ├─ 4. 自審防護檢查
  ├─ 5. 代理人檢查（DelegateSettingRepository）
  ├─ 6. 寫入 WorkflowStepLog
  ├─ 7. 更新 Instance（currentStep / status）
  └─ 8. publishEvent(WorkflowTransitionEvent)
         │
         ▼
    通知模組 Listeners（非同步）
    ├─ CompletionReportedNotificationListener
    ├─ RepairDispatchNotificationListener
    ├─ RepairClosedNotificationListener
    └─ ReplacementDispatchNotificationListener

查詢待辦
  │
  ▼
GET /v1/auth/workflow/pending
  ├─ 收集 assigneeIds = { userId } ∪ { 委託人IDs（他人委託給我的） }
  └─ findPendingByAssignees(assigneeIds, ACTIVE)
```

---

## 7. 列舉值定義

### WorkflowStatus

| 值 | 說明 |
|---|---|
| ACTIVE | 進行中 |
| COMPLETED | 已完成 |
| CANCELLED | 已取消 |

### WorkflowAction

| 值 | 說明 |
|---|---|
| SUBMIT | 提交 |
| APPROVE | 核准 |
| REJECT | 駁回 |
| RETURN | 退回 |
| DISPATCH | 派工 |
| MERGE | 合併 |
| COMPLETE | 完成 |
| CANCEL | 取消 |

### WorkflowType

| 值 | 說明 |
|---|---|
| FAULT_REVIEW | 通報審查流程 |
| REPAIR_DISPATCH | 報修派工流程 |
| REPAIR_CLOSE | 報修結案流程 |
| REPLACEMENT_REVIEW | 換裝審查流程 |
| ASSET_CHANGE | 資產異動流程 |

### TicketType

| 值 | 說明 |
|---|---|
| FAULT_TICKET | 通報單 |
| REPAIR_TICKET | 報修單 |
| REPLACEMENT_ORDER | 換裝派工單 |
| ASSET_CHANGE | 資產異動單 |
