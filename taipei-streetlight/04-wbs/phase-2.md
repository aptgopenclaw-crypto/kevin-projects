# WBS 1.2 — Phase 2：報修派工（報修維護 + 巡查管理）

> **狀態**：✅ 已完成  
> **Flyway**：V36–V39  
> **SRS 對應**：SRS-05-001~017  
> **x-plan**：`/_archive/x-plan/phase-2-repair.md`、`/_archive/x-plan/phase-2-todo.md`

---

## 1.2.1 資料庫 Schema

| WBS ID | 工作包 | Flyway | 產出 | 狀態 |
|--------|-------|--------|------|------|
| 1.2.1.1 | 巡查任務 + 巡查紀錄 DDL | V36 | `inspection_tasks`, `inspection_records` + menu + permission + role binding | ✅ |
| 1.2.1.2 | 報修工單 + 派工 + 附件 DDL | V37 | `repair_tickets`, `repair_dispatches`, `ticket_attachments` | ✅ |
| 1.2.1.3 | 選單結構修正 | V38 | 修正 parent_id/route_name/component/menu_type | ✅ |
| 1.2.1.4 | 選單權限修正 | V39 | 設定 REPAIR_VIEW / INSPECTION_VIEW | ✅ |

---

## 1.2.2 後端 — 報修模組 (`com.taipei.iot.repair`)

### 1.2.2.1 Entity 層

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.2.2.1.1 | RepairTicket | `TenantAware`, FK → FaultTicket/Device/Circuit/Contract | ✅ |
| 1.2.2.1.2 | RepairTicketStatus Enum | 10 狀態 FSM | ✅ |
| 1.2.2.1.3 | RepairTicketSource Enum | FAULT_TICKET/CITIZEN_WEB/EXTERNAL_1999/PATROL/PHONE | ✅ |
| 1.2.2.1.4 | RepairTicketPriority Enum | LOW/NORMAL/HIGH/URGENT | ✅ |
| 1.2.2.1.5 | RepairDispatch | `TenantAware`, FK → RepairTicket/Contract | ✅ |
| 1.2.2.1.6 | RepairDispatchStatus Enum | DISPATCHED/IN_PROGRESS/COMPLETED/CANCELLED | ✅ |
| 1.2.2.1.7 | TicketAttachment | `TenantAware`, 多態 (ticket_type + ticket_id) | ✅ |
| 1.2.2.1.8 | AttachmentPhase Enum | BEFORE/DURING/AFTER/REPORT | ✅ |
| 1.2.2.1.9 | ScanStatus Enum | PENDING/CLEAN/INFECTED | ✅ |

### 1.2.2.2 Repository 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.2.2.2.1 | RepairTicketRepository (分頁+多條件+DataScope) | ✅ |
| 1.2.2.2.2 | RepairDispatchRepository | ✅ |
| 1.2.2.2.3 | TicketAttachmentRepository | ✅ |

### 1.2.2.3 DTO 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.2.2.3.1 | RepairTicketRequest / Response | ✅ |
| 1.2.2.3.2 | RepairTicketQueryParams | 多條件篩選 | ✅ |
| 1.2.2.3.3 | DispatchRequest | 派工請求 | ✅ |
| 1.2.2.3.4 | CompletionReportRequest | 完工回報 | ✅ |
| 1.2.2.3.5 | AttachmentUploadRequest / AttachmentResponse | ✅ |

### 1.2.2.4 Service 層

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.2.2.4.1 | RepairTicketService | 雙路徑建單 + 收案 + 完工回報 + 改分轉送 + DataScope | ✅ |
| 1.2.2.4.2 | RepairDispatchService | 派工 + 退回重派 | ✅ |
| 1.2.2.4.3 | TicketAttachmentService | 上傳 + 病毒掃描狀態 + 查詢 | ✅ |
| 1.2.2.4.4 | LocalFileStorageService | 本地存儲 + 路徑遍歷防護 | ✅ |

### 1.2.2.5 事件監聽器

| WBS ID | 工作包 | 事件 | 說明 | 狀態 |
|--------|-------|------|------|------|
| 1.2.2.5.1 | FaultApprovedListener | E1 | 障礙審核通過 → 自動建 repair_ticket | ✅ |
| 1.2.2.5.2 | RepairDispatchedListener | E4 | 派工 → 設備狀態改 UNDER_REPAIR | ✅ |
| 1.2.2.5.3 | RepairClosedListener | E9 | 結案審核通過 → 設備復原 ACTIVE + 寫歷程 | ✅ |

### 1.2.2.6 Controller 層

| WBS ID | 工作包 | 端點數 | 狀態 |
|--------|-------|--------|------|
| 1.2.2.6.1 | RepairTicketController | 9 | ✅ |
| 1.2.2.6.2 | TicketAttachmentController | 3 | ✅ |

---

## 1.2.3 後端 — 巡查模組 (`com.taipei.iot.repair`)

### 1.2.3.1 Entity 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.2.3.1.1 | InspectionTask | `TenantAware` | ✅ |
| 1.2.3.1.2 | InspectionRecord | `TenantAware` | ✅ |
| 1.2.3.1.3 | 列舉型別 | InspectionType / InspectionStatus / InspectionFrequency / InspectionResult | ✅ |

### 1.2.3.2 Service 層

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.2.3.2.1 | InspectionService | CRUD + E13（異常→自動建 fault_ticket） | ✅ |

### 1.2.3.3 Controller 層

| WBS ID | 工作包 | 端點數 | 狀態 |
|--------|-------|--------|------|
| 1.2.3.3.1 | InspectionController | 8 | ✅ |

---

## 1.2.4 前端 — 報修維護

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.2.4.1 | TypeScript 型別 | `types/repair.ts` (10 enum + 12 interface) | ✅ |
| 1.2.4.2 | API 模組 | `api/repair/index.ts` (11 函式) | ✅ |
| 1.2.4.3 | API 模組 | `api/inspection/index.ts` (8 函式) | ✅ |
| 1.2.4.4 | Store | `stores/repairStore.ts` | ✅ |
| 1.2.4.5 | 報修工單列表 | `RepairTicketView.vue` (篩選+分頁+新增) | ✅ |
| 1.2.4.6 | 工單詳情 | `RepairTicketDetailView.vue` (4 Tabs + Action) | ✅ |
| 1.2.4.7 | 派工 Dialog | `RepairDispatchDialog.vue` | ✅ |
| 1.2.4.8 | 完工回報 Dialog | `CompletionReportDialog.vue` | ✅ |
| 1.2.4.9 | 巡查任務列表 | `InspectionView.vue` | ✅ |
| 1.2.4.10 | 巡查紀錄列表 | `InspectionRecordView.vue` | ✅ |
| 1.2.4.11 | 附件上傳元件 | `AttachmentUploader.vue` (多檔+GPS+拖放) | ✅ |
| 1.2.4.12 | 附件瀏覽元件 | `AttachmentGallery.vue` (phase 分組+預覽) | ✅ |
| 1.2.4.13 | Store 更新 | `menuStore.ts` — implicitChildren 路由 | ✅ |
| 1.2.4.14 | 路由 | 4 條路由 (tickets, :id, inspection, :taskId/records) | ✅ |
| 1.2.4.15 | 國際化 | repair + inspection (zh-TW/en/zh-CN) | ✅ |

---

## 1.2.5 單元測試

| WBS ID | 工作包 | 測試類 | 狀態 |
|--------|-------|--------|------|
| 1.2.5.1 | 報修 Controller | RepairTicketControllerTest | ✅ |
| 1.2.5.2 | 報修 Service | RepairTicketServiceTest | ✅ |
| 1.2.5.3 | 派工 Service | RepairDispatchServiceTest | ✅ |
| 1.2.5.4 | 附件 Service | TicketAttachmentServiceTest | ✅ |
| 1.2.5.5 | 附件 Controller | TicketAttachmentControllerTest | ✅ |
| 1.2.5.6 | 巡查 Controller | InspectionControllerTest | ✅ |
| 1.2.5.7 | 巡查 Service | InspectionServiceTest | ✅ |
| 1.2.5.8 | 檔案存儲 | LocalFileStorageServiceTest | ✅ |

---

## 1.2.6 共用基礎設施

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.2.6.1 | AuditCategory 擴充 | MAINTENANCE | ✅ |
| 1.2.6.2 | AuditEventType 擴充 | CREATE/UPDATE/DISPATCH/COMPLETE/CLOSE_REPAIR | ✅ |
| 1.2.6.3 | ErrorCode 擴充 | 70xxx 系列 (7 個) | ✅ |
| 1.2.6.4 | WorkflowServiceImpl 擴充 | REPAIR_DISPATCH + REPAIR_CLOSE + INITIAL_STEPS | ✅ |
