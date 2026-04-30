# VR-P1 — Phase 1 基礎模組驗證紀錄

> **範圍**：SA-00 共用 + SA-01 系統管理 + SA-02 簽核引擎 + SA-03 資產管理  
> **FN 總數**：167 (30 + 55 + 22 + 60)

---

## 最新執行摘要

| 項目 | 值 |
|------|-----|
| 驗證日期 | 2026-04-24 |
| Git Commit | `61b4ced` |
| 涵蓋 Test Classes | 45 |
| 涵蓋 Test Cases | 370 |
| Failures | 0 |
| 已驗 FN | 108 / 167 |
| 未驗 FN（已實作） | 17 |
| 未實作 FN | 42 |

---

## SA-00 共用基礎 (30 FN)

### 測試對應

| Test Class | Tests | Result | 涵蓋 FN |
|-----------|-------|--------|---------|
| AuthServiceTest | 15 | ✅ PASS | FN-00-001, 002, 003 |
| AuthControllerTest | 20 | ✅ PASS | FN-00-004 |
| CaptchaServiceTest | 6 | ✅ PASS | FN-00-005, 006 |
| SecurityContextUtilsTest | 11 | ✅ PASS | FN-00-007, 009 |
| TenantContextTest | 3 | ✅ PASS | FN-00-008 |
| TenantEntityListenerTest | 2 | ✅ PASS | FN-00-008 |
| TenantInterceptorTest | 3 | ✅ PASS | FN-00-008 |
| BaseLoggerAspectTest | 4 | ✅ PASS | FN-00-010 |
| AuditControllerTest | 5 | ✅ PASS | FN-00-011 |
| AuditServiceTest | 6 | ✅ PASS | FN-00-012 |
| AuditPurgeJobTest | 2 | ✅ PASS | FN-00-013 |
| DeviceExportServiceTest | 6 | ✅ PASS | FN-00-024, 025, 026 |
| AuditAsyncWriterTest | 3 | ✅ PASS | FN-00-028, 029, 030 |
| PayloadSanitizerTest | 6 | ✅ PASS | (輔助工具) |

### FN 驗證明細

| FN | 功能 | 驗證方式 | 測試類別 | 結果 | 日期 | 備註 |
|----|------|---------|---------|------|------|------|
| FN-00-001 | JWT 核發 | Unit | AuthServiceTest | ✅ PASS | 04-24 | |
| FN-00-002 | JWT 驗證(Filter) | Unit | AuthServiceTest | ✅ PASS | 04-24 | |
| FN-00-003 | Token 刷新 | Unit | AuthServiceTest | ✅ PASS | 04-24 | |
| FN-00-004 | 登出 | API | AuthControllerTest | ✅ PASS | 04-24 | |
| FN-00-005 | 驗證碼產生 | Unit | CaptchaServiceTest | ✅ PASS | 04-24 | |
| FN-00-006 | 驗證碼驗證 | Unit | CaptchaServiceTest | ✅ PASS | 04-24 | |
| FN-00-007 | 權限檢查(AOP) | Unit | SecurityContextUtilsTest | ✅ PASS | 04-24 | |
| FN-00-008 | 多租戶過濾 | Unit | TenantContext/Entity/Interceptor | ✅ PASS | 04-24 | 3 test classes |
| FN-00-009 | 目前使用者取得 | Unit | SecurityContextUtilsTest | ✅ PASS | 04-24 | |
| FN-00-010 | 操作日誌紀錄 | Unit | BaseLoggerAspectTest | ✅ PASS | 04-24 | |
| FN-00-011 | 稽核日誌查詢 | API | AuditControllerTest | ✅ PASS | 04-24 | |
| FN-00-012 | 稽核日誌匯出 | Unit | AuditServiceTest | ✅ PASS | 04-24 | |
| FN-00-013 | 日誌自動清理 | Unit | AuditPurgeJobTest | ✅ PASS | 04-24 | |
| FN-00-014 | 站內通知發送 | — | — | 🔲 未實作 | — | Phase 5+ |
| FN-00-015 | 通知列表查詢 | — | — | 🔲 未實作 | — | Phase 5+ |
| FN-00-016 | 標記已讀 | — | — | 🔲 未實作 | — | Phase 5+ |
| FN-00-017 | WebSocket 推送 | — | — | 🔲 未實作 | — | Phase 5+ |
| FN-00-018 | E-mail 通知 | — | — | 🔲 未實作 | — | Phase 5+ |
| FN-00-019 | SMS 通知 | — | — | 🔲 未實作 | — | Phase 5+ |
| FN-00-020 | 檔案上傳 | 已實作 | — | ⚠️ 待補測 | — | FileStorageService 無測試 |
| FN-00-021 | 檔案下載 | 已實作 | — | ⚠️ 待補測 | — | FileStorageService 無測試 |
| FN-00-022 | 檔案預覽 | — | — | 🔲 未實作 | — | |
| FN-00-023 | 檔案刪除 | — | — | 🔲 未實作 | — | |
| FN-00-024 | CSV 匯出 | Unit | DeviceExportServiceTest | ✅ PASS | 04-24 | |
| FN-00-025 | XLSX 匯出 | Unit | DeviceExportServiceTest | ✅ PASS | 04-24 | |
| FN-00-026 | ODS 匯出 | Unit | DeviceExportServiceTest | ✅ PASS | 04-24 | |
| FN-00-027 | Excel 匯入引擎 | 已實作 | — | ⚠️ 待補測 | — | ApprovedMaterialService |
| FN-00-028 | 事件發佈 | Unit | AuditAsyncWriterTest | ✅ PASS | 04-24 | |
| FN-00-029 | 事件訂閱處理 | Unit | AuditAsyncWriterTest | ✅ PASS | 04-24 | |
| FN-00-030 | 事件紀錄 | Unit | AuditAsyncWriterTest | ✅ PASS | 04-24 | |

**小計**：✅ 22 / ⚠️ 3 / 🔲 5

---

## SA-01 系統管理 (55 FN)

### 測試對應

| Test Class | Tests | Result | 涵蓋 FN |
|-----------|-------|--------|---------|
| UserAdminControllerTest | 3 | ✅ PASS | FN-01-001~004 |
| UserAdminServiceTest | 16 | ✅ PASS | FN-01-002~004, 028 |
| UserSelfServiceTest | 4 | ✅ PASS | FN-01-029 |
| RoleServiceTest | 9 | ✅ PASS | FN-01-010~015 |
| MenuControllerTest | 3 | ✅ PASS | FN-01-018~021 |
| MenuServiceTest | 9 | ✅ PASS | FN-01-018~021 |
| AuthControllerTest | 20 | ✅ PASS | FN-01-022~027, 031, 033 |
| CaptchaServiceTest | 6 | ✅ PASS | FN-01-022 |
| DeptControllerTest | 10 | ✅ PASS | FN-01-050~053 |
| DeptServiceTest | 14 | ✅ PASS | FN-01-050~053 |
| DataPermissionAspectTest | 7 | ✅ PASS | (RBAC 輔助) |
| DataScopeHelperTest | 9 | ✅ PASS | (RBAC 輔助) |
| SystemSettingControllerTest | 9 | ✅ PASS | FN-01-035, 054, 055 |
| SystemSettingServiceTest | 7 | ✅ PASS | FN-01-054, 055 |
| AuditControllerTest | 5 | ✅ PASS | FN-01-036 |

### FN 驗證明細

| FN | 功能 | 驗證方式 | 測試類別 | 結果 | 日期 | 備註 |
|----|------|---------|---------|------|------|------|
| FN-01-001 | 帳號列表查詢 | API | UserAdminControllerTest | ✅ PASS | 04-24 | |
| FN-01-002 | 新增帳號 | API+Unit | UserAdmin Controller+Service | ✅ PASS | 04-24 | |
| FN-01-003 | 編輯帳號 | API+Unit | UserAdmin Controller+Service | ✅ PASS | 04-24 | |
| FN-01-004 | 停用帳號 | API+Unit | UserAdmin Controller+Service | ✅ PASS | 04-24 | |
| FN-01-005 | 復用帳號 | — | — | 🔲 未實作 | — | |
| FN-01-006 | 重設帳號密碼 | — | — | 🔲 未實作 | — | |
| FN-01-007 | 指派角色 | API | UserAdminControllerTest | ✅ PASS | 04-24 | |
| FN-01-008 | 查看帳號詳情 | API | UserAdminControllerTest | ✅ PASS | 04-24 | |
| FN-01-009 | 匯出帳號清單 | — | — | 🔲 未實作 | — | |
| FN-01-010 | 角色列表查詢 | Unit | RoleServiceTest | ⚠️ 缺API測 | 04-24 | 無 RoleControllerTest |
| FN-01-011 | 新增角色 | Unit | RoleServiceTest | ⚠️ 缺API測 | 04-24 | |
| FN-01-012 | 編輯角色 | Unit | RoleServiceTest | ⚠️ 缺API測 | 04-24 | |
| FN-01-013 | 刪除角色 | — | — | 🔲 未實作 | — | |
| FN-01-014 | 指派角色權限 | Unit | RoleServiceTest | ⚠️ 缺API測 | 04-24 | |
| FN-01-015 | 查看角色權限 | Unit | RoleServiceTest | ⚠️ 缺API測 | 04-24 | |
| FN-01-016 | 移動角色層級 | — | — | 🔲 未實作 | — | |
| FN-01-017 | 複製角色權限 | — | — | 🔲 未實作 | — | |
| FN-01-018 | 選單樹查詢 | API+Unit | Menu Controller+Service | ✅ PASS | 04-24 | |
| FN-01-019 | 新增選單 | API+Unit | Menu Controller+Service | ✅ PASS | 04-24 | |
| FN-01-020 | 編輯選單 | API+Unit | Menu Controller+Service | ✅ PASS | 04-24 | |
| FN-01-021 | 刪除選單 | API+Unit | Menu Controller+Service | ✅ PASS | 04-24 | |
| FN-01-022 | 取得驗證碼 | API | AuthControllerTest | ✅ PASS | 04-24 | |
| FN-01-023 | 帳密登入 | API | AuthControllerTest | ✅ PASS | 04-24 | |
| FN-01-024 | 臺北通 QRCode | — | — | 🔲 未實作 | — | |
| FN-01-025 | Taipeion 登入 | — | — | 🔲 未實作 | — | |
| FN-01-026 | Token 刷新 | API | AuthControllerTest | ✅ PASS | 04-24 | |
| FN-01-027 | 登出 | API | AuthControllerTest | ✅ PASS | 04-24 | |
| FN-01-028 | 密碼強度驗證 | Unit | UserAdminServiceTest | ✅ PASS | 04-24 | |
| FN-01-029 | 變更密碼 | Unit | UserSelfServiceTest | ✅ PASS | 04-24 | |
| FN-01-030 | 定期換密碼提醒 | 已實作 | — | ⚠️ 待補測 | — | 登入時檢查 |
| FN-01-031 | 申請密碼重設 | API | AuthControllerTest | ✅ PASS | 04-24 | |
| FN-01-032 | 驗證重設 Token | — | — | 🔲 未實作 | — | |
| FN-01-033 | 執行密碼重設 | API | AuthControllerTest | ✅ PASS | 04-24 | |
| FN-01-034 | 閒置偵測 | 已實作(前端) | — | ⚠️ 待補測 | — | 前端邏輯 |
| FN-01-035 | 閒置時間設定 | API | SystemSettingControllerTest | ✅ PASS | 04-24 | |
| FN-01-036 | 登入登出紀錄 | API | AuditControllerTest | ✅ PASS | 04-24 | |
| FN-01-037 | 紀錄匯出 | — | — | 🔲 未實作 | — | |
| FN-01-038 | 公告列表查詢 | 已實作 | — | ⚠️ 待補測 | — | 無 AnnouncementControllerTest |
| FN-01-039 | 新增公告 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-01-040 | 編輯公告 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-01-041 | 發佈公告 | — | — | 🔲 未實作 | — | |
| FN-01-042 | 刪除公告 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-01-043 | 公告欄 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-01-044 | 標記公告已讀 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-01-045 | 通知列表查詢 | — | — | 🔲 未實作 | — | |
| FN-01-046 | 未讀通知數量 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-01-047 | 標記通知已讀 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-01-048 | 待辦案件列表 | — | — | 🔲 未實作 | — | |
| FN-01-049 | 即時通知推送 | — | — | 🔲 未實作 | — | |
| FN-01-050 | 部門樹查詢 | API+Unit | Dept Controller+Service | ✅ PASS | 04-24 | |
| FN-01-051 | 新增部門 | API+Unit | Dept Controller+Service | ✅ PASS | 04-24 | |
| FN-01-052 | 編輯部門 | API+Unit | Dept Controller+Service | ✅ PASS | 04-24 | |
| FN-01-053 | 刪除部門 | API+Unit | Dept Controller+Service | ✅ PASS | 04-24 | |
| FN-01-054 | 查詢系統參數 | API+Unit | SystemSetting Controller+Service | ✅ PASS | 04-24 | |
| FN-01-055 | 更新系統參數 | API+Unit | SystemSetting Controller+Service | ✅ PASS | 04-24 | |

**小計**：✅ 30 / ⚠️ 12 / 🔲 13

---

## SA-02 簽核引擎 (22 FN)

### 測試對應

| Test Class | Tests | Result | 涵蓋 FN |
|-----------|-------|--------|---------|
| WorkflowControllerTest | 6 | ✅ PASS | FN-02-001, 003, 005, 009 |
| WorkflowServiceTest | 14 | ✅ PASS | FN-02-004~014 |
| DelegateControllerTest | 8 | ✅ PASS | FN-02-016~018 |
| DelegateServiceTest | 9 | ✅ PASS | FN-02-016~018, 020 |

### FN 驗證明細

| FN | 功能 | 驗證方式 | 測試類別 | 結果 | 日期 | 備註 |
|----|------|---------|---------|------|------|------|
| FN-02-001 | 待辦案件列表 | API | WorkflowControllerTest | ✅ PASS | 04-24 | |
| FN-02-002 | 待辦案件數量 | — | — | 🔲 未實作 | — | |
| FN-02-003 | 流程歷程查詢 | API | WorkflowControllerTest | ✅ PASS | 04-24 | |
| FN-02-004 | 流程狀態查詢 | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-005 | 送審 | API+Unit | Workflow Controller+Service | ✅ PASS | 04-24 | |
| FN-02-006 | 審核通過 | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-007 | 退回補件 | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-008 | 駁回 | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-009 | 取消流程 | API | WorkflowControllerTest | ✅ PASS | 04-24 | |
| FN-02-010 | 派工(簽核中) | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-011 | FAULT_REVIEW | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-012 | REPAIR_DISPATCH | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-013 | REPAIR_CLOSE | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-014 | REPLACEMENT_REVIEW | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | |
| FN-02-015 | ASSET_CHANGE | — | — | 🔲 未實作 | — | |
| FN-02-016 | 代理人列表 | API+Unit | Delegate Controller+Service | ✅ PASS | 04-24 | |
| FN-02-017 | 新增代理人 | API+Unit | Delegate Controller+Service | ✅ PASS | 04-24 | |
| FN-02-018 | 停用代理 | API+Unit | Delegate Controller+Service | ✅ PASS | 04-24 | |
| FN-02-019 | 代理到期失效 | — | — | 🔲 未實作 | — | 排程 |
| FN-02-020 | 代理簽核執行 | Unit | DelegateServiceTest | ⚠️ 缺API測 | 04-24 | |
| FN-02-021 | 流程定義列表 | — | — | 🔲 未實作 | — | |
| FN-02-022 | 流程步驟模板 | — | — | 🔲 未實作 | — | |

**小計**：✅ 14 / ⚠️ 1 / 🔲 7 (其中 4 未實作 + 3 設計中)

---

## SA-03 資產管理 (60 FN)

### 測試對應

| Test Class | Tests | Result | 涵蓋 FN |
|-----------|-------|--------|---------|
| DeviceControllerTest | 12 | ✅ PASS | FN-03-001~005, 009~011 |
| DeviceServiceTest | 14 | ✅ PASS | FN-03-008, 012 |
| CircuitControllerTest | 8 | ✅ PASS | FN-03-024~027 |
| CircuitServiceTest | 7 | ✅ PASS | FN-03-024~027 |
| ContractControllerTest | 7 | ✅ PASS | FN-03-029~032 |
| ContractServiceTest | 6 | ✅ PASS | FN-03-029~032 |
| FaultTicketControllerTest | 7 | ✅ PASS | FN-03-038~041 |
| FaultTicketServiceTest | 7 | ✅ PASS | FN-03-038~041 |
| FaultCorrelationServiceTest | 5 | ✅ PASS | FN-03-042~045 |
| DeviceExportServiceTest | 6 | ✅ PASS | FN-03-053~055 |

### FN 驗證明細

| FN | 功能 | 驗證方式 | 測試類別 | 結果 | 日期 | 備註 |
|----|------|---------|---------|------|------|------|
| FN-03-001 | 設備列表查詢 | API+Unit | Device Controller+Service | ✅ PASS | 04-24 | |
| FN-03-002 | 新增設備 | API+Unit | Device Controller+Service | ✅ PASS | 04-24 | |
| FN-03-003 | 編輯設備 | API+Unit | Device Controller+Service | ✅ PASS | 04-24 | |
| FN-03-004 | 查看設備詳情 | API | DeviceControllerTest | ✅ PASS | 04-24 | |
| FN-03-005 | 刪除設備 | API | DeviceControllerTest | ✅ PASS | 04-24 | |
| FN-03-006 | 設備統計概覽 | — | — | 🔲 未實作 | — | |
| FN-03-007 | 設備快速搜尋 | — | — | 🔲 未實作 | — | |
| FN-03-008 | JSONB 動態欄位 | Unit | DeviceServiceTest | ✅ PASS | 04-24 | |
| FN-03-009 | 設備歷程查詢 | API | DeviceControllerTest | ✅ PASS | 04-24 | |
| FN-03-010 | 設備組件替換 | API | DeviceControllerTest | ✅ PASS | 04-24 | |
| FN-03-011 | 拓撲樹查詢 | API | DeviceControllerTest | ✅ PASS | 04-24 | |
| FN-03-012 | 設定父設備 | Unit | DeviceServiceTest | ✅ PASS | 04-24 | |
| FN-03-013 | 設備地圖查詢 | API (13 TC) | GisControllerTest | ✅ PASS | 04-24 | GIS, Phase 5C |
| FN-03-014 | 坐標轉換 | — | — | 🔲 未實作 | — | GIS |
| FN-03-015 | 街景連結 | — | — | 🔲 未實作 | — | 前端 |
| FN-03-016 | 底圖切換 | 前端 | GisMapView.vue | ✅ 已實作 | 04-24 | NLSC WMTS+OSM fallback |
| FN-03-017 | 分區範圍圖層 | — | — | 🔲 未實作 | — | GIS |
| FN-03-018 | 管線圖層 | — | — | 🔲 未實作 | — | GIS |
| FN-03-019 | GML 匯入 | — | — | 🔲 未實作 | — | |
| FN-03-020 | GML 匯出 | — | — | 🔲 未實作 | — | |
| FN-03-021 | 工務局管線匯入 | — | — | 🔲 未實作 | — | |
| FN-03-022 | 資料大平台匯出 | — | — | 🔲 未實作 | — | |
| FN-03-023 | CAD 匯出 | — | — | 🔲 未實作 | — | |
| FN-03-024 | 回路列表查詢 | API+Unit | Circuit Controller+Service | ✅ PASS | 04-24 | |
| FN-03-025 | 新增回路 | API+Unit | Circuit Controller+Service | ✅ PASS | 04-24 | |
| FN-03-026 | 編輯回路 | API+Unit | Circuit Controller+Service | ✅ PASS | 04-24 | |
| FN-03-027 | 刪除回路 | API+Unit | Circuit Controller+Service | ✅ PASS | 04-24 | |
| FN-03-028 | 查看回路設備 | — | — | 🔲 未實作 | — | |
| FN-03-029 | 契約列表查詢 | API+Unit | Contract Controller+Service | ✅ PASS | 04-24 | |
| FN-03-030 | 新增契約 | API+Unit | Contract Controller+Service | ✅ PASS | 04-24 | |
| FN-03-031 | 編輯契約 | API+Unit | Contract Controller+Service | ✅ PASS | 04-24 | |
| FN-03-032 | 刪除契約 | API+Unit | Contract Controller+Service | ✅ PASS | 04-24 | |
| FN-03-033 | 契約資產明細 | — | — | 🔲 未實作 | — | |
| FN-03-034 | 契約資產批次轉移 | — | — | 🔲 未實作 | — | |
| FN-03-035 | 保固到期提醒 | — | — | 🔲 未實作 | — | |
| FN-03-036 | 預防性維護排程 | — | — | 🔲 未實作 | — | |
| FN-03-037 | 維護排程提醒 | — | — | 🔲 未實作 | — | |
| FN-03-038 | 障礙工單列表 | API+Unit | FaultTicket Controller+Service | ✅ PASS | 04-24 | |
| FN-03-039 | 新增障礙工單 | API+Unit | FaultTicket Controller+Service | ✅ PASS | 04-24 | |
| FN-03-040 | 查看障礙詳情 | API+Unit | FaultTicket Controller+Service | ✅ PASS | 04-24 | |
| FN-03-041 | 解決障礙 | API+Unit | FaultTicket Controller+Service | ✅ PASS | 04-24 | |
| FN-03-042 | 關聯障礙列表 | Unit | FaultCorrelationServiceTest | ⚠️ 缺API測 | 04-24 | |
| FN-03-043 | 確認關聯障礙 | Unit | FaultCorrelationServiceTest | ⚠️ 缺API測 | 04-24 | |
| FN-03-044 | 解決關聯障礙 | Unit | FaultCorrelationServiceTest | ⚠️ 缺API測 | 04-24 | |
| FN-03-045 | 回路關聯偵測 | Unit | FaultCorrelationServiceTest | ✅ PASS | 04-24 | |
| FN-03-046 | Gateway 心跳偵測 | — | — | 🔲 未實作 | — | |
| FN-03-047 | SIM 到期預警 | — | — | 🔲 未實作 | — | |
| FN-03-048 | 資產加帳申請 | — | — | 🔲 未實作 | — | |
| FN-03-049 | 資產除帳申請 | — | — | 🔲 未實作 | — | |
| FN-03-050 | 資產變更申請 | — | — | 🔲 未實作 | — | |
| FN-03-051 | 批次異動申請 | — | — | 🔲 未實作 | — | |
| FN-03-052 | 異動申請列表 | — | — | 🔲 未實作 | — | |
| FN-03-053 | 資產匯出 CSV | Unit | DeviceExportServiceTest | ✅ PASS | 04-24 | |
| FN-03-054 | 資產匯出 XLSX | Unit | DeviceExportServiceTest | ✅ PASS | 04-24 | |
| FN-03-055 | 資產匯出 ODS | Unit | DeviceExportServiceTest | ✅ PASS | 04-24 | |
| FN-03-056 | 上傳驗收文件 | — | — | 🔲 未實作 | — | |
| FN-03-057 | 驗收文件列表 | — | — | 🔲 未實作 | — | |
| FN-03-058 | 下載驗收文件 | — | — | 🔲 未實作 | — | |
| FN-03-059 | 刪除驗收文件 | — | — | 🔲 未實作 | — | |
| FN-03-060 | 設備分類統計 | — | — | 🔲 未實作 | — | |

**小計**：✅ 22 / ⚠️ 3 / 🔲 35 (其中 3 已實作待測 + 32 未實作)

---

## P1 總結

| 模組 | ✅ 已驗 | ⚠️ 待補測 | 🔲 未實作 | 小計 |
|------|---------|----------|---------|------|
| SA-00 共用 | 22 | 3 | 5 | 30 |
| SA-01 系統管理 | 30 | 12 | 13 | 55 |
| SA-02 簽核引擎 | 14 | 1 | 7 | 22 |
| SA-03 資產管理 | 22 | 3 | 35 | 60 |
| **P1 合計** | **88** | **19** | **60** | **167** |

### 下一步行動

1. **補 AnnouncementControllerTest** — 一次解決 FN-01-038~047 共 8 個 ⚠️
2. **補 RoleControllerTest** — 解決 FN-01-010~015 的 API 層測試
3. **補 FileStorageServiceTest** — 解決 FN-00-020~021
