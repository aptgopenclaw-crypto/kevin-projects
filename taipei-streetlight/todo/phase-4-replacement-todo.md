# Phase 4 — 換裝維護（換裝工單 + 號碼牌 + 自檢）TODO

> **建立日期**: 2026-04-27  
> **最後更新**: 2026-04-27 (全部完成)  
> **甘特圖**: 04/20 – 05/02 (2 週)  
> **前置**: Phase 2 報修（RepairTicket → ReplacementOrder）、Phase 3 材料（IssueRequest E6）  
> **執行計畫**: 99-plan/2026-04-24-gantt.md §Phase 4  
> **關鍵路徑**: Phase 2 → **Phase 4** → Phase 5  
> **里程碑**: m0d — 2026-05-02  
> **WBS**: 04-wbs/phase-4.md  
> **SRS 對應**: SRS-06-001~012

---

### 進度總覽

| 區塊 | Gantt ID | 工期 | 進度 | 說明 |
|------|----------|------|------|------|
| 4a DB Schema (V45–V47) | 4a | 2d | ✅ | 3 tables + menu/permission |
| 4b 換裝模組 (7 狀態 FSM + E6/E10) | 4b | 8d | ✅ | 12 端點 + 2 Listener |
| 4c 號碼牌 + QR Code 生成 | 4c | 3d | ✅ | LightPoleNumberService + QR |
| 4d 前端 10 頁面 | 4d | 8d | ✅ | 10 Vue pages |
| 4e 單元測試 (7 類) | 4e | 3d | ✅ | 7 test classes |

### 甘特依賴

```
4a DB Schema (2d) ──→ 4b 換裝模組 (8d) ──→ 4e 測試 (3d)
       │
       ├──→ 4c 號碼牌 + QR (3d)
       │
       └──→ 4d 前端 10 頁面 (8d, 與後端並行) ──┘
```

---

## 4a — DB Schema V45–V47 (2 天) ✅ 已完成

> **SD**: SD-05-replacement.md

### Flyway Migrations

- [x] **V45** — 換裝 DDL: `replacement_orders`, `replacement_items`, `light_pole_numbers`
- [x] **V46** — 選單 + 權限 + 角色綁定
- [x] **V47** — 欄位修正

### 表格統計 (3 tables)

| 表 | 說明 |
|----|------|
| replacement_orders | 換裝工單主表，7 狀態 FSM |
| replacement_items | 換裝項目，FK → MaterialSpec |
| light_pole_numbers | 號碼牌管理，QR Code |

---

## 4b — 換裝模組 (8 天) ✅ 已完成

> **Package**: `com.taipei.iot.replacement`  
> **SD**: SD-05-replacement.md

### Entity

- [x] ReplacementOrder — TenantAware, FK → Contract, RepairTicket (nullable)
- [x] ReplacementItem — TenantAware, FK → ReplacementOrder, MaterialSpec
- [x] LightPoleNumber — TenantAware, QR Code 生成
- [x] ReplacementOrderStatus Enum — 7 狀態 FSM (DRAFT→DISPATCHED→IN_PROGRESS→SELF_CHECKED→PENDING_REVIEW→CLOSED/RETURNED)
- [x] ReplacementOrderType Enum — 6 種工單類型
- [x] ReplacementItemStatus Enum — PENDING/INSTALLED/CANCELLED

### Service

- [x] ReplacementOrderService — 雙路徑建單 (repair→replacement / independent)
- [x] 派工 dispatch
- [x] 開工 startWork
- [x] 自主檢核 selfCheck (replaceComponent + deviceCode writeback)
- [x] 送審 submitReview
- [x] 審核通過 approve
- [x] 退回補件 returnOrder + 重送 resubmit
- [x] ReplacementItemService — addItem + 合格材料管控 (ACTIVE check)
- [x] LightPoleNumberService — CRUD + QR Code 生成
- [x] SystemSettingService — getSetting (FRONTEND_BASE_URL for QR)

### 事件監聽器

- [x] **E10** ReplacementClosedListener — 結案 → 設備 ACTIVE + 寫歷程 + 新設備入帳
- [x] **E6** ReplacementNeedMaterialListener — 換裝建立 → 自動建 IssueRequest

### Controller

- [x] ReplacementOrderController — 12 端點 (CRUD + 狀態操作)
- [x] ReplacementItemController — 4 端點 (add/list/update/delete)
- [x] LightPoleNumberController — 4 端點 (CRUD + QR)

---

## 4c — 號碼牌 + QR Code 生成 (3 天) ✅ 已完成

> **SRS**: SRS-06-(1)

- [x] LightPoleNumber Entity — 號碼牌編碼規則
- [x] LightPoleNumberService — CRUD + QR Code 生成
- [x] QR Code 內容: `{FRONTEND_BASE_URL}/pole/{id}` (讀取 SystemSetting)
- [x] LightPoleNumberController — 4 端點

---

## 4d — 前端 10 頁面 (8 天) ✅ 已完成

- [x] TypeScript 型別: `types/replacement.ts`
- [x] API 模組: `api/replacement/index.ts`
- [x] Store: `stores/replacementStore.ts`
- [x] ReplacementOrderView.vue — 工單列表
- [x] ReplacementOrderDetailView.vue — Tabs + Action
- [x] SelfCheckView.vue — 自主檢核
- [x] LightPoleNumberView.vue — 號碼牌管理
- [x] 4 routes under `/admin/replacement/*`
- [x] 國際化: replacement (zh-TW/en/zh-CN)
- [x] IssueService 擴充: `createFromReplacement()` 整合

---

## 4e — 單元測試 (3 天) ✅ 已完成

| # | 測試類 | 狀態 |
|---|--------|------|
| 1 | ReplacementOrderControllerTest | ✅ |
| 2 | ReplacementOrderServiceTest | ✅ |
| 3 | ReplacementItemServiceTest | ✅ |
| 4 | LightPoleNumberServiceTest | ✅ |
| 5 | LightPoleNumberControllerTest | ✅ |
| 6 | ReplacementClosedListenerTest (E10) | ✅ |
| 7 | SystemSettingServiceTest | ✅ |

---

## 共用基礎設施 ✅ 已完成

- [x] AuditCategory 擴充: REPLACEMENT
- [x] AuditEventType 擴充: CREATE/DISPATCH/SELF_CHECK/APPROVE/CLOSE_REPLACEMENT
- [x] ErrorCode 擴充: 80xxx 系列
- [x] WorkflowServiceImpl 擴充: REPLACEMENT_REVIEW FSM
- [x] IssueService 擴充: createFromReplacement (E6)
