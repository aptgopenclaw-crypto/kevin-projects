# Phase 1 完成度追蹤

## ✅ 已完成

### DB Migrations
- [x] V30 devices/circuits/device_events/contracts 表建立
- [x] V31 workflow 5 張表 + seed 流程定義
- [x] V32 fault_tickets/fault_correlations + menu + permission
- [x] V33 ALTER devices ADD mount_position
- [x] V34 seed data (360 devices + 2 contracts + 12 circuits)

### 後端 — device/
- [x] Entity: Device, Circuit, DeviceEvent, Contract, DeviceManager
- [x] Enum: DeviceType, DeviceStatus, ConnectivityType, DeviceEventType, ContractStatus
- [x] Repository: DeviceRepository, CircuitRepository, DeviceEventRepository, ContractRepository, DeviceManagerRepository
- [x] Service: DeviceService, CircuitService, DeviceEventService, ContractService
- [x] Controller: DeviceController, CircuitController, ContractController
- [x] DTO: DeviceRequest/Response, CircuitRequest/Response, ContractRequest/Response, ComponentReplaceRequest, DeviceStatsResponse

### 後端 — workflow/
- [x] Entity: WorkflowDefinition, WorkflowStepsTemplate, WorkflowInstance, WorkflowStepLog, DelegateSetting
- [x] Enum: WorkflowType, WorkflowStatus, WorkflowAction, TicketType
- [x] Repository: 全 5 個
- [x] Service: WorkflowService (interface), WorkflowServiceImpl (FSM), DelegateService
- [x] Controller: WorkflowController, DelegateController
- [x] DTO: WorkflowInstanceResponse, WorkflowStepLogResponse, WorkflowTransitionRequest, DelegateSettingRequest/Response
- [x] Event: WorkflowTransitionEvent

### 後端 — fault/
- [x] Entity: FaultTicket, FaultCorrelation
- [x] Enum: FaultTicketStatus, FaultTicketSource, RootCauseType
- [x] Repository: FaultTicketRepository, FaultCorrelationRepository
- [x] Service: FaultTicketService, FaultCorrelationService
- [x] Controller: FaultTicketController
- [x] DTO: FaultTicketRequest/Response, FaultCorrelationResponse

### 前端 — 設備 + 回路
- [x] types/device.ts, types/circuit.ts
- [x] api/device/index.ts, api/circuit/index.ts
- [x] stores/deviceStore.ts, stores/circuitStore.ts
- [x] views/admin/asset/DeviceManagementView.vue (含 expandable rows)
- [x] views/admin/asset/CircuitManagementView.vue
- [x] components/DeptTreeSelector.vue
- [x] i18n: device.*, circuit.* (zh-TW/zh-CN/en)

---

## ✅ 已完成

### 後端缺漏
- [x] DeviceExportService (ODS/XLS/CSV 匯出，展開 JSONB attributes)
- [x] DeviceControllerTest (12 tests — 10 端點 + 401/403 權限)
- [x] DeviceServiceTest (14 tests — CRUD + 循環防護 + DataScope + JSONB 驗證 + 組合元件)
- [x] CircuitControllerTest (8 tests — 5 端點 + 權限 + CIRCUIT_HAS_DEVICES)
- [x] CircuitServiceTest (7 tests — CRUD + CIRCUIT_HAS_DEVICES 驗證)
- [x] ContractServiceTest (6 tests — CRUD + 預設狀態 + createdBy)
- [x] ContractControllerTest (7 tests — 5 端點 + CONTRACT_VIEW/MANAGE)
- [x] DeviceExportServiceTest (6 tests — CSV/XLSX/ODS + BOM + JSONB 欄位展開 + 逸出)
- [x] WorkflowServiceTest (13 tests — 5 種流程轉換 + 自審防護 + 代理簽核 + 取消)
- [x] WorkflowControllerTest (6 tests — pending/logs/transition/cancel + WORKFLOW_VIEW)
- [x] DelegateServiceTest (8 tests — CRUD + 重疊驗證 + 自我代理 + end_date + 停用)
- [x] DelegateControllerTest (6 tests — list/create/deactivate + DELEGATE_MANAGE)
- [x] FaultTicketControllerTest (7 tests — 4 端點 + FAULT_VIEW/MANAGE)
- [x] FaultTicketServiceTest (7 tests — CRUD + 關聯偵測觸發 + resolve)
- [x] FaultCorrelationServiceTest (5 tests — 迴路閾值 + null-check skip + Gateway)

### 前端缺漏 — 資產管理頁面
- [x] views/admin/asset/ContractManagementView.vue (契約管理)
- [x] views/admin/asset/DeviceTopologyView.vue (拓撲檢視)
- [x] views/admin/asset/FaultTicketView.vue (障礙工單)
- [x] views/admin/asset/FaultCorrelationView.vue (關聯障礙)
- [x] types/fault.ts
- [x] api/fault/index.ts
- [x] api/contract/index.ts (前端 API layer)
- [x] i18n: fault.*, contract.* (zh-TW/zh-CN/en)

### 前端缺漏 — 簽核引擎頁面
- [x] views/admin/workflow/PendingTasksView.vue (待辦案件)
- [x] views/admin/workflow/DelegateSettingsView.vue (代理人管理)
- [x] components/WorkflowStepper.vue (共用流程步驟元件)
- [x] components/WorkflowActionBar.vue (共用操作按鈕元件)
- [x] types/workflow.ts
- [x] api/workflow/index.ts
- [x] i18n: workflow.* (zh-TW/zh-CN/en)
