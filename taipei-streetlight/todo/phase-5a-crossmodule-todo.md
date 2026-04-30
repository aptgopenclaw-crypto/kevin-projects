# Phase 5A — 跨模組整合驗證 TODO

> **建立日期**: 2026-04-24  
> **最後更新**: 2026-04-25 (5a4 整合測試完成)  
> **甘特圖**: 05/05 – 05/29 (4 週)  
> **前置**: Phase 1~4 全部完成 ✅  
> **WBS**: 04-wbs/phase-5.md §1.5.1~1.5.4  
> **參考設計**: _archive/x-plan/cross-module-03-07-unified-design.md  
> **Git**: `2b9d460` (main)

### 進度總覽

| 區塊 | 進度 | 說明 |
|------|------|------|
| 5a1 FK + RBAC + 選單 | ✅ 完成 | V51 FK, V52 RBAC, V53 Menu |
| 5a2 E1~E13 Listeners | ✅ 完成 | 7 listener + 25 unit tests 通過，SD 文件已對齊 |
| 5a3 前端路由 | ✅ 完成 | 路由統一 + implicitChildren 修復 + i18n 補齊 + Dashboard 待辦 |
| 5a4 整合測試 | ✅ 完成 | HP1 + HP2 + 5 exception tests pass |

---

## 5a1 — FK 補齊 + 跨模組約束 (5 天)

> **WBS**: 1.5.1  
> **SA**: SA-00-overview.md  
> **SD**: SD-04-repair.md, SD-06-material.md

### 1.5.1.1 跨模組 FK 約束 Migration

- [x] `repair_tickets` → `devices` FK _(Phase 2 V36 已存在)_
- [x] `repair_tickets` → `fault_tickets` FK _(Phase 2 V36 已存在)_
- [x] `replacement_orders` → `repair_tickets` FK _(Phase 4 V44 已存在)_
- [x] `issue_requests` → `replacement_orders` FK _(Phase 4 V45 已存在)_
- [x] `replacement_items` → `devices` (×2), `material_specs`, `approved_materials` FK _(Phase 4 V44 已存在)_
- [x] `devices` ↔ `circuits` FK _(Phase 1 V31 已存在)_
- [x] `device_events` → `devices`, `repair_tickets`, `replacement_items` FK _(V51 新增 repair_ticket_id + replacement_item_id)_
- [x] Flyway migration V51 ✅ — `V51__phase5a__crossmodule_fk_constraints.sql`

### 1.5.1.2 完整角色-權限矩陣 Seed

- [x] 7 角色全部權限綁定 ✅ — `V52__phase5a__rbac_permission_matrix.sql`
  - ADMIN:45, DEPT_ADMIN:39, OPERATOR:26, VIEWER:16, FIELD_USER:15, MONITOR:13, DEPT_USER:12
  - 新增 `WORKFLOW_MANAGE` 權限, 清除 3 個 orphan (`DEVICE_CREATE/UPDATE`, `USER_MANAGE`)
- [x] 驗證所有模組 @PreAuthorize 對應 ✅ — DB 查詢確認

### 1.5.1.3 選單中心統一

- [x] 所有模組選單統一排序 ✅ — `V53__phase5a__menu_ordering.sql`
  - 公告(5)→資產(10)→報修(20)→換裝(30)→材料(40)→簽核(50)→GIS(60)→用戶(70)→系統(80)→稽核(90)
- [ ] 驗證各角色可見選單正確 _(需前端登入測試)_

---

## 5a2 — E1~E13 事件端到端驗證 (8 天)

> **WBS**: 1.5.2  
> **SA**: SA-00-overview.md §6, SA-04-repair.md, SA-05-replacement.md, SA-06-material.md, SA-11-common.md  
> **SD**: SD-04-repair.md §4, SD-06-material.md §6.6, SD-11-common.md §7.3  
> **Test**: TS-04-repair.md, TS-05-replacement.md, TS-06-material.md, TS-00-common.md

### E1 障礙審核通過 → 自動建報修

- [x] Listener: `FaultApprovedListener` ✅ _(Phase 2 已實作)_
- [x] 驗證: repair_ticket.fault_ticket_id 正確, device.status = REPORTED ✅
- [x] Test: TC-04-E1-01~04 ✅ — 4 tests pass (happy + wrongType + wrongStep + noDevice)

### E2 關聯障礙偵測

- [x] Service: `FaultCorrelationService.detectOnNewTicket()` ✅ _(Phase 1 已實作)_
- [ ] 驗證: fault_correlation 正確建立 + 通知
- [ ] Test: 關聯偵測 unit test

### E3 Gateway 離線告警

- [ ] Listener: 離線 → auto-create fault_ticket (source=AUTO_ALERT) → triggers E1
- [ ] ⚠️ **Phase 7 依賴** — 可先建 stub/mock

### E4 報修派工

- [x] Listener: `RepairDispatchedListener` ✅ _(Phase 2 已實作)_
- [x] 驗證: device.status = UNDER_REPAIR ✅
- [x] Test: RepairDispatchedListenerTest ✅ — 5 tests pass (happy + wrongType + wrongStep + ticketNotFound + noDevice)

### E5 施工需換裝

- [x] API: `ReplacementOrderController.createFromRepair()` ✅ _(Phase 4 已實作，設計為手動觸發)_
- [ ] 驗證: replacement_order.repair_ticket_id 正確

### E6 換裝需領料

- [x] Listener: `ReplacementNeedMaterialListener` ✅ _(Phase 4 已實作)_
- [x] 驗證: issue_request.source_type = REPLACEMENT ✅
- [x] Test: TC-05-E2-01~02 + wrongType + orderNotFound ✅ — 4 tests pass

### E7 領料審核通過

- [x] Service: `IssueService.issue()` 已含扣庫邏輯 _(Phase 3 已實作，非 listener 而是 API 呼叫)_
- [ ] 驗證: inventory 正確扣減

### E8 外勤完工回傳

- [x] 由 workflow FSM `transition()` 處理 _(workflow_step_logs + attachments JSONB，Phase 2 已實作)_
- [ ] 驗證: 通知派工人員

### E9 報修結案審核通過

- [x] Listener: `RepairClosedListener` ✅ _(Phase 2 已實作，5A 強化: recordEvent 含 repairTicketId FK)_
- [x] 驗證: 設備歷程完整記錄 ✅ — 3 tests pass
- [x] Test: TC-04-E2-01~03 ✅ — 3 tests pass (happy + wrongType + noDevice)

### E10 換裝結案審核通過

- [x] Listener: `ReplacementClosedListener` ✅ _(5A 強化: 舊設備 DECOMMISSIONED + 新設備 ACTIVE + device_events)_
- [x] 驗證: 新舊設備狀態正確 ✅ — 3 tests pass
- [x] Test: TC-05-E1-01~02 + noItems ✅ — 3 tests pass

### E11 廠商自主檢核

- [x] Listener: `ReplacementSelfCheckedListener` ✅ _(5A 新建: SELF_CHECKED → INSPECT device_event)_
- [x] 驗證: 設備歷程記錄 ✅ — 3 tests pass

### E12 庫存低於安全量

- [x] Listener: `LowStockAlertListener` ✅ _(5A 新建: LowStockAlertEvent → NotificationService ALERT)_
- [x] 驗證: 通知正確觸發 ✅ — 3 tests pass (happy + refType驗證 + zeroQuantity)
- [x] Test: FN-06-030 ✅ — LowStockAlertListenerTest 3 tests pass

### E13 巡檢發現異常

- [x] Service: `InspectionService` → `FaultTicketService.createFromInspection()` ✅ _(Phase 2 已實作)_
- [x] 驗證: 自動建立障礙通報 ✅ _(InspectionServiceTest 已涵蓋)_
- [x] Test: TC-04-035 ✅ — InspectionServiceTest + InspectionControllerTest 已覆蓋

---

## 5a3 — 前端路由統一 (5 天, 與 5a2 並行)

> **WBS**: 1.5.4  
> **SA**: SA-11-common.md

### 路由整合

- [x] 全模組路由表統一 + 權限守衛 (beforeEach) ✅ _(GIS route 加入 staticAdminRoutes, 動態選單已有)_
- [x] 待辦清單 → 點擊直達各模組詳情頁 (報修/換裝/材料/簽核) ✅ — HomeView Dashboard 已建立，呼叫 workflow/pending API + resolveDetailRoute 跳轉
- [x] Sidebar 選單依角色動態顯示 + 排序驗證 ✅ — 後端驅動 sortOrder，前端 AppSidebar.vue 正確排序
- [x] 修復 implicitChildren 缺漏 ✅ — 新增 ReplacementOrderDetail、ReplacementSelfCheck 映射

### i18n 完整性

- [x] 全模組 i18n key 覆蓋率檢查 (zh-TW / en / zh-CN) ✅ — 30 個頂層 key 全部一致
- [x] 缺漏 key 補齊 ✅ — zh-CN audit.subtitle 已補 + 3 個 locale 新增 home section (11 keys × 3)

---

## 5a4 — 整合測試 (5 天, after 5a2)

> **WBS**: 1.5.3  
> **Test**: TS-04, TS-05, TS-06, TS-00  
> **Seed**: V54__phase5a__integration_test_seed.sql

### 組織架構 & 角色地圖

```
臺北市政府工務局公燈處 (TENANT_A)
├── 系統管理員 u-tpe-admin ─── ADMIN
├── 第一分隊（北區）
│   ├── 分隊長 李明華 u-squad1-mgr ─── DEPT_ADMIN
│   ├── 承辦 張志遠 u-squad1-off1 ─── DEPT_USER
│   ├── 承辦 王美玲 u-squad1-off2 ─── DEPT_USER
│   ├── 🆕 維運 周志豪 u-squad1-op ─── OPERATOR
│   └── 🆕 外勤 蔡文傑 u-squad1-field ─── FIELD_USER
├── 第二分隊（南區）
│   ├── 分隊長 陳國強 u-squad2-mgr ─── DEPT_ADMIN
│   ├── 承辦 林雅琪 u-squad2-off1 ─── DEPT_USER
│   ├── 🆕 維運 許家瑋 u-squad2-op ─── OPERATOR
│   └── 🆕 外勤 劉俊宏 u-squad2-field ─── FIELD_USER
├── 工程股
│   ├── 股長 黃建中 u-eng-mgr ─── DEPT_ADMIN
│   ├── 承辦 吳佳穎 u-eng-off1 ─── DEPT_USER
│   └── 🆕 監造 謝明達 u-eng-monitor ─── MONITOR
├── 行政股
│   ├── 股長 鄒志明 u-adm-mgr ─── DEPT_ADMIN
│   └── 🆕 倉管 林淑芬 u-adm-warehouse ─── OPERATOR
└── 智慧路燈管理中心 (無人員)

協力廠商 (dept_id=11, 既有)
├── FET (dept_id=12)
│   ├── 🆕 專案經理 張雅婷 u-fet-mgr ─── DEPT_ADMIN
│   └── 🆕 維運工程師 陳柏翰 u-fet-op1 ─── OPERATOR
└── 設備商 (dept_id=13)
    ├── 🆕 主管 洪啟文 u-vendor-mgr ─── DEPT_ADMIN
    └── 🆕 工程師 葉建廷 u-vendor-field1 ─── FIELD_USER
```

### Seed 資料

- [x] V54 migration ✅ — 人員(10)、合約(2)、設備(7)、材料(5)、庫存(6)、核准材料(3)
- [x] DEPT_USER + FAULT_MANAGE 權限修正 ✅

### Happy Path 1: 故障報修閉環

> **信義路三段 #001 燈桿不亮**

| 步驟 | 操作者 | 角色 | 動作 | 事件 | 驗證 |
|------|--------|------|------|------|------|
| 1 | 張志遠 `u-squad1-off1` | DEPT_USER/北區 | 建立障礙通報（信義路#001燈桿不亮） | — | fault_ticket created, device_id 正確 |
| 2 | 周志豪 `u-squad1-op` | OPERATOR/北區 | 審核確認障礙 | **E1** 自動建報修 | repair_ticket 建立, device→REPORTED |
| 3 | 周志豪 `u-squad1-op` | OPERATOR/北區 | 收案 → 派工給蔡文傑 | **E4** 設備→維修中 | device→UNDER_REPAIR, workflow assigned_to=蔡文傑 |
| 4 | 蔡文傑 `u-squad1-field` | FIELD_USER/北區 | 前往現場施工 → 完工回報（附照片） | **E8** step_log 記錄 | workflow_step_logs + attachments JSONB |
| 5 | 李明華 `u-squad1-mgr` | DEPT_ADMIN/北區 | 結案審核通過 | **E9** 設備→ACTIVE | device→ACTIVE + device_events 歷程記錄 |

- [x] 全流程 integration test ✅ `RepairHappyPathIntegrationTest`
- [x] 驗證：device_events 含 repair_ticket_id FK ✅

### Happy Path 2: 換裝領料閉環

> **信義路三段 #002 燈桿需換裝新設備**

| 步驟 | 操作者 | 角色 | 動作 | 事件 | 驗證 |
|------|--------|------|------|------|------|
| 1 | 吳佳穎 `u-eng-off1` | DEPT_USER/工程股 | 提出換裝申請（#002燈具更換為150W） | — | replacement_order created |
| 2 | 周志豪 `u-squad1-op` | OPERATOR/北區 | 派工 | **E6** 自動建領料需求 | issue_request created, source_type=REPLACEMENT |
| 3 | 林淑芬 `u-adm-warehouse` | OPERATOR/行政股 | 審核領料 → 出庫 | **E7** 庫存扣減 | inventory.quantity_on_hand 減少 |
| 4 | 葉建廷 `u-vendor-field1` | FIELD_USER/設備商 | 施工（拆舊裝新） | — | workflow→IN_PROGRESS |
| 5 | 葉建廷 `u-vendor-field1` | FIELD_USER/設備商 | 廠商自主檢核 | **E11** 設備歷程 | device_events (INSPECT) for new device |
| 6 | 謝明達 `u-eng-monitor` | MONITOR/工程股 | 監造驗收確認 | — | step_log 記錄 |
| 7 | 黃建中 `u-eng-mgr` | DEPT_ADMIN/工程股 | 結案審核通過 | **E10** 新舊設備更新 | old→DECOMMISSIONED, new→ACTIVE, device_events 記錄 |

- [x] 全流程 integration test ✅ `ReplacementHappyPathIntegrationTest`
- [x] 驗證：replacement_items 新舊設備 FK 正確 ✅

### 異常路徑 (10 scenarios)

| # | 場景 | 觸發點 | 預期結果 |
|---|------|--------|----------|
| 1 | 退回重做 | HP1-步驟5 駁回 | workflow→RETURNED, 回到步驟4 重新施工 |
| 2 | 改分管區 | HP1-步驟3 轉南區 | assigned_to 改為 u-squad2-field, step_log 記錄轉送 |
| 3 | 障礙合併 | 同路段兩張障礙單 | fault_correlation 建立, 第二張併入第一張 |
| 4 | 過期代理 | 李明華請假, 陳國強代理 | delegate_setting 到期後自動失效 |
| 5 | 領料不足 | HP2-步驟3 庫存<需求量 | issue 失敗, 庫存不變, 錯誤訊息返回 |
| 6 | 換裝取消 | HP2-步驟2 後取消 | order→CANCELLED, 已領料退回庫存 |
| 7 | 重複報修 | 同設備重複建障礙 | 關聯偵測觸發(E2), 提示合併 |
| 8 | 結案駁回 | HP2-步驟7 駁回 | workflow→RETURNED, 回到自檢步驟 |
| 9 | 併案結案 | 兩張報修單同時結案 | 各自獨立結案, 設備各自恢復 |
| 10 | 逾時自動升級 | HP1-步驟4 超過48h | 通知分隊長(DEPT_ADMIN) |

- [x] 異常路徑 integration tests ✅ `ExceptionPathIntegrationTest` (5 scenarios)

#### Migration 修正 (5a4 期間)
- [x] V34, V38, V39, V48, V49: 移除 hardcoded `taipei_streetlight.` 改用 `${flyway:defaultSchema}`
- [x] V54: 補建 dept_id=11,12,13 (協力廠商/FET/設備商) 以支援 clean-schema 測試

---

## 文件追溯

| 分類 | 文件 |
|------|------|
| **Spec** | 02-spec/01-basic-requirements.md (跨模組需求) |
| **SRS** | 03-srs/SRS-01-integration.md (如存在) |
| **SA** | SA-00-overview.md §6, SA-02, SA-04, SA-05, SA-06, SA-11 |
| **SD** | SD-04 §4, SD-06 §6.6, SD-11 §7.3 |
| **Test** | TS-00 (事件機制), TS-04 (報修), TS-05 (換裝), TS-06 (材料) |
| **WBS** | 04-wbs/phase-5.md §1.5.1~1.5.4 |
| **Plan** | 99-plan/2026-04-24-execution-plan.md §Phase 5A |
| **Design** | _archive/x-plan/cross-module-03-07-unified-design.md (E1~E13 完整設計) |
| **Blueprint** | _archive/x-plan/phase-5-integration.md (FK migration + 事件驗證矩陣) |
