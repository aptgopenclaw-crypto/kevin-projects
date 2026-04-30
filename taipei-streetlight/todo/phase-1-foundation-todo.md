# Phase 1 — 地基層（資產 + 簽核 + 障礙偵測）TODO

> **建立日期**: 2026-04-27  
> **最後更新**: 2026-04-27 (全部完成)  
> **甘特圖**: 03/09 – 04/04 (4 週)  
> **前置**: Phase 0 基礎建設（認證/RBAC/多租戶/稽核/部門）  
> **執行計畫**: 99-plan/2026-04-24-gantt.md §Phase 1  
> **關鍵路徑**: Phase 1 → Phase 2 → Phase 4 → Phase 5  
> **里程碑**: m0b — 2026-04-04  
> **WBS**: 04-wbs/phase-1.md  
> **SRS 對應**: SRS-03-001, SRS-03-002, SRS-04-006~014

---

### 進度總覽

| 區塊 | Gantt ID | 工期 | 進度 | 說明 |
|------|----------|------|------|------|
| 1a DB Schema (V30–V34) | 1a | 3d | ✅ | 13 tables + seed data |
| 1b 設備模組 (Entity/Repo/Service) | 1b | 8d | ✅ | Device/Circuit/Contract CRUD + 匯出 |
| 1c 簽核引擎 FSM (5 狀態) | 1c | 10d | ✅ | WorkflowService FSM + 代理簽核 |
| 1d 障礙偵測 + 關聯分析 | 1d | 5d | ✅ | FaultTicket + 三維度關聯偵測 |
| 1e 前端 16 頁面 (資產/簽核/障礙) | 1e | 15d | ✅ | 16 Vue pages + 共用元件 |
| 1f 單元測試 (14 類, 112 tests) | 1f | 5d | ✅ | 14 test classes |

### 甘特依賴

```
1a DB Schema (3d) ──→ 1b 設備模組 (8d) ──→ 1d 障礙偵測 (5d) ──→ 1f 測試 (5d)
       │                                                              ↑
       ├──→ 1c 簽核引擎 (10d) ────────────────────────────────────────┘
       │
       └──→ 1e 前端 16 頁面 (15d, 與後端並行) ────────────────────────┘
```

---

## 1a — DB Schema V30–V34 (3 天) ✅ 已完成

> **SD**: SD-03-asset.md, SD-02-approval.md  
> **SRS**: SRS-04-007~014, SRS-03-001~002

### Flyway Migrations

- [x] **V30** — 設備主表 DDL: `devices`, `circuits`, `device_events`, `contracts` + indexes + FK
- [x] **V31** — 簽核引擎 DDL: `workflow_definitions`, `workflow_steps_template`, `workflow_instances`, `workflow_step_logs`, `delegate_settings`
- [x] **V32** — 障礙工單 DDL: `fault_tickets`, `fault_correlations` + menu + permission + role binding
- [x] **V33** — 設備欄位補充: `ALTER devices ADD mount_position`
- [x] **V34** — Seed 資料: 360 devices + 2 contracts + 12 circuits

### 表格統計

| 表 | 模組 | 說明 |
|----|------|------|
| devices | 設備 | 主表，JSONB attributes，TenantAware |
| circuits | 設備 | 回路 |
| device_events | 設備 | 設備歷程 |
| contracts | 設備 | 維護契約 |
| workflow_definitions | 簽核 | 流程定義（全域） |
| workflow_steps_template | 簽核 | 步驟模板（全域） |
| workflow_instances | 簽核 | 流程實例 |
| workflow_step_logs | 簽核 | 步驟紀錄 |
| delegate_settings | 簽核 | 代理人設定 |
| fault_tickets | 障礙 | 障礙工單 |
| fault_correlations | 障礙 | 關聯偵測結果 |

---

## 1b — 設備模組 (8 天) ✅ 已完成

> **Package**: `com.taipei.iot.device`  
> **SD**: SD-03-asset.md

### Entity

- [x] Device (JSONB attributes, TenantAware, @Filter)
- [x] DeviceType Enum (POLE/LUMINAIRE/PANEL_BOX/CONTROLLER/POWER_EQUIPMENT/ATTACHMENT)
- [x] DeviceStatus Enum (ACTIVE/REPORTED/UNDER_REPAIR/INACTIVE/DECOMMISSIONED)
- [x] ConnectivityType Enum (NONE/DIRECT/GATEWAY)
- [x] Circuit, DeviceEvent, Contract, DeviceManager

### Repository

- [x] DeviceRepository — 分頁 + 篩選 + DataScope
- [x] CircuitRepository, DeviceEventRepository, ContractRepository, DeviceManagerRepository

### Service

- [x] DeviceService — CRUD + DataScope + 拓撲循環防護 + JSONB 大小限制
- [x] CircuitService — CRUD + 刪除限制 (CIRCUIT_HAS_DEVICES)
- [x] DeviceEventService — 歷程寫入（供 Phase 2/4 結案呼叫）
- [x] ContractService — CRUD + 保固到期提醒
- [x] DeviceExportService — ODS/XLS/CSV 匯出 + JSONB 欄位展開

### Controller

- [x] DeviceController — 8 端點
- [x] CircuitController — 5 端點
- [x] ContractController — CRUD + 列表

---

## 1c — 簽核引擎 FSM (10 天) ✅ 已完成

> **Package**: `com.taipei.iot.workflow`  
> **SD**: SD-02-approval.md

### Entity

- [x] WorkflowDefinition — 全域表（不需 TenantAware）
- [x] WorkflowStepsTemplate — 全域表
- [x] WorkflowInstance — TenantAware, 多態 FK (ticket_type + ticket_id)
- [x] WorkflowStepLog — TenantAware, JSONB attachments + delegate 欄位
- [x] DelegateSetting — TenantAware, end_date 必填
- [x] 列舉型別: WorkflowType / WorkflowStatus / WorkflowAction / TicketType

### Service

- [x] WorkflowService (interface) — 統一簽核介面
- [x] WorkflowServiceImpl — **核心 FSM**: 5 種流程狀態轉換 + 自審防護 + 代理簽核
- [x] DelegateService — CRUD + 期間驗證 + 重疊檢查 + 同部門限制
- [x] WorkflowTransitionEvent — Spring ApplicationEvent

### Controller

- [x] WorkflowController — 待辦列表 + 流程歷程 + 狀態轉換
- [x] DelegateController — 代理人 CRUD

---

## 1d — 障礙偵測 + 關聯分析 (5 天) ✅ 已完成

> **Package**: `com.taipei.iot.fault`  
> **SD**: SD-04-repair.md §障礙

### Entity

- [x] FaultTicket — TenantAware, FK → devices, circuits
- [x] FaultCorrelation — TenantAware
- [x] 列舉: FaultTicketStatus / FaultTicketSource / RootCauseType

### Service

- [x] FaultTicketService — CRUD + 新增時觸發關聯偵測
- [x] FaultCorrelationService — 三維度偵測（回路/Gateway/地理）+ 排程掃描

### Controller

- [x] FaultTicketController — 7 端點

---

## 1e — 前端 16 頁面 (15 天) ✅ 已完成

### 資產管理前端

- [x] TypeScript 型別: `types/device.ts`, `types/circuit.ts`, `types/fault.ts`
- [x] API 模組: `api/device/`, `api/circuit/`, `api/fault/`, `api/contract/`
- [x] Store: `stores/deviceStore.ts`, `stores/circuitStore.ts`
- [x] DeviceManagementView.vue — expandable rows
- [x] CircuitManagementView.vue
- [x] ContractManagementView.vue
- [x] DeviceTopologyView.vue — 樹狀 + 在線狀態
- [x] FaultTicketView.vue
- [x] FaultCorrelationView.vue
- [x] DeptTreeSelector.vue — 共用元件

### 簽核引擎前端

- [x] PendingTasksView.vue
- [x] DelegateSettingsView.vue
- [x] WorkflowStepper.vue — 共用
- [x] WorkflowActionBar.vue — 共用
- [x] `types/workflow.ts`, `api/workflow/`
- [x] 國際化: device/circuit/fault/contract/workflow (zh-TW/en/zh-CN)

---

## 1f — 單元測試 (5 天) ✅ 已完成

| # | 測試類 | Tests | 狀態 |
|---|--------|-------|------|
| 1 | DeviceControllerTest | 12 | ✅ |
| 2 | DeviceServiceTest | 14 | ✅ |
| 3 | CircuitControllerTest | 8 | ✅ |
| 4 | CircuitServiceTest | 7 | ✅ |
| 5 | ContractServiceTest | 6 | ✅ |
| 6 | ContractControllerTest | 7 | ✅ |
| 7 | DeviceExportServiceTest | 6 | ✅ |
| 8 | WorkflowServiceTest | 13 | ✅ |
| 9 | WorkflowControllerTest | 6 | ✅ |
| 10 | DelegateServiceTest | 8 | ✅ |
| 11 | DelegateControllerTest | 6 | ✅ |
| 12 | FaultTicketControllerTest | 7 | ✅ |
| 13 | FaultTicketServiceTest | 7 | ✅ |
| 14 | FaultCorrelationServiceTest | 5 | ✅ |
| | **合計** | **112** | |

---

## 共用基礎設施 ✅ 已完成

- [x] AuditCategory 擴充: ASSET, WORKFLOW
- [x] AuditEventType 擴充: CREATE/UPDATE/DELETE/EXPORT_DEVICE, WORKFLOW_*
- [x] ErrorCode 擴充: 60xxx (設備), 90xxx (簽核)
- [x] Menu + Permission Seed: menu 35~39, 9 permissions, role binding
- [x] Router 整合: `/admin/asset/*` 路由
