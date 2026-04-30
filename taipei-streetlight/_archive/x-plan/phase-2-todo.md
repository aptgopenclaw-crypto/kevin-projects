# Phase 2 — 報修派工模組

> 最後更新：2026-04-23
> 後端：✅ 已完成（含修正）｜前端：✅ 已完成｜選單修正：✅ 已完成

---

## 已修正問題

| # | 問題 | 修正內容 |
|---|------|----------|
| 1 | V35 migration 建在錯誤路徑 `backend/` | 已複製到正確路徑 `project/backend/` 並以 **V37** 編號套用（因 V36 已先行套用） |
| 2 | V36 SQL 欄位名稱不符實際 schema | `id`→`menu_id`、`menu_name`→`name`、`perm_code`→`permission_code` 等；改用 `DO $$` + `RETURNING` 取代硬編 menu_id |
| 3 | V36 使用不存在的 `role_menu` 表 | 改用 `permissions` + `role_permissions` 表，與既有 RBAC 模式一致 |
| 4 | `WorkflowServiceImpl` 初始步驟不穩定 | `Map.of()` 不保證順序，新增 `INITIAL_STEPS` 明確映射表 |
| 5 | V36 選單 `parent_id=0`、缺 `route_name`、`component` 路徑錯誤 | V38 修正：`parent_id`→`NULL`、補 `route_name`、`component` 加 `views/` 前綴 + `.vue`、`menu_type` 改 `PAGE` |
| 6 | V36 PAGE 選單 `permission_code=NULL` 導致選單不顯示 | V39 修正：設定 `permission_code` 為 `REPAIR_VIEW` / `INSPECTION_VIEW` |

---

## 後端完成清單

- [x] Flyway V36：inspection_tasks / inspection_records / 選單 / 權限 / 角色綁定
- [x] Flyway V37（原 V35）：repair_tickets / repair_dispatches / ticket_attachments
- [x] Flyway V38：修正選單 parent_id / route_name / component / menu_type
- [x] Flyway V39：修正選單 permission_code（REPAIR_VIEW / INSPECTION_VIEW）
- [x] Entities × 5 + Enums × 10
- [x] Repositories × 5（含 DataScope 分頁篩選）
- [x] DTOs × 12
- [x] RepairTicketService（雙路徑建單 + 收案 + 完工回報 + 改分轉送 + DataScope 列表）
- [x] RepairDispatchService（派工 + 退回重派）
- [x] TicketAttachmentService（上傳 + 下載 + 列表）
- [x] InspectionService（CRUD + E13 異常→自動建 fault_ticket）
- [x] LocalFileStorageService（本地檔案存儲 + 路徑遍歷防護）
- [x] 事件監聽器：E1（障礙審核→自動建報修單）、E4（派工→設備狀態維修中）、E9（結案→設備復原+歷程）
- [x] RepairTicketController（9 端點）+ TicketAttachmentController（3 端點）+ InspectionController（8 端點）
- [x] 審計：AuditCategory.MAINTENANCE + 5 個 AuditEventType
- [x] ErrorCode 70xxx 系列（7 個）
- [x] WorkflowServiceImpl：REPAIR_DISPATCH + REPAIR_CLOSE 轉換表 + INITIAL_STEPS
- [x] 單元測試 × 8 檔案，`mvn test` 390 tests 全過

---

## 前端完成清單

### 1. TypeScript 型別定義

- [x] `src/types/repair.ts` — 10 enum type aliases + 12 interfaces + PageResponse\<T\>

### 2. API 模組

- [x] `src/api/repair/index.ts` — 11 函式（CRUD + 派工 + 完工 + 改分 + 附件上傳/下載）
- [x] `src/api/inspection/index.ts` — 8 函式（巡查任務 CRUD + 巡查紀錄）

### 3. 狀態管理

- [x] `src/stores/repairStore.ts` — tickets 列表 + 分頁 + loading + currentTicket + stats
- [x] `src/stores/menuStore.ts` — 新增 implicitChildren: RepairTicketDetail → RepairTicket, InspectionRecord → InspectionTask

### 4. 頁面

- [x] `src/views/admin/repair/RepairTicketView.vue` — 報修工單列表 + 狀態/來源/優先度篩選 + 分頁 + 新增 Dialog
- [x] `src/views/admin/repair/RepairTicketDetailView.vue` — 工單詳情：4 Tabs（基本資料/派工記錄/附件/流程歷程）+ 動態 Action 按鈕
- [x] `src/views/admin/repair/RepairDispatchDialog.vue` — 派工 Dialog（派工單位/契約/期限/備註）
- [x] `src/views/admin/repair/CompletionReportDialog.vue` — 完工回報 Dialog（維修說明/故障原因）
- [x] `src/views/admin/repair/InspectionView.vue` — 巡查任務列表 + 新增/編輯/停用 + 查看紀錄
- [x] `src/views/admin/repair/InspectionRecordView.vue` — 巡查紀錄列表 + 新增紀錄 Dialog

### 5. 共用元件

- [x] `src/components/AttachmentUploader.vue` — 多檔上傳 + GPS 自動讀取 + phase 標記 + 拖放區域
- [x] `src/components/AttachmentGallery.vue` — 依 phase 分組 + 圖片預覽 + GPS 顯示 + 下載

### 6. 路由

- [x] `src/router/index.ts` — 新增 4 條路由（tickets, tickets/:id, inspection, inspection/:taskId/records）

### 7. 國際化

- [x] `src/locales/zh-TW.ts` — 新增 `repair` + `inspection` 區段
- [x] `src/locales/en.ts` — 同上（英文）
- [x] `src/locales/zh-CN.ts` — 同上（簡體中文）

### 8. 編譯驗證

- [x] `vue-tsc --noEmit` 通過（零錯誤）
- [x] `npm run build` 新增檔案零錯誤（14 個既有錯誤均在其他模組）

---

## 備註

- 既有 `WorkflowStepper.vue` 和 `WorkflowActionBar.vue` 已複用於報修詳情頁
- 選單結構經 V38 + V39 修正後正確：`parent_id=NULL` + `menu_type=PAGE` + `route_name` + `permission_code`
- 後端 API 路徑：`/v1/auth/repair/*` 和 `/v1/auth/inspection/*`
