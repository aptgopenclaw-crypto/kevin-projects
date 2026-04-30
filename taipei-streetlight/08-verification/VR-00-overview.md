# VR-00 — 驗證總覽 (Verification Overview)

> **用途**：追蹤各 Phase 的測試驗證進度與最新執行狀態  
> **更新規則**：每次 `mvn test` 後更新本文件

---

## 最新執行狀態

| 項目 | 值 |
|------|-----|
| **驗證日期** | 2026-04-24 12:29 |
| **Git Commit** | `61b4ced` |
| **Maven Test 總計** | 453 Tests, 0 Failures, 0 Errors, 0 Skipped |
| **Test Classes** | 66 |
| **BUILD** | ✅ SUCCESS |

---

## Phase 驗證進度

| Phase | 模組 | VR 文件 | 涵蓋 FN | 已驗 FN | 未驗 FN | 通過率 |
|-------|------|---------|---------|---------|---------|--------|
| P1 | 共用 + 系統 + 資產 + 簽核 | [VR-P1](VR-P1-foundation.md) | 167 | 108 | 59 | 64.7% |
| P2 | 報修 + 巡查 | [VR-P2](VR-P2-repair.md) | 47 | 25 | 22 | 53.2% |
| P3 | 材料管理 | [VR-P3](VR-P3-material.md) | 45 | 26 | 19 | 57.8% |
| P4 | 換裝維護 | [VR-P4](VR-P4-replacement.md) | 35 | 21 | 14 | 60.0% |
| P5 | 智慧路燈 | — | 43 | 0 | 43 | 0% |
| P6 | 績效管理 | — | 20 | 0 | 20 | 0% |
| P7 | 行動 APP | — | 21 | 0 | 21 | 0% |
| P8 | 儀表板 | — | 22 | 0 | 22 | 0% |
| **合計** | | | **400** | **180** | **220** | **45.0%** |

> **說明**：「已驗 FN」= 有對應測試且 PASS 的 FN，「未驗 FN」= 無測試 or 尚未實作

---

## 測試類別清單 (66 classes, 453 tests)

### 共用基礎 (audit / auth / common / tenant)

| # | Test Class | Tests | Pass | Fail | 歸屬 |
|---|-----------|-------|------|------|------|
| 1 | ApplicationTests | 1 | 1 | 0 | 基礎 |
| 2 | BaseLoggerAspectTest | 4 | 4 | 0 | SA-00 |
| 3 | AuditAsyncWriterTest | 3 | 3 | 0 | SA-00 |
| 4 | AuditControllerTest | 5 | 5 | 0 | SA-00 |
| 5 | AuditPurgeJobTest | 2 | 2 | 0 | SA-00 |
| 6 | AuditServiceTest | 6 | 6 | 0 | SA-00 |
| 7 | PayloadSanitizerTest | 6 | 6 | 0 | SA-00 |
| 8 | AuthControllerTest | 20 | 20 | 0 | SA-00/01 |
| 9 | AuthServiceTest | 15 | 15 | 0 | SA-00/01 |
| 10 | CaptchaServiceTest | 6 | 6 | 0 | SA-00/01 |
| 11 | UserInfoTest | 7 | 7 | 0 | 共用 |
| 12 | ErrorCodeTest | 4 | 4 | 0 | 共用 |
| 13 | BusinessExceptionTest | 4 | 4 | 0 | 共用 |
| 14 | GlobalExceptionHandlerTest | 14 | 14 | 0 | 共用 |
| 15 | BaseResponseTest | 10 | 10 | 0 | 共用 |
| 16 | SecurityContextUtilsTest | 11 | 11 | 0 | SA-00 |
| 17 | TenantContextTest | 3 | 3 | 0 | SA-00 |
| 18 | TenantEntityListenerTest | 2 | 2 | 0 | SA-00 |
| 19 | TenantInterceptorTest | 3 | 3 | 0 | SA-00 |

### 系統管理 (dept / rbac / user / setting / announcement)

| # | Test Class | Tests | Pass | Fail | 歸屬 |
|---|-----------|-------|------|------|------|
| 20 | DataPermissionAspectTest | 7 | 7 | 0 | SA-01 |
| 21 | DeptControllerTest | 10 | 10 | 0 | SA-01 |
| 22 | DataScopeHelperTest | 9 | 9 | 0 | SA-01 |
| 23 | DeptServiceTest | 14 | 14 | 0 | SA-01 |
| 24 | MenuControllerTest | 3 | 3 | 0 | SA-01 |
| 25 | MenuServiceTest | 9 | 9 | 0 | SA-01 |
| 26 | RoleServiceTest | 9 | 9 | 0 | SA-01 |
| 27 | UserAdminControllerTest | 3 | 3 | 0 | SA-01 |
| 28 | UserAdminServiceTest | 16 | 16 | 0 | SA-01 |
| 29 | UserSelfServiceTest | 4 | 4 | 0 | SA-01 |
| 30 | SystemSettingControllerTest | 9 | 9 | 0 | SA-01 |
| 31 | SystemSettingServiceTest | 7 | 7 | 0 | SA-01 |

### 資產管理 (device / circuit / contract / fault)

| # | Test Class | Tests | Pass | Fail | 歸屬 |
|---|-----------|-------|------|------|------|
| 32 | CircuitControllerTest | 8 | 8 | 0 | SA-03 |
| 33 | ContractControllerTest | 7 | 7 | 0 | SA-03 |
| 34 | DeviceControllerTest | 12 | 12 | 0 | SA-03 |
| 35 | CircuitServiceTest | 7 | 7 | 0 | SA-03 |
| 36 | ContractServiceTest | 6 | 6 | 0 | SA-03 |
| 37 | DeviceExportServiceTest | 6 | 6 | 0 | SA-03 |
| 38 | DeviceServiceTest | 14 | 14 | 0 | SA-03 |
| 39 | FaultTicketControllerTest | 7 | 7 | 0 | SA-03 |
| 40 | FaultCorrelationServiceTest | 5 | 5 | 0 | SA-03 |
| 41 | FaultTicketServiceTest | 7 | 7 | 0 | SA-03 |

### 簽核引擎 (workflow / delegate)

| # | Test Class | Tests | Pass | Fail | 歸屬 |
|---|-----------|-------|------|------|------|
| 42 | DelegateControllerTest | 8 | 8 | 0 | SA-02 |
| 43 | WorkflowControllerTest | 6 | 6 | 0 | SA-02 |
| 44 | DelegateServiceTest | 9 | 9 | 0 | SA-02 |
| 45 | WorkflowServiceTest | 14 | 14 | 0 | SA-02 |

### 報修維護 (repair / inspection)

| # | Test Class | Tests | Pass | Fail | 歸屬 |
|---|-----------|-------|------|------|------|
| 46 | InspectionControllerTest | 6 | 6 | 0 | SA-04 |
| 47 | RepairTicketControllerTest | 10 | 10 | 0 | SA-04 |
| 48 | FaultApprovedListenerTest | 4 | 4 | 0 | SA-04 |
| 49 | RepairClosedListenerTest | 3 | 3 | 0 | SA-04 |
| 50 | InspectionServiceTest | 6 | 6 | 0 | SA-04 |
| 51 | RepairDispatchServiceTest | 5 | 5 | 0 | SA-04 |
| 52 | RepairTicketServiceTest | 10 | 10 | 0 | SA-04 |
| 53 | TicketAttachmentServiceTest | 4 | 4 | 0 | SA-04 |

### 材料管理 (material)

| # | Test Class | Tests | Pass | Fail | 歸屬 |
|---|-----------|-------|------|------|------|
| 54 | InventoryAdjustmentServiceTest | 4 | 4 | 0 | SA-06 |
| 55 | InventoryServiceTest | 4 | 4 | 0 | SA-06 |
| 56 | IssueServiceTest | 6 | 6 | 0 | SA-06 |
| 57 | PurchaseOrderServiceTest | 5 | 5 | 0 | SA-06 |
| 58 | ReceivingServiceTest | 2 | 2 | 0 | SA-06 |
| 59 | WarehouseServiceTest | 5 | 5 | 0 | SA-06 |

### 換裝維護 (replacement)

| # | Test Class | Tests | Pass | Fail | 歸屬 |
|---|-----------|-------|------|------|------|
| 60 | LightPoleNumberControllerTest | 3 | 3 | 0 | SA-05 |
| 61 | ReplacementOrderControllerTest | 9 | 9 | 0 | SA-05 |
| 62 | ReplacementClosedListenerTest | 2 | 2 | 0 | SA-05 |
| 63 | ReplacementNeedMaterialListenerTest | 2 | 2 | 0 | SA-05 |
| 64 | LightPoleNumberServiceTest | 2 | 2 | 0 | SA-05 |
| 65 | ReplacementItemServiceTest | 8 | 8 | 0 | SA-05 |
| 66 | ReplacementOrderServiceTest | 11 | 11 | 0 | SA-05 |

---

## 歷史執行紀錄

| 日期 | Commit | Tests | Failures | 備註 |
|------|--------|-------|----------|------|
| 2026-04-24 | `61b4ced` | 453 | 0 | 基線建立：Phase 1-4 完成，全部 PASS |

---

## 測試覆蓋缺口（待補）

### 完全無測試的 Controller (6)

| Controller | 模組 | 影響 FN | 優先序 |
|------------|------|---------|--------|
| AnnouncementController | 公告 | FN-01-038~047 | P1 |
| PermissionController | RBAC | FN-01-015 | P2 |
| SupplierController | 材料 | FN-06-009~011 | P3 |
| MaterialSpecController | 材料 | FN-06-005~007 | P3 |
| ApprovedMaterialController | 材料 | FN-06-013~015 | P3 |
| DisposalController | 材料 | FN-06-038~039 | P3 |

### 僅 Service 測試（缺 Controller 測試）

| Controller | 有 Service Test | 影響 FN |
|------------|----------------|---------|
| RoleController | RoleServiceTest (9) | FN-01-010~015 |
| WarehouseController | WarehouseServiceTest (5) | FN-06-001~004 |
| PurchaseOrderController | PurchaseOrderServiceTest (5) | FN-06-017~020 |
| ReceivingController | ReceivingServiceTest (2) | FN-06-023~024 |
| InventoryController | InventoryServiceTest (4) | FN-06-026, 030~031 |
| InventoryAdjustmentController | InventoryAdjustmentServiceTest (4) | FN-06-025, 027~028 |
| IssueController | IssueServiceTest (6) | FN-06-032~035 |
