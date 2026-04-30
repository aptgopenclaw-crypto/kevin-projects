# SA-02 簽核引擎 Function List

> **對應需求**：§3-(1) ~ §3-(2)  
> **SRS 對應**：SRS-03-001, SRS-03-002  
> **Spec 來源**：`/02-spec/03-approval-workflow.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-02-01 | 查看待辦案件 | 所有已認證 | 查看分派給自己（含代理）的待辦 |
| UC-02-02 | 簽核操作 | GOV_ADMIN, GOV_MGR, GOV_CHIEF, OPERATOR | 通過/退回/駁回/派工 |
| UC-02-03 | 查看流程歷程 | 所有已認證 | 檢視案件簽核步驟與時間軸 |
| UC-02-04 | 代理人設定 | GOV_MGR, GOV_CHIEF, DEPT_ADMIN | 指定代理人與期間 |
| UC-02-05 | 代理簽核 | 代理人 | 代理期間以被代理人身分簽核 |
| UC-02-06 | 流程定義查看 | SUPER_ADMIN | 查看/管理流程定義與步驟 |

---

## Function List

### 待辦案件 (§3-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-02-001 | 待辦案件列表 | R | 所有已認證 | 工單類型、分頁 | 待辦清單 | 含自己指派的 + 代理人委託的（日期有效）；標記代理來源 | SRS-03-001 | §3-1 | GET /v1/auth/workflow/pending |
| FN-02-002 | 待辦案件數量 | R | 所有已認證 | — | 各類型數量 | 同上 | SRS-03-001 | §3-1 | GET /v1/auth/workflow/pending/count |
| FN-02-003 | 流程歷程查詢 | R | 所有已認證 | 流程實例 ID | 步驟清單 (時間軸) | 含每步驟操作人、時間、動作、備註、附件、代理標記 | SRS-03-001 | §3-1 | GET /v1/auth/workflow/{instanceId}/logs |
| FN-02-004 | 流程狀態查詢 | R | 所有已認證 | 工單類型 + 工單 ID | 流程實例資訊 | — | SRS-03-001 | §3-1 | GET /v1/auth/workflow/instance |

### 簽核操作 (§3-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-02-005 | 送審 | P | GOV_ADMIN, OPERATOR, FIELD_USER | 流程實例 ID、目標步驟、備註、附件 | 更新結果 | 建立 WorkflowInstance → 狀態轉換 → 寫 step_log | SRS-03-001 | §3-1 | POST /v1/auth/workflow/{instanceId}/transition |
| FN-02-006 | 審核通過 | P | GOV_MGR, GOV_CHIEF | 流程實例 ID、目標步驟、備註 | 更新結果 | 送件人不可自審 (SELF_APPROVAL_NOT_ALLOWED)；驗證 assignee 或有效代理人 | SRS-03-001 | §3-1 | POST /v1/auth/workflow/{instanceId}/transition |
| FN-02-007 | 退回補件 | P | GOV_MGR, GOV_CHIEF | 流程實例 ID、目標步驟、退回原因 | 更新結果 | 退回至前一步驟；寫 step_log | SRS-03-001 | §3-1 | POST /v1/auth/workflow/{instanceId}/transition |
| FN-02-008 | 駁回 | P | GOV_MGR, GOV_CHIEF | 流程實例 ID、目標步驟、駁回原因 | 更新結果 | 案件終止（REJECTED）；寫 step_log | SRS-03-001 | §3-1 | POST /v1/auth/workflow/{instanceId}/transition |
| FN-02-009 | 取消流程 | P | 發起人 | 流程實例 ID | 更新結果 | 僅 ACTIVE 狀態可取消；僅發起人可操作 | SRS-03-001 | §3-1 | POST /v1/auth/workflow/{instanceId}/cancel |
| FN-02-010 | 派工（簽核中） | P | GOV_ADMIN, OPERATOR | 流程實例 ID、目標步驟、派工資訊 | 更新結果 | REPAIR_DISPATCH / REPLACEMENT_REVIEW 流程的派工步驟 | SRS-03-001 | §3-1 | POST /v1/auth/workflow/{instanceId}/transition |

### 工作流程 FSM (5 種流程)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-02-011 | FAULT_REVIEW 流程 | P | 系統/GOV_ADMIN | — | — | OPEN → REVIEW → CONFIRMED/REJECTED/MERGED；CONFIRMED 觸發 E1 | SRS-03-001 | §3-1 | (內部) |
| FN-02-012 | REPAIR_DISPATCH 流程 | P | GOV_ADMIN/FIELD_USER | — | — | PENDING → ACCEPTED → DISPATCHED → IN_PROGRESS → COMPLETION_REPORTED (+TRANSFERRED) | SRS-03-001 | §3-1 | (內部) |
| FN-02-013 | REPAIR_CLOSE 流程 | P | GOV_ADMIN/GOV_MGR | — | — | COMPLETION_REPORTED → PENDING_REVIEW → CLOSED/RETURNED；CLOSED 觸發 E9 | SRS-03-001 | §3-1 | (內部) |
| FN-02-014 | REPLACEMENT_REVIEW 流程 | P | GOV_ADMIN/FIELD_USER | — | — | DRAFT → DISPATCHED → IN_PROGRESS → SELF_CHECKED → PENDING_REVIEW → CLOSED/RETURNED；CLOSED 觸發 E10 | SRS-03-001 | §3-1 | (內部) |
| FN-02-015 | ASSET_CHANGE 流程 | P | GOV_ADMIN/DEPT_ADMIN | — | — | DRAFT → PENDING_REVIEW → APPROVED → APPLIED/RETURNED | SRS-03-001 | §3-1 | (內部) |

### 代理人管理 (§3-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-02-016 | 代理人列表查詢 | R | GOV_MGR, GOV_CHIEF, DEPT_ADMIN | 分頁 | 代理設定清單 | 僅顯示自己設定的代理 | SRS-03-002 | §3-2 | GET /v1/auth/delegates |
| FN-02-017 | 新增代理人 | C | GOV_MGR, GOV_CHIEF, DEPT_ADMIN | 代理人 ID、起始日、結束日、原因 | 代理設定 | end_date 必填；不可自我代理；同部門限制；日期不可重疊 | SRS-03-002 | §3-2 | POST /v1/auth/delegates |
| FN-02-018 | 停用代理 | U | GOV_MGR, GOV_CHIEF, DEPT_ADMIN | 代理設定 ID | 更新結果 | 提前結束代理 | SRS-03-002 | §3-2 | PUT /v1/auth/delegates/{id}/deactivate |
| FN-02-019 | 代理到期自動失效 | P | 系統 | — | — | end_date < 今天 → 查詢自動排除；歷史紀錄保留 | SRS-03-002 | §3-2 | (系統排程) |
| FN-02-020 | 代理簽核執行 | P | 代理人 | 流程實例 ID、動作、備註 | 更新結果 | 驗證有效代理；step_log 記錄 is_delegated=true + originalAssigneeId | SRS-03-002 | §3-2 | (同 FN-02-005~008) |

### 流程定義管理

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-02-021 | 流程定義列表 | R | SUPER_ADMIN | — | 流程定義清單 | 5 種內建流程 | SRS-03-001 | §3-1 | GET /v1/auth/admin/workflow-definitions |
| FN-02-022 | 流程步驟模板查詢 | R | SUPER_ADMIN | workflowType | 步驟模板清單 | — | SRS-03-001 | §3-1 | GET /v1/auth/admin/workflow-definitions/{type}/steps |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 待辦案件 | /admin/workflow/pending | 跨工單類型待辦列表 | FN-02-001~002 |
| 代理人管理 | /admin/workflow/delegates | 代理設定 CRUD | FN-02-016~018 |
| 流程歷程 (共用元件) | — | WorkflowStepper + WorkflowActionBar | FN-02-003~010 |
