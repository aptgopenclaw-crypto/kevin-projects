# Phase 2 — 報修派工（報修維護 + 巡查管理）TODO

> **建立日期**: 2026-04-27  
> **最後更新**: 2026-04-27 (全部完成)  
> **甘特圖**: 04/06 – 04/25 (3 週)  
> **前置**: Phase 1 地基層（設備表 + 簽核引擎 + 障礙偵測）  
> **執行計畫**: 99-plan/2026-04-24-gantt.md §Phase 2  
> **關鍵路徑**: Phase 1 → **Phase 2** → Phase 4 → Phase 5  
> **里程碑**: m0c — 2026-04-25 (Phase 2+3 完成)  
> **WBS**: 04-wbs/phase-2.md  
> **SRS 對應**: SRS-05-001~017

---

### 進度總覽

| 區塊 | Gantt ID | 工期 | 進度 | 說明 |
|------|----------|------|------|------|
| 2a DB Schema (V36–V39) | 2a | 2d | ✅ | 5 tables + menu/permission |
| 2b 報修模組 (9 Entity + 3 Listener) | 2b | 10d | ✅ | RepairTicket CRUD + 派工 + 完工 + E1/E4/E9 |
| 2c 巡查模組 (E13 異常→障礙) | 2c | 3d | ✅ | InspectionTask + Record + E13 |
| 2d 前端 15 頁面 (報修/巡查) | 2d | 10d | ✅ | 15 Vue pages + 共用附件元件 |
| 2e 單元測試 (8 類) | 2e | 3d | ✅ | 8 test classes |

### 甘特依賴

```
2a DB Schema (2d) ──→ 2b 報修模組 (10d) ──→ 2c 巡查模組 (3d) ──→ 2e 測試 (3d)
       │
       └──→ 2d 前端 15 頁面 (10d, 與後端並行) ───────────────────────┘
```

---

## 2a — DB Schema V36–V39 (2 天) ✅ 已完成

> **SD**: SD-04-repair.md

### Flyway Migrations

- [x] **V36** — 巡查 DDL: `inspection_tasks`, `inspection_records` + menu + permission + role binding
- [x] **V37** — 報修 DDL: `repair_tickets`, `repair_dispatches`, `ticket_attachments`
- [x] **V38** — 選單結構修正: parent_id/route_name/component/menu_type
- [x] **V39** — 選單權限修正: REPAIR_VIEW / INSPECTION_VIEW

### 表格統計

| 表 | 模組 | 說明 |
|----|------|------|
| repair_tickets | 報修 | 報修工單主表 |
| repair_dispatches | 報修 | 派工紀錄 |
| ticket_attachments | 報修 | 多態附件 (ticket_type + ticket_id) |
| inspection_tasks | 巡查 | 巡查任務 |
| inspection_records | 巡查 | 巡查紀錄 |

---

## 2b — 報修模組 (10 天) ✅ 已完成

> **Package**: `com.taipei.iot.repair`  
> **SD**: SD-04-repair.md

### Entity

- [x] RepairTicket — TenantAware, FK → FaultTicket/Device/Circuit/Contract
- [x] RepairTicketStatus Enum — 10 狀態 FSM
- [x] RepairTicketSource Enum — FAULT_TICKET/CITIZEN_WEB/EXTERNAL_1999/PATROL/PHONE
- [x] RepairTicketPriority Enum — LOW/NORMAL/HIGH/URGENT
- [x] RepairDispatch — TenantAware, FK → RepairTicket/Contract
- [x] RepairDispatchStatus Enum — DISPATCHED/IN_PROGRESS/COMPLETED/CANCELLED
- [x] TicketAttachment — TenantAware, 多態 (ticket_type + ticket_id)
- [x] AttachmentPhase Enum — BEFORE/DURING/AFTER/REPORT
- [x] ScanStatus Enum — PENDING/CLEAN/INFECTED

### Repository

- [x] RepairTicketRepository — 分頁 + 多條件 + DataScope
- [x] RepairDispatchRepository
- [x] TicketAttachmentRepository

### Service

- [x] RepairTicketService — 雙路徑建單 + 收案 + 完工回報 + 改分轉送 + DataScope
- [x] RepairDispatchService — 派工 + 退回重派
- [x] TicketAttachmentService — 上傳 + 病毒掃描狀態 + 查詢
- [x] LocalFileStorageService — 本地存儲 + 路徑遍歷防護

### 事件監聽器

- [x] **E1** FaultApprovedListener — 障礙審核通過 → 自動建 repair_ticket
- [x] **E4** RepairDispatchedListener — 派工 → 設備狀態改 UNDER_REPAIR
- [x] **E9** RepairClosedListener — 結案審核通過 → 設備復原 ACTIVE + 寫歷程

### Controller

- [x] RepairTicketController — 9 端點
- [x] TicketAttachmentController — 3 端點

---

## 2c — 巡查模組 (3 天) ✅ 已完成

> **Package**: `com.taipei.iot.repair` (同模組)  
> **SD**: SD-04-repair.md §巡查

### Entity

- [x] InspectionTask — TenantAware
- [x] InspectionRecord — TenantAware
- [x] 列舉: InspectionType / InspectionStatus / InspectionFrequency / InspectionResult

### Service

- [x] InspectionService — CRUD + **E13**（異常 → 自動建 fault_ticket）

### Controller

- [x] InspectionController — 8 端點

---

## 2d — 前端 15 頁面 (10 天) ✅ 已完成

### 報修前端

- [x] TypeScript 型別: `types/repair.ts` (10 enum + 12 interface)
- [x] API 模組: `api/repair/index.ts` (11 函式), `api/inspection/index.ts` (8 函式)
- [x] Store: `stores/repairStore.ts`
- [x] RepairTicketView.vue — 篩選+分頁+新增
- [x] RepairTicketDetailView.vue — 4 Tabs + Action
- [x] RepairDispatchDialog.vue
- [x] CompletionReportDialog.vue

### 巡查前端

- [x] InspectionView.vue
- [x] InspectionRecordView.vue

### 共用附件元件

- [x] AttachmentUploader.vue — 多檔+GPS+拖放
- [x] AttachmentGallery.vue — phase 分組+預覽

### 路由 + i18n

- [x] menuStore.ts — implicitChildren 路由
- [x] 4 條路由 (tickets, :id, inspection, :taskId/records)
- [x] 國際化: repair + inspection (zh-TW/en/zh-CN)

---

## 2e — 單元測試 (3 天) ✅ 已完成

| # | 測試類 | 狀態 |
|---|--------|------|
| 1 | RepairTicketControllerTest | ✅ |
| 2 | RepairTicketServiceTest | ✅ |
| 3 | RepairDispatchServiceTest | ✅ |
| 4 | TicketAttachmentServiceTest | ✅ |
| 5 | TicketAttachmentControllerTest | ✅ |
| 6 | InspectionControllerTest | ✅ |
| 7 | InspectionServiceTest | ✅ |
| 8 | LocalFileStorageServiceTest | ✅ |

---

## 共用基礎設施 ✅ 已完成

- [x] AuditCategory 擴充: MAINTENANCE
- [x] AuditEventType 擴充: CREATE/UPDATE/DISPATCH/COMPLETE/CLOSE_REPAIR
- [x] ErrorCode 擴充: 70xxx 系列 (7 個)
- [x] WorkflowServiceImpl 擴充: REPAIR_DISPATCH + REPAIR_CLOSE + INITIAL_STEPS
