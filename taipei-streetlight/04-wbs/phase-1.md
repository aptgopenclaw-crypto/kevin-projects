# WBS 1.1 — Phase 1：地基層（資產 + 簽核 + 障礙偵測）

> **狀態**：✅ 已完成  
> **Flyway**：V30–V34  
> **SRS 對應**：SRS-03-001, SRS-03-002, SRS-04-007~014, SRS-04-006  
> **x-plan**：`/_archive/x-plan/phase-1-foundation.md`、`/_archive/x-plan/phase-1-todo.md`

---

## 1.1.1 資料庫 Schema

| WBS ID | 工作包 | Flyway | 產出 | 狀態 |
|--------|-------|--------|------|------|
| 1.1.1.1 | 設備主表 + 回路 + 契約 DDL | V30 | `devices`, `circuits`, `device_events`, `contracts` + indexes + FK | ✅ |
| 1.1.1.2 | 簽核引擎全套 DDL | V31 | `workflow_definitions`, `workflow_steps_template`, `workflow_instances`, `workflow_step_logs`, `delegate_settings` | ✅ |
| 1.1.1.3 | 障礙工單 + 關聯偵測 DDL | V32 | `fault_tickets`, `fault_correlations` + menu + permission + role binding | ✅ |
| 1.1.1.4 | 設備欄位補充 | V33 | `ALTER devices ADD mount_position` | ✅ |
| 1.1.1.5 | Seed 資料（開發/測試環境） | V34 | 360 devices + 2 contracts + 12 circuits | ✅ |

---

## 1.1.2 後端 — 設備模組 (`com.taipei.iot.device`)

### 1.1.2.1 Entity 層

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.1.2.1.1 | Device Entity | `Device.java` (`@Filter`, `TenantAware`, JSONB attributes) | ✅ |
| 1.1.2.1.2 | DeviceType Enum | `DeviceType.java` (POLE/LUMINAIRE/PANEL_BOX/CONTROLLER/POWER_EQUIPMENT/ATTACHMENT) | ✅ |
| 1.1.2.1.3 | DeviceStatus Enum | `DeviceStatus.java` (ACTIVE/REPORTED/UNDER_REPAIR/INACTIVE/DECOMMISSIONED) | ✅ |
| 1.1.2.1.4 | ConnectivityType Enum | `ConnectivityType.java` (NONE/DIRECT/GATEWAY) | ✅ |
| 1.1.2.1.5 | Circuit Entity | `Circuit.java` | ✅ |
| 1.1.2.1.6 | DeviceEvent Entity | `DeviceEvent.java` + `DeviceEventType.java` | ✅ |
| 1.1.2.1.7 | Contract Entity | `Contract.java` + `ContractStatus.java` | ✅ |
| 1.1.2.1.8 | DeviceManager Entity | `DeviceManager.java` | ✅ |

### 1.1.2.2 Repository 層

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.1.2.2.1 | DeviceRepository | 分頁 + 篩選 + DataScope | ✅ |
| 1.1.2.2.2 | CircuitRepository | TenantScopedRepository | ✅ |
| 1.1.2.2.3 | DeviceEventRepository | TenantScopedRepository | ✅ |
| 1.1.2.2.4 | ContractRepository | TenantScopedRepository | ✅ |
| 1.1.2.2.5 | DeviceManagerRepository | TenantScopedRepository | ✅ |

### 1.1.2.3 DTO 層

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.1.2.3.1 | DeviceRequest / DeviceResponse | `@Valid` 驗證 + deptName + childrenCount | ✅ |
| 1.1.2.3.2 | DeviceStatsResponse | totalDevices, byType, byStatus, onlineRate | ✅ |
| 1.1.2.3.3 | CircuitRequest / CircuitResponse | | ✅ |
| 1.1.2.3.4 | ContractRequest / ContractResponse | | ✅ |
| 1.1.2.3.5 | ComponentReplaceRequest | 組合元件替換 | ✅ |

### 1.1.2.4 Service 層

| WBS ID | 工作包 | 檔案 | 說明 | 狀態 |
|--------|-------|------|------|------|
| 1.1.2.4.1 | DeviceService | `DeviceService.java` | CRUD + DataScope + 拓撲循環防護 + JSONB 大小限制 | ✅ |
| 1.1.2.4.2 | CircuitService | `CircuitService.java` | CRUD + 刪除限制 (CIRCUIT_HAS_DEVICES) | ✅ |
| 1.1.2.4.3 | DeviceEventService | `DeviceEventService.java` | 歷程寫入（供 05/06 結案呼叫） | ✅ |
| 1.1.2.4.4 | ContractService | `ContractService.java` | CRUD + 保固到期提醒 | ✅ |
| 1.1.2.4.5 | DeviceExportService | `DeviceExportService.java` | ODS/XLS/CSV 匯出 + JSONB 欄位展開 | ✅ |

### 1.1.2.5 Controller 層

| WBS ID | 工作包 | 檔案 | 端點數 | 狀態 |
|--------|-------|------|--------|------|
| 1.1.2.5.1 | DeviceController | `DeviceController.java` | 8 | ✅ |
| 1.1.2.5.2 | CircuitController | `CircuitController.java` | 5 | ✅ |
| 1.1.2.5.3 | ContractController | `ContractController.java` | CRUD + 列表 | ✅ |

---

## 1.1.3 後端 — 簽核引擎 (`com.taipei.iot.workflow`)

### 1.1.3.1 Entity 層

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.1.3.1.1 | WorkflowDefinition | 全域表（不需 TenantAware） | ✅ |
| 1.1.3.1.2 | WorkflowStepsTemplate | 全域表 | ✅ |
| 1.1.3.1.3 | WorkflowInstance | `TenantAware`, 多態 FK (ticket_type + ticket_id) | ✅ |
| 1.1.3.1.4 | WorkflowStepLog | `TenantAware`, JSONB attachments + delegate 欄位 | ✅ |
| 1.1.3.1.5 | DelegateSetting | `TenantAware`, end_date 必填 | ✅ |
| 1.1.3.1.6 | 列舉型別 | WorkflowType / WorkflowStatus / WorkflowAction / TicketType | ✅ |

### 1.1.3.2 Repository 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.1.3.2.1 | WorkflowDefinitionRepository (全域) | ✅ |
| 1.1.3.2.2 | WorkflowStepsTemplateRepository (全域) | ✅ |
| 1.1.3.2.3 | WorkflowInstanceRepository | ✅ |
| 1.1.3.2.4 | WorkflowStepLogRepository | ✅ |
| 1.1.3.2.5 | DelegateSettingRepository | ✅ |

### 1.1.3.3 Service 層

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.1.3.3.1 | WorkflowService (interface) | 統一簽核介面 | ✅ |
| 1.1.3.3.2 | WorkflowServiceImpl | **核心 FSM**：5 種流程狀態轉換 + 自審防護 + 代理簽核 | ✅ |
| 1.1.3.3.3 | DelegateService | CRUD + 期間驗證 + 重疊檢查 + 同部門限制 | ✅ |
| 1.1.3.3.4 | WorkflowTransitionEvent | Spring ApplicationEvent | ✅ |

### 1.1.3.4 Controller 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.1.3.4.1 | WorkflowController | 待辦列表 + 流程歷程 + 狀態轉換 | ✅ |
| 1.1.3.4.2 | DelegateController | 代理人 CRUD | ✅ |

---

## 1.1.4 後端 — 障礙偵測 (`com.taipei.iot.fault`)

### 1.1.4.1 Entity 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.1.4.1.1 | FaultTicket | `TenantAware`, FK → devices, circuits | ✅ |
| 1.1.4.1.2 | FaultCorrelation | `TenantAware` | ✅ |
| 1.1.4.1.3 | 列舉型別 | FaultTicketStatus / FaultTicketSource / RootCauseType | ✅ |

### 1.1.4.2 Service 層

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.1.4.2.1 | FaultTicketService | CRUD + 新增時觸發關聯偵測 | ✅ |
| 1.1.4.2.2 | FaultCorrelationService | 三維度偵測（回路/Gateway/地理） + 排程掃描 | ✅ |

### 1.1.4.3 Controller 層

| WBS ID | 工作包 | 端點數 | 狀態 |
|--------|-------|--------|------|
| 1.1.4.3.1 | FaultTicketController | 7 | ✅ |

---

## 1.1.5 前端 — 資產管理

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.1.5.1 | TypeScript 型別 | `types/device.ts`, `types/circuit.ts`, `types/fault.ts` | ✅ |
| 1.1.5.2 | API 模組 | `api/device/`, `api/circuit/`, `api/fault/`, `api/contract/` | ✅ |
| 1.1.5.3 | Store | `stores/deviceStore.ts`, `stores/circuitStore.ts` | ✅ |
| 1.1.5.4 | 設備管理頁面 | `DeviceManagementView.vue` (expandable rows) | ✅ |
| 1.1.5.5 | 回路管理頁面 | `CircuitManagementView.vue` | ✅ |
| 1.1.5.6 | 契約管理頁面 | `ContractManagementView.vue` | ✅ |
| 1.1.5.7 | 拓撲檢視頁面 | `DeviceTopologyView.vue` (樹狀 + 在線狀態) | ✅ |
| 1.1.5.8 | 障礙工單頁面 | `FaultTicketView.vue` | ✅ |
| 1.1.5.9 | 關聯障礙頁面 | `FaultCorrelationView.vue` | ✅ |
| 1.1.5.10 | 共用元件 | `DeptTreeSelector.vue` | ✅ |

### 前端 — 簽核引擎

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.1.5.11 | 待辦案件頁面 | `PendingTasksView.vue` | ✅ |
| 1.1.5.12 | 代理人管理頁面 | `DelegateSettingsView.vue` | ✅ |
| 1.1.5.13 | 流程步驟元件 | `WorkflowStepper.vue` (共用) | ✅ |
| 1.1.5.14 | 操作按鈕元件 | `WorkflowActionBar.vue` (共用) | ✅ |
| 1.1.5.15 | 型別 + API | `types/workflow.ts`, `api/workflow/` | ✅ |
| 1.1.5.16 | 國際化 | device/circuit/fault/contract/workflow (zh-TW/en/zh-CN) | ✅ |

---

## 1.1.6 單元測試

| WBS ID | 工作包 | 測試類 | Tests | 狀態 |
|--------|-------|--------|-------|------|
| 1.1.6.1 | 設備 Controller | DeviceControllerTest | 12 | ✅ |
| 1.1.6.2 | 設備 Service | DeviceServiceTest | 14 | ✅ |
| 1.1.6.3 | 回路 Controller | CircuitControllerTest | 8 | ✅ |
| 1.1.6.4 | 回路 Service | CircuitServiceTest | 7 | ✅ |
| 1.1.6.5 | 契約 Service | ContractServiceTest | 6 | ✅ |
| 1.1.6.6 | 契約 Controller | ContractControllerTest | 7 | ✅ |
| 1.1.6.7 | 匯出 Service | DeviceExportServiceTest | 6 | ✅ |
| 1.1.6.8 | 簽核 Service | WorkflowServiceTest | 13 | ✅ |
| 1.1.6.9 | 簽核 Controller | WorkflowControllerTest | 6 | ✅ |
| 1.1.6.10 | 代理 Service | DelegateServiceTest | 8 | ✅ |
| 1.1.6.11 | 代理 Controller | DelegateControllerTest | 6 | ✅ |
| 1.1.6.12 | 障礙工單 Controller | FaultTicketControllerTest | 7 | ✅ |
| 1.1.6.13 | 障礙工單 Service | FaultTicketServiceTest | 7 | ✅ |
| 1.1.6.14 | 關聯偵測 Service | FaultCorrelationServiceTest | 5 | ✅ |

---

## 1.1.7 共用基礎設施

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.1.7.1 | AuditCategory 擴充 | ASSET, WORKFLOW | ✅ |
| 1.1.7.2 | AuditEventType 擴充 | CREATE/UPDATE/DELETE/EXPORT_DEVICE, WORKFLOW_* | ✅ |
| 1.1.7.3 | ErrorCode 擴充 | 60xxx (設備), 90xxx (簽核) | ✅ |
| 1.1.7.4 | Menu + Permission Seed | menu 35~39, 9 permissions, role binding | ✅ |
| 1.1.7.5 | Router 整合 | `/admin/asset/*` 路由 | ✅ |
