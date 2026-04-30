# WBS 1.4 — Phase 4：換裝維護

> **狀態**：✅ 已完成  
> **Flyway**：V45–V47  
> **SRS 對應**：SRS-06-001~012  
> **x-plan**：`/_archive/x-plan/phase-4-replacement.md`

---

## 1.4.1 資料庫 Schema

| WBS ID | 工作包 | Flyway | 產出 | 狀態 |
|--------|-------|--------|------|------|
| 1.4.1.1 | 換裝工單 + 項目 + 號碼牌 DDL | V45 | `replacement_orders`, `replacement_items`, `light_pole_numbers` | ✅ |
| 1.4.1.2 | 選單 + 權限 + 角色綁定 | V46 | menu + permission + role binding | ✅ |
| 1.4.1.3 | 修補/調整 | V47 | 欄位修正 | ✅ |

---

## 1.4.2 後端 — 換裝模組 (`com.taipei.iot.replacement`)

### 1.4.2.1 Entity 層

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.4.2.1.1 | ReplacementOrder | `TenantAware`, FK → Contract, RepairTicket (nullable) | ✅ |
| 1.4.2.1.2 | ReplacementItem | `TenantAware`, FK → ReplacementOrder, MaterialSpec | ✅ |
| 1.4.2.1.3 | LightPoleNumber | `TenantAware`, QR Code 生成 | ✅ |
| 1.4.2.1.4 | ReplacementOrderStatus Enum | 7 狀態 FSM (DRAFT→DISPATCHED→IN_PROGRESS→SELF_CHECKED→PENDING_REVIEW→CLOSED/RETURNED) | ✅ |
| 1.4.2.1.5 | ReplacementOrderType Enum | 6 種工單類型 | ✅ |
| 1.4.2.1.6 | ReplacementItemStatus Enum | PENDING/INSTALLED/CANCELLED | ✅ |

### 1.4.2.2 Repository 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.4.2.2.1 | ReplacementOrderRepository (分頁+DataScope) | ✅ |
| 1.4.2.2.2 | ReplacementItemRepository | ✅ |
| 1.4.2.2.3 | LightPoleNumberRepository | ✅ |

### 1.4.2.3 DTO 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.4.2.3.1 | ReplacementOrderRequest / Response | ✅ |
| 1.4.2.3.2 | ReplacementItemRequest / Response | ✅ |
| 1.4.2.3.3 | SelfCheckRequest | 自主檢核提交 | ✅ |
| 1.4.2.3.4 | LightPoleNumberRequest / Response | ✅ |

### 1.4.2.4 Service 層

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.4.2.4.1 | ReplacementOrderService | 雙路徑建單 (repair→replacement / independent) | ✅ |
| 1.4.2.4.2 | — | 派工 dispatch | ✅ |
| 1.4.2.4.3 | — | 開工 startWork | ✅ |
| 1.4.2.4.4 | — | 自主檢核 selfCheck (replaceComponent + deviceCode writeback) | ✅ |
| 1.4.2.4.5 | — | 送審 submitReview | ✅ |
| 1.4.2.4.6 | — | 審核通過 approve | ✅ |
| 1.4.2.4.7 | — | 退回補件 returnOrder + 重送 resubmit | ✅ |
| 1.4.2.4.8 | ReplacementItemService | addItem + 合格材料管控 (ACTIVE check) | ✅ |
| 1.4.2.4.9 | LightPoleNumberService | CRUD + QR Code 生成 | ✅ |
| 1.4.2.4.10 | SystemSettingService | getSetting (FRONTEND_BASE_URL for QR) | ✅ |

### 1.4.2.5 事件監聽器

| WBS ID | 工作包 | 事件 | 說明 | 狀態 |
|--------|-------|------|------|------|
| 1.4.2.5.1 | ReplacementClosedListener | E10 | 結案 → 設備 ACTIVE + 寫歷程 + 新設備入帳 | ✅ |
| 1.4.2.5.2 | ReplacementNeedMaterialListener | E6 | 換裝建立 → 自動建 IssueRequest | ✅ |

### 1.4.2.6 Controller 層

| WBS ID | 工作包 | 端點數 | 狀態 |
|--------|-------|--------|------|
| 1.4.2.6.1 | ReplacementOrderController | 12 (CRUD + 狀態操作) | ✅ |
| 1.4.2.6.2 | ReplacementItemController | 4 (add/list/update/delete) | ✅ |
| 1.4.2.6.3 | LightPoleNumberController | 4 (CRUD + QR) | ✅ |

---

## 1.4.3 前端 — 換裝維護

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.4.3.1 | TypeScript 型別 | `types/replacement.ts` | ✅ |
| 1.4.3.2 | API 模組 | `api/replacement/index.ts` | ✅ |
| 1.4.3.3 | Store | `stores/replacementStore.ts` | ✅ |
| 1.4.3.4 | 換裝工單列表 | `ReplacementOrderView.vue` | ✅ |
| 1.4.3.5 | 工單詳情 | `ReplacementOrderDetailView.vue` (Tabs + Action) | ✅ |
| 1.4.3.6 | 自主檢核頁 | `SelfCheckView.vue` | ✅ |
| 1.4.3.7 | 號碼牌管理 | `LightPoleNumberView.vue` | ✅ |
| 1.4.3.8 | 路由 | 4 routes under `/admin/replacement/*` | ✅ |
| 1.4.3.9 | 國際化 | replacement (zh-TW/en/zh-CN) | ✅ |
| 1.4.3.10 | IssueService 擴充 | `createFromReplacement()` 整合 | ✅ |

---

## 1.4.4 單元測試

| WBS ID | 工作包 | 測試類 | 狀態 |
|--------|-------|--------|------|
| 1.4.4.1 | 換裝 Controller | ReplacementOrderControllerTest | ✅ |
| 1.4.4.2 | 換裝 Service | ReplacementOrderServiceTest | ✅ |
| 1.4.4.3 | 項目 Service | ReplacementItemServiceTest | ✅ |
| 1.4.4.4 | 號碼牌 Service | LightPoleNumberServiceTest | ✅ |
| 1.4.4.5 | 號碼牌 Controller | LightPoleNumberControllerTest | ✅ |
| 1.4.4.6 | E10 Listener | ReplacementClosedListenerTest | ✅ |
| 1.4.4.7 | 系統設定 Service | SystemSettingServiceTest | ✅ |

---

## 1.4.5 共用基礎設施

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.4.5.1 | AuditCategory 擴充 | REPLACEMENT | ✅ |
| 1.4.5.2 | AuditEventType 擴充 | CREATE/DISPATCH/SELF_CHECK/APPROVE/CLOSE_REPLACEMENT | ✅ |
| 1.4.5.3 | ErrorCode 擴充 | 80xxx 系列 | ✅ |
| 1.4.5.4 | WorkflowServiceImpl 擴充 | REPLACEMENT_REVIEW FSM | ✅ |
| 1.4.5.5 | IssueService 擴充 | createFromReplacement (E6) | ✅ |
