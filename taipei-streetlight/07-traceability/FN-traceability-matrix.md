# FN Traceability Matrix — 功能追溯矩陣

> **版本**：1.1  
> **日期**：2026-04-24  
> **用途**：從需求規格 → SA 功能 → SD 設計 → 程式碼 → 測試 → 驗證紀錄，全鏈貫穿追溯  
> **總計**：400 FN（10 模組 + 1 共用）  
> **最新驗證**：2026-04-24 · Commit `61b4ced` · 453 Tests / 0 Failures

---

## 追溯鏈結構

```
Spec(§)  →  SRS  →  SA(FN)  →  SD(API/DB)  →  Code(Class)  →  Test  →  VR(驗證紀錄)
 需求規格    需求文件    功能清單     系統設計      實作程式碼     測試      執行結果
```

## 驗證紀錄索引

| VR 文件 | 範圍 | 已驗 FN | 路徑 |
|---------|------|---------|------|
| VR-00-overview | 總覽 | 180/400 | [08-verification/VR-00-overview.md](../08-verification/VR-00-overview.md) |
| VR-P1-foundation | 共用+系統+簽核+資產 | 108/167 | [08-verification/VR-P1-foundation.md](../08-verification/VR-P1-foundation.md) |
| VR-P2-repair | 報修+巡查 | 25/47 | [08-verification/VR-P2-repair.md](../08-verification/VR-P2-repair.md) |
| VR-P3-material | 材料管理 | 10/45 | [08-verification/VR-P3-material.md](../08-verification/VR-P3-material.md) |
| VR-P4-replacement | 換裝維護 | 21/35 | [08-verification/VR-P4-replacement.md](../08-verification/VR-P4-replacement.md) |

**欄位說明**

| 欄位 | 說明 |
|------|------|
| FN | SA Function Number — 全鏈追溯 ID |
| 功能 | 功能名稱 |
| Spec | 對應需求規格章節 |
| SRS | 對應 SRS 編號 |
| SD | 對應 SD 文件 |
| API | 實作的 API endpoint |
| Code | 主要實作 Class (Controller/Service) |
| Test | 對應測試檔案 |
| Phase | 實作 Phase |
| 狀態 | ✅ 已實作+已測 / ⚠️ 已實作未測 / 🔲 未實作(設計中) |

---

## 統計總覽

| 模組 | FN 數 | ✅ 已實作+已測 | ⚠️ 已實作未測 | 🔲 未實作 |
|------|-------|--------------|-------------|---------|
| SA-00 共用 | 30 | 22 | 2 | 6 |
| SA-01 系統管理 | 55 | 30 | 12 | 13 |
| SA-02 簽核引擎 | 22 | 14 | 4 | 4 |
| SA-03 資產管理 | 60 | 22 | 6 | 32 |
| SA-04 報修維護 | 47 | 18 | 7 | 22 |
| SA-05 換裝維護 | 35 | 17 | 4 | 14 |
| SA-06 材料管理 | 45 | 10 | 16 | 19 |
| SA-07 智慧路燈 | 43 | 0 | 0 | 43 |
| SA-08 績效管理 | 20 | 0 | 0 | 20 |
| SA-09 行動 APP | 21 | 0 | 0 | 21 |
| SA-10 儀表板 | 22 | 0 | 0 | 22 |
| **合計** | **400** | **133** | **51** | **216** |

---

## SA-00 共用基礎 (30 FN)

| FN | 功能 | Spec | SRS | SD | API | Code | Test | Phase | 狀態 |
|----|------|------|-----|-----|-----|------|------|-------|------|
| FN-00-001 | JWT 核發 | §2-6 | SRS-01-001 | SD-11 | POST /v1/noauth/login | AuthService | AuthServiceTest | P1 | ✅ |
| FN-00-002 | JWT 驗證(Filter) | §2-6 | SRS-01-001 | SD-11 | (Filter) | JwtAuthenticationFilter | AuthServiceTest | P1 | ✅ |
| FN-00-003 | Token 刷新 | §2-6 | SRS-01-001 | SD-11 | POST /v1/noauth/token/refresh | AuthService | AuthServiceTest | P1 | ✅ |
| FN-00-004 | 登出 | §2-6 | SRS-01-001 | SD-11 | POST /v1/auth/logout | AuthController | AuthControllerTest | P1 | ✅ |
| FN-00-005 | 驗證碼產生 | §2-6 | SRS-01-001 | SD-11 | POST /v1/noauth/captcha | CaptchaService | CaptchaServiceTest | P1 | ✅ |
| FN-00-006 | 驗證碼驗證 | §2-6 | SRS-01-001 | SD-11 | (內部) | CaptchaService | CaptchaServiceTest | P1 | ✅ |
| FN-00-007 | 權限檢查(AOP) | §2-1 | SRS-01-002 | SD-11 | (AOP) | @PreAuthorize | SecurityContextUtilsTest | P1 | ✅ |
| FN-00-008 | 多租戶過濾 | §2-1 | SRS-01-002 | SD-11 | (JPA Filter) | TenantFilterAspect | TenantFilterAspectIntegrationTest | P1 | ✅ |
| FN-00-009 | 目前使用者取得 | §2-1 | SRS-01-002 | SD-11 | (內部) | SecurityContextUtils | SecurityContextUtilsTest | P1 | ✅ |
| FN-00-010 | 操作日誌紀錄(AOP) | §2-7 | SRS-01-004 | SD-11 | (AOP) | BaseLoggerAspect | BaseLoggerAspectTest | P1 | ✅ |
| FN-00-011 | 稽核日誌查詢 | §2-7 | SRS-01-004 | SD-11 | GET /v1/auth/audit/* | AuditController | AuditControllerTest | P1 | ✅ |
| FN-00-012 | 稽核日誌匯出 | §2-7 | SRS-01-004 | SD-11 | GET /v1/auth/audit/export | AuditService | AuditServiceTest | P1 | ✅ |
| FN-00-013 | 日誌自動清理 | §2-7 | SRS-01-004 | SD-11 | (排程) | AuditPurgeJob | AuditPurgeJobTest | P1 | ✅ |
| FN-00-014 | 站內通知發送 | §2-9 | SRS-01-005 | SD-11 | (內部) | — | — | — | 🔲 |
| FN-00-015 | 通知列表查詢 | §2-9 | SRS-01-005 | SD-11 | — | — | — | — | 🔲 |
| FN-00-016 | 標記已讀 | §2-9 | SRS-01-005 | SD-11 | — | — | — | — | 🔲 |
| FN-00-017 | WebSocket 即時推送 | §2-9 | SRS-01-005 | SD-11 | — | — | — | — | 🔲 |
| FN-00-018 | E-mail 通知 | §2-9 | SRS-01-005 | SD-11 | — | — | — | — | 🔲 |
| FN-00-019 | SMS 通知 | §2-9 | SRS-01-005 | SD-11 | — | — | — | — | 🔲 |
| FN-00-020 | 檔案上傳 | §5-3 | SRS-NFR-002 | SD-11 | POST /v1/auth/files/upload | FileStorageService | — | P2 | ⚠️ |
| FN-00-021 | 檔案下載 | §5-3 | SRS-NFR-002 | SD-11 | GET /v1/auth/files/{id}/download | FileStorageService | — | P2 | ⚠️ |
| FN-00-022 | 檔案預覽 | §5-3 | SRS-NFR-002 | SD-11 | — | — | — | — | 🔲 |
| FN-00-023 | 檔案刪除 | §5-3 | SRS-NFR-002 | SD-11 | — | — | — | — | 🔲 |
| FN-00-024 | CSV 匯出 | §1-3 | SRS-NFR-003 | SD-11 | (共用 Service) | DeviceExportService | DeviceExportServiceTest | P1 | ✅ |
| FN-00-025 | XLSX 匯出 | §1-3 | SRS-NFR-003 | SD-11 | (共用 Service) | DeviceExportService | DeviceExportServiceTest | P1 | ✅ |
| FN-00-026 | ODS 匯出 | §1-3 | SRS-NFR-003 | SD-11 | (共用 Service) | DeviceExportService | DeviceExportServiceTest | P1 | ✅ |
| FN-00-027 | Excel 匯入引擎 | §1-3 | SRS-NFR-003 | SD-11 | (共用 Service) | ApprovedMaterialService | — | P3 | ⚠️ |
| FN-00-028 | 事件發佈 | — | — | SD-11 | (內部) | ApplicationEventPublisher | ListenerTests | P1 | ✅ |
| FN-00-029 | 事件訂閱處理 | — | — | SD-11 | (內部) | @EventListener | ListenerTests | P1 | ✅ |
| FN-00-030 | 事件紀錄 | — | — | SD-11 | (內部) | AuditAsyncWriter | AuditAsyncWriterTest | P1 | ✅ |

---

## SA-01 系統管理 (55 FN)

| FN | 功能 | Spec | SRS | SD | API | Code | Test | Phase | 狀態 |
|----|------|------|-----|-----|-----|------|------|-------|------|
| FN-01-001 | 帳號列表查詢 | §2-1 | SRS-02-001 | SD-01 | GET /v1/auth/users | UserAdminController | UserAdminControllerTest | P1 | ✅ |
| FN-01-002 | 新增帳號 | §2-1 | SRS-02-001 | SD-01 | POST /v1/auth/users | UserAdminController | UserAdminControllerTest | P1 | ✅ |
| FN-01-003 | 編輯帳號 | §2-1 | SRS-02-001 | SD-01 | PUT /v1/auth/users/{id} | UserAdminController | UserAdminControllerTest | P1 | ✅ |
| FN-01-004 | 停用帳號 | §2-1 | SRS-02-001 | SD-01 | DELETE /v1/auth/users/{id} | UserAdminController | UserAdminControllerTest | P1 | ✅ |
| FN-01-005 | 復用帳號 | §2-1 | SRS-02-001 | SD-01 | — | — | — | — | 🔲 |
| FN-01-006 | 重設帳號密碼 | §2-1 | SRS-02-001 | SD-01 | — | — | — | — | 🔲 |
| FN-01-007 | 指派角色 | §2-1 | SRS-02-001 | SD-01 | POST /v1/auth/users/{id}/tenant-roles | UserAdminController | UserAdminControllerTest | P1 | ✅ |
| FN-01-008 | 查看帳號詳情 | §2-1 | SRS-02-001 | SD-01 | GET /v1/auth/users/{id}/tenant-roles | UserAdminController | UserAdminControllerTest | P1 | ✅ |
| FN-01-009 | 匯出帳號清單 | §2-1 | SRS-02-001 | SD-01 | — | — | — | — | 🔲 |
| FN-01-010 | 角色列表查詢 | §2-2 | SRS-02-002 | SD-01 | GET /v1/auth/roles | RoleController | RoleServiceTest | P1 | ⚠️ |
| FN-01-011 | 新增角色 | §2-2 | SRS-02-002 | SD-01 | POST /v1/auth/roles | RoleController | RoleServiceTest | P1 | ⚠️ |
| FN-01-012 | 編輯角色 | §2-2 | SRS-02-002 | SD-01 | PUT /v1/auth/roles/{id} | RoleController | RoleServiceTest | P1 | ⚠️ |
| FN-01-013 | 刪除角色 | §2-2 | SRS-02-002 | SD-01 | — | — | — | — | 🔲 |
| FN-01-014 | 指派角色權限 | §2-2 | SRS-02-002 | SD-01 | PUT /v1/auth/roles/{id}/permissions | RoleController | RoleServiceTest | P1 | ⚠️ |
| FN-01-015 | 查看角色權限 | §2-2 | SRS-02-002 | SD-01 | GET /v1/auth/roles/{id}/permissions | RoleController | RoleServiceTest | P1 | ⚠️ |
| FN-01-016 | 移動角色層級 | §2-2 | SRS-02-002 | SD-01 | — | — | — | — | 🔲 |
| FN-01-017 | 複製角色權限 | §2-2 | SRS-02-002 | SD-01 | — | — | — | — | 🔲 |
| FN-01-018 | 選單樹查詢 | §2-2 | SRS-02-002 | SD-01 | GET /v1/auth/menus/tree | MenuController | MenuControllerTest | P1 | ✅ |
| FN-01-019 | 新增選單 | §2-2 | SRS-02-002 | SD-01 | POST /v1/auth/menus | MenuController | MenuControllerTest | P1 | ✅ |
| FN-01-020 | 編輯選單 | §2-2 | SRS-02-002 | SD-01 | PUT /v1/auth/menus | MenuController | MenuControllerTest | P1 | ✅ |
| FN-01-021 | 刪除選單 | §2-2 | SRS-02-002 | SD-01 | DELETE /v1/auth/menus/{id} | MenuController | MenuControllerTest | P1 | ✅ |
| FN-01-022 | 取得驗證碼 | §2-3 | SRS-02-003 | SD-01 | POST /v1/noauth/captcha | AuthController | CaptchaServiceTest | P1 | ✅ |
| FN-01-023 | 帳密登入 | §2-3 | SRS-02-003 | SD-01 | POST /v1/noauth/login | AuthController | AuthControllerTest | P1 | ✅ |
| FN-01-024 | 臺北通 QRCode | §2-3 | SRS-02-003 | SD-01 | — | — | — | — | 🔲 |
| FN-01-025 | Taipeion 登入 | §2-3 | SRS-02-003 | SD-01 | — | — | — | — | 🔲 |
| FN-01-026 | Token 刷新 | §2-3 | SRS-02-003 | SD-01 | POST /v1/noauth/token/refresh | AuthController | AuthControllerTest | P1 | ✅ |
| FN-01-027 | 登出 | §2-3 | SRS-02-003 | SD-01 | POST /v1/auth/logout | AuthController | AuthControllerTest | P1 | ✅ |
| FN-01-028 | 密碼強度驗證 | §2-4 | SRS-02-004 | SD-01 | (內部) | UserAdminService | UserAdminServiceTest | P1 | ✅ |
| FN-01-029 | 變更密碼 | §2-4 | SRS-02-004 | SD-01 | POST /v1/auth/user/change-password | UserSelfController | UserSelfServiceTest | P1 | ✅ |
| FN-01-030 | 定期換密碼提醒 | §2-4 | SRS-02-004 | SD-01 | (登入時) | AuthService | — | P1 | ⚠️ |
| FN-01-031 | 申請密碼重設 | §2-5 | SRS-02-005 | SD-01 | POST /v1/noauth/user/forgot-password | AuthController | AuthControllerTest | P1 | ✅ |
| FN-01-032 | 驗證重設 Token | §2-5 | SRS-02-005 | SD-01 | — | — | — | — | 🔲 |
| FN-01-033 | 執行密碼重設 | §2-5 | SRS-02-005 | SD-01 | PUT /v1/noauth/user/reset-password | AuthController | AuthControllerTest | P1 | ✅ |
| FN-01-034 | 閒置偵測 | §2-6 | SRS-02-006 | SD-01 | (前端) | — | — | P1 | ⚠️ |
| FN-01-035 | 閒置時間設定 | §2-6 | SRS-02-006 | SD-01 | PUT /v1/auth/system-settings/idle-timeout | SystemSettingController | SystemSettingControllerTest | P1 | ✅ |
| FN-01-036 | 登入登出紀錄 | §2-7 | SRS-02-007 | SD-01 | GET /v1/auth/audit/user/login/my | AuditController | AuditControllerTest | P1 | ✅ |
| FN-01-037 | 紀錄匯出 | §2-7 | SRS-02-007 | SD-01 | — | — | — | — | 🔲 |
| FN-01-038 | 公告列表查詢 | §2-9 | SRS-02-009 | SD-01 | GET /v1/auth/announcements | AnnouncementController | — | P1 | ⚠️ |
| FN-01-039 | 新增公告 | §2-9 | SRS-02-009 | SD-01 | POST /v1/auth/announcements | AnnouncementController | — | P1 | ⚠️ |
| FN-01-040 | 編輯公告 | §2-9 | SRS-02-009 | SD-01 | PUT /v1/auth/announcements/{id} | AnnouncementController | — | P1 | ⚠️ |
| FN-01-041 | 發佈公告 | §2-9 | SRS-02-009 | SD-01 | — | — | — | — | 🔲 |
| FN-01-042 | 刪除公告 | §2-9 | SRS-02-009 | SD-01 | DELETE /v1/auth/announcements/{id} | AnnouncementController | — | P1 | ⚠️ |
| FN-01-043 | 公告欄 | §2-9 | SRS-02-009 | SD-01 | GET /v1/auth/announcements | AnnouncementController | — | P1 | ⚠️ |
| FN-01-044 | 標記公告已讀 | §2-9 | SRS-02-009 | SD-01 | POST /v1/auth/announcements/{id}/read | AnnouncementController | — | P1 | ⚠️ |
| FN-01-045 | 通知列表查詢 | §2-10 | SRS-02-010 | SD-01 | — | — | — | — | 🔲 |
| FN-01-046 | 未讀通知數量 | §2-10 | SRS-02-010 | SD-01 | GET /v1/auth/announcements/unread-count | AnnouncementController | — | P1 | ⚠️ |
| FN-01-047 | 標記通知已讀 | §2-10 | SRS-02-010 | SD-01 | POST /v1/auth/announcements/read-all | AnnouncementController | — | P1 | ⚠️ |
| FN-01-048 | 待辦案件列表 | §2-10 | SRS-02-010 | SD-01 | — | — | — | — | 🔲 |
| FN-01-049 | 即時通知推送 | §2-10 | SRS-02-010 | SD-01 | — | — | — | — | 🔲 |
| FN-01-050 | 部門樹查詢 | §2-1 | SRS-02-001 | SD-01 | GET /v1/auth/dept/list | DeptController | DeptControllerTest | P1 | ✅ |
| FN-01-051 | 新增部門 | §2-1 | SRS-02-001 | SD-01 | POST /v1/auth/dept | DeptController | DeptControllerTest | P1 | ✅ |
| FN-01-052 | 編輯部門 | §2-1 | SRS-02-001 | SD-01 | PUT /v1/auth/dept | DeptController | DeptControllerTest | P1 | ✅ |
| FN-01-053 | 刪除部門 | §2-1 | SRS-02-001 | SD-01 | DELETE /v1/auth/dept/{id} | DeptController | DeptControllerTest | P1 | ✅ |
| FN-01-054 | 查詢系統參數 | §2-6 | SRS-02-006 | SD-01 | GET /v1/auth/system-settings | SystemSettingController | SystemSettingControllerTest | P1 | ✅ |
| FN-01-055 | 更新系統參數 | §2-6 | SRS-02-006 | SD-01 | PUT /v1/auth/system-settings/{key} | SystemSettingController | SystemSettingControllerTest | P1 | ✅ |

---

## SA-02 簽核引擎 (22 FN)

| FN | 功能 | Spec | SRS | SD | API | Code | Test | Phase | 狀態 |
|----|------|------|-----|-----|-----|------|------|-------|------|
| FN-02-001 | 待辦案件列表 | §3-1 | SRS-03-001 | SD-02 | GET /v1/auth/workflow/pending | WorkflowController | WorkflowControllerTest | P1 | ✅ |
| FN-02-002 | 待辦案件數量 | §3-1 | SRS-03-001 | SD-02 | — | — | — | — | 🔲 |
| FN-02-003 | 流程歷程查詢 | §3-1 | SRS-03-001 | SD-02 | GET /v1/auth/workflow/{id}/logs | WorkflowController | WorkflowControllerTest | P1 | ✅ |
| FN-02-004 | 流程狀態查詢 | §3-1 | SRS-03-001 | SD-02 | — | WorkflowService | WorkflowServiceTest | P1 | ✅ |
| FN-02-005 | 送審 | §3-1 | SRS-03-001 | SD-02 | POST /v1/auth/workflow/{id}/transition | WorkflowController | WorkflowControllerTest | P1 | ✅ |
| FN-02-006 | 審核通過 | §3-1 | SRS-03-001 | SD-02 | POST /v1/auth/workflow/{id}/transition | WorkflowController | WorkflowServiceTest | P1 | ✅ |
| FN-02-007 | 退回補件 | §3-1 | SRS-03-001 | SD-02 | POST /v1/auth/workflow/{id}/transition | WorkflowController | WorkflowServiceTest | P1 | ✅ |
| FN-02-008 | 駁回 | §3-1 | SRS-03-001 | SD-02 | POST /v1/auth/workflow/{id}/transition | WorkflowController | WorkflowServiceTest | P1 | ✅ |
| FN-02-009 | 取消流程 | §3-1 | SRS-03-001 | SD-02 | POST /v1/auth/workflow/{id}/cancel | WorkflowController | WorkflowControllerTest | P1 | ✅ |
| FN-02-010 | 派工（簽核中） | §3-1 | SRS-03-001 | SD-02 | POST /v1/auth/workflow/{id}/transition | WorkflowService | WorkflowServiceTest | P1 | ✅ |
| FN-02-011 | FAULT_REVIEW 流程 | §3-1 | SRS-03-001 | SD-02 | (內部) | WorkflowService | WorkflowServiceTest | P1 | ✅ |
| FN-02-012 | REPAIR_DISPATCH 流程 | §3-1 | SRS-03-001 | SD-02 | (內部) | WorkflowService | WorkflowServiceTest | P2 | ✅ |
| FN-02-013 | REPAIR_CLOSE 流程 | §3-1 | SRS-03-001 | SD-02 | (內部) | WorkflowService | WorkflowServiceTest | P2 | ✅ |
| FN-02-014 | REPLACEMENT_REVIEW 流程 | §3-1 | SRS-03-001 | SD-02 | (內部) | WorkflowService | WorkflowServiceTest | P4 | ✅ |
| FN-02-015 | ASSET_CHANGE 流程 | §3-1 | SRS-03-001 | SD-02 | (內部) | — | — | — | 🔲 |
| FN-02-016 | 代理人列表 | §3-2 | SRS-03-002 | SD-02 | GET /v1/auth/delegates | DelegateController | DelegateControllerTest | P1 | ✅ |
| FN-02-017 | 新增代理人 | §3-2 | SRS-03-002 | SD-02 | POST /v1/auth/delegates | DelegateController | DelegateControllerTest | P1 | ✅ |
| FN-02-018 | 停用代理 | §3-2 | SRS-03-002 | SD-02 | DELETE /v1/auth/delegates/{id} | DelegateController | DelegateControllerTest | P1 | ✅ |
| FN-02-019 | 代理到期自動失效 | §3-2 | SRS-03-002 | SD-02 | (排程) | — | — | — | 🔲 |
| FN-02-020 | 代理簽核執行 | §3-2 | SRS-03-002 | SD-02 | (同 transition) | DelegateService | DelegateServiceTest | P1 | ⚠️ |
| FN-02-021 | 流程定義列表 | §3-1 | SRS-03-001 | SD-02 | — | — | — | — | 🔲 |
| FN-02-022 | 流程步驟模板查詢 | §3-1 | SRS-03-001 | SD-02 | — | — | — | — | 🔲 |

---

## SA-03 資產管理 (60 FN)

| FN | 功能 | Spec | SRS | SD | API | Code | Test | Phase | 狀態 |
|----|------|------|-----|-----|-----|------|------|-------|------|
| FN-03-001 | 設備列表查詢 | §4-3 | SRS-04-007 | SD-03 | GET /v1/auth/devices | DeviceController | DeviceControllerTest | P1 | ✅ |
| FN-03-002 | 新增設備 | §4-3 | SRS-04-007 | SD-03 | POST /v1/auth/devices | DeviceController | DeviceControllerTest | P1 | ✅ |
| FN-03-003 | 編輯設備 | §4-3 | SRS-04-007 | SD-03 | PUT /v1/auth/devices/{id} | DeviceController | DeviceControllerTest | P1 | ✅ |
| FN-03-004 | 查看設備詳情 | §4-3 | SRS-04-007 | SD-03 | GET /v1/auth/devices/{id} | DeviceController | DeviceControllerTest | P1 | ✅ |
| FN-03-005 | 刪除設備 | §4-3 | SRS-04-007 | SD-03 | DELETE /v1/auth/devices/{id} | DeviceController | DeviceControllerTest | P1 | ✅ |
| FN-03-006 | 設備統計概覽 | §4-3 | SRS-04-007 | SD-03 | — | — | — | — | 🔲 |
| FN-03-007 | 設備快速搜尋 | §4-3G | SRS-04-015 | SD-03 | — | — | — | — | 🔲 |
| FN-03-008 | JSONB 動態欄位 | §4-3 | SRS-04-007 | SD-03 | (含於 create/update) | DeviceService | DeviceServiceTest | P1 | ✅ |
| FN-03-009 | 設備歷程查詢 | §4-3A | SRS-04-008 | SD-03 | GET /v1/auth/devices/{id}/events | DeviceController | DeviceControllerTest | P1 | ✅ |
| FN-03-010 | 設備組件替換 | §4-3C | SRS-04-008 | SD-03 | POST /v1/auth/devices/{id}/components/replace | DeviceController | DeviceControllerTest | P1 | ✅ |
| FN-03-011 | 拓撲樹查詢 | §4-3 D-2 | SRS-04-009 | SD-03 | GET /v1/auth/devices/{id}/components | DeviceController | DeviceControllerTest | P1 | ✅ |
| FN-03-012 | 設定父設備 | §4-3 D-2 | SRS-04-009 | SD-03 | (含於 update) | DeviceService | DeviceServiceTest | P1 | ✅ |
| FN-03-013 | 設備地圖查詢 | §4-1A | SRS-04-001 | SD-03 §6 | GET /v1/gis/devices, /bounds, /nearby | GisController + GisService | GisControllerTest (13 TC) | P5C | ✅ |
| FN-03-014 | 坐標轉換 | §4-1B | SRS-04-002 | SD-03 | — | — | — | — | 🔲 |
| FN-03-015 | 街景連結 | §4-1C | SRS-04-003 | SD-03 | (前端) | — | — | — | 🔲 |
| FN-03-016 | 底圖切換 | §4-1D | SRS-04-004 | SD-03 §6 | (前端) | GisMapView.vue (NLSC+OSM) | — | P5C | ✅ |
| FN-03-017 | 分區範圍圖層 | §4-1D | SRS-04-004 | SD-03 | — | — | — | — | 🔲 |
| FN-03-018 | 管線圖層 | §4-1D | SRS-04-004 | SD-03 | — | — | — | — | 🔲 |
| FN-03-019 | GML 匯入 | §4-1F | SRS-04-005 | SD-03 | — | — | — | — | 🔲 |
| FN-03-020 | GML 匯出 | §4-1F | SRS-04-005 | SD-03 | — | — | — | — | 🔲 |
| FN-03-021 | 工務局管線匯入 | §4-1F | SRS-04-005 | SD-03 | — | — | — | — | 🔲 |
| FN-03-022 | 資料大平台匯出 | §4-1F | SRS-04-005 | SD-03 | — | — | — | — | 🔲 |
| FN-03-023 | CAD 匯出 | §4-1F | SRS-04-005 | SD-03 | — | — | — | — | 🔲 |
| FN-03-024 | 回路列表查詢 | §4-3 D-3 | SRS-04-010 | SD-03 | GET /v1/auth/circuits | CircuitController | CircuitControllerTest | P1 | ✅ |
| FN-03-025 | 新增回路 | §4-3 D-3 | SRS-04-010 | SD-03 | POST /v1/auth/circuits | CircuitController | CircuitControllerTest | P1 | ✅ |
| FN-03-026 | 編輯回路 | §4-3 D-3 | SRS-04-010 | SD-03 | PUT /v1/auth/circuits/{id} | CircuitController | CircuitControllerTest | P1 | ✅ |
| FN-03-027 | 刪除回路 | §4-3 D-3 | SRS-04-010 | SD-03 | DELETE /v1/auth/circuits/{id} | CircuitController | CircuitControllerTest | P1 | ✅ |
| FN-03-028 | 查看回路設備 | §4-3 D-3 | SRS-04-010 | SD-03 | — | — | — | — | 🔲 |
| FN-03-029 | 契約列表查詢 | §4-2A | SRS-04-006 | SD-03 | GET /v1/auth/contracts | ContractController | ContractControllerTest | P1 | ✅ |
| FN-03-030 | 新增契約 | §4-2A | SRS-04-006 | SD-03 | POST /v1/auth/contracts | ContractController | ContractControllerTest | P1 | ✅ |
| FN-03-031 | 編輯契約 | §4-2A | SRS-04-006 | SD-03 | PUT /v1/auth/contracts/{id} | ContractController | ContractControllerTest | P1 | ✅ |
| FN-03-032 | 刪除契約 | §4-2A | SRS-04-006 | SD-03 | DELETE /v1/auth/contracts/{id} | ContractController | ContractControllerTest | P1 | ✅ |
| FN-03-033 | 契約資產明細 | §4-2B | SRS-04-006 | SD-03 | — | — | — | — | 🔲 |
| FN-03-034 | 契約資產批次轉移 | §4-2D | SRS-04-006 | SD-03 | — | — | — | — | 🔲 |
| FN-03-035 | 保固到期提醒 | §4-2C | SRS-04-006 | SD-03 | — | — | — | — | 🔲 |
| FN-03-036 | 預防性維護排程 | §4-2C | SRS-04-006 | SD-03 | — | — | — | — | 🔲 |
| FN-03-037 | 維護排程提醒 | §4-2C | SRS-04-006 | SD-03 | — | — | — | — | 🔲 |
| FN-03-038 | 障礙工單列表 | §4-3 D-5 | SRS-04-011 | SD-03 | GET /v1/auth/faults | FaultTicketController | FaultTicketControllerTest | P1 | ✅ |
| FN-03-039 | 新增障礙工單 | §4-3 D-5 | SRS-04-011 | SD-03 | POST /v1/auth/faults | FaultTicketController | FaultTicketControllerTest | P1 | ✅ |
| FN-03-040 | 查看障礙詳情 | §4-3 D-5 | SRS-04-011 | SD-03 | GET /v1/auth/faults/{id} | FaultTicketController | FaultTicketControllerTest | P1 | ✅ |
| FN-03-041 | 解決障礙 | §4-3 D-5 | SRS-04-011 | SD-03 | POST /v1/auth/faults/{id}/resolve | FaultTicketController | FaultTicketControllerTest | P1 | ✅ |
| FN-03-042 | 關聯障礙列表 | §4-3 D-5 | SRS-04-011 | SD-03 | — | FaultCorrelationService | FaultCorrelationServiceTest | P1 | ⚠️ |
| FN-03-043 | 確認關聯障礙 | §4-3 D-5 | SRS-04-011 | SD-03 | — | FaultCorrelationService | FaultCorrelationServiceTest | P1 | ⚠️ |
| FN-03-044 | 解決關聯障礙 | §4-3 D-5 | SRS-04-011 | SD-03 | — | FaultCorrelationService | FaultCorrelationServiceTest | P1 | ⚠️ |
| FN-03-045 | 回路關聯偵測 | §4-3 D-5 | SRS-04-011 | SD-03 | (內部) | FaultCorrelationService | FaultCorrelationServiceTest | P1 | ✅ |
| FN-03-046 | Gateway 心跳偵測 | §4-3 D-5 | SRS-04-011 | SD-03 | — | — | — | — | 🔲 |
| FN-03-047 | SIM 到期預警 | §4-3 D-5 | SRS-04-011 | SD-03 | — | — | — | — | 🔲 |
| FN-03-048 | 資產加帳申請 | §4-4 | SRS-04-012 | SD-03 | — | — | — | — | 🔲 |
| FN-03-049 | 資產除帳申請 | §4-4 | SRS-04-012 | SD-03 | — | — | — | — | 🔲 |
| FN-03-050 | 資產變更申請 | §4-4 | SRS-04-012 | SD-03 | — | — | — | — | 🔲 |
| FN-03-051 | 批次異動申請 | §4-4 | SRS-04-012 | SD-03 | — | — | — | — | 🔲 |
| FN-03-052 | 異動申請列表 | §4-4 | SRS-04-012 | SD-03 | — | — | — | — | 🔲 |
| FN-03-053 | 資產匯出 CSV | §4-5 | SRS-04-013 | SD-03 | GET /v1/auth/devices/export?format=csv | DeviceController | DeviceExportServiceTest | P1 | ✅ |
| FN-03-054 | 資產匯出 XLSX | §4-5 | SRS-04-013 | SD-03 | GET /v1/auth/devices/export?format=xlsx | DeviceController | DeviceExportServiceTest | P1 | ✅ |
| FN-03-055 | 資產匯出 ODS | §4-5 | SRS-04-013 | SD-03 | GET /v1/auth/devices/export?format=ods | DeviceController | DeviceExportServiceTest | P1 | ✅ |
| FN-03-056 | 上傳驗收文件 | §4-6 | SRS-04-014 | SD-03 | — | — | — | — | 🔲 |
| FN-03-057 | 驗收文件列表 | §4-6 | SRS-04-014 | SD-03 | — | — | — | — | 🔲 |
| FN-03-058 | 下載驗收文件 | §4-6 | SRS-04-014 | SD-03 | — | — | — | — | 🔲 |
| FN-03-059 | 刪除驗收文件 | §4-6 | SRS-04-014 | SD-03 | — | — | — | — | 🔲 |
| FN-03-060 | 設備分類統計 | §4-5 | SRS-04-013 | SD-03 | — | — | — | — | 🔲 |

---

## SA-04 報修維護 (47 FN)

| FN | 功能 | Spec | SRS | SD | API | Code | Test | Phase | 狀態 |
|----|------|------|-----|-----|-----|------|------|-------|------|
| FN-04-001 | 民眾報修頁面 | §5-1 | SRS-05-001 | SD-04 | — | — | — | — | 🔲 |
| FN-04-002 | 提交民眾報修 | §5-1 | SRS-05-001 | SD-04 | — | — | — | — | 🔲 |
| FN-04-003 | 報修進度查詢 | §5-1 | SRS-05-001 | SD-04 | — | — | — | — | 🔲 |
| FN-04-004 | 1999 案件接收 | §5-2 | SRS-05-002 | SD-04 | — | — | — | — | 🔲 |
| FN-04-005 | 1999 結果回覆 | §5-2 | SRS-05-002 | SD-04 | — | — | — | — | 🔲 |
| FN-04-006 | 上傳附件 | §5-3 | SRS-05-003 | SD-04 | POST /v1/auth/repair/tickets/{id}/attachments | TicketAttachmentController | TicketAttachmentServiceTest | P2 | ✅ |
| FN-04-007 | 附件列表 | §5-3 | SRS-05-003 | SD-04 | GET /v1/auth/repair/tickets/{id}/attachments | TicketAttachmentController | TicketAttachmentServiceTest | P2 | ✅ |
| FN-04-008 | 下載附件 | §5-3 | SRS-05-003 | SD-04 | GET /v1/auth/repair/attachments/{id}/download | TicketAttachmentController | TicketAttachmentServiceTest | P2 | ✅ |
| FN-04-009 | 刪除附件 | §5-3 | SRS-05-003 | SD-04 | — | — | — | — | 🔲 |
| FN-04-010 | 報修工單列表 | §5-5 | SRS-05-005 | SD-04 | GET /v1/auth/repair/tickets | RepairTicketController | RepairTicketControllerTest | P2 | ✅ |
| FN-04-011 | 新增報修工單 | §5-4 | SRS-05-004 | SD-04 | POST /v1/auth/repair/tickets | RepairTicketController | RepairTicketControllerTest | P2 | ✅ |
| FN-04-012 | 查看工單詳情 | §5-8 | SRS-05-008 | SD-04 | GET /v1/auth/repair/tickets/{id} | RepairTicketController | RepairTicketControllerTest | P2 | ✅ |
| FN-04-013 | 收案 | §5-6 | SRS-05-006 | SD-04 | POST /v1/auth/repair/tickets/{id}/accept | RepairTicketController | RepairTicketControllerTest | P2 | ✅ |
| FN-04-014 | 派工 | §5-4 | SRS-05-004 | SD-04 | POST /v1/auth/repair/tickets/{id}/dispatch | RepairTicketController | RepairTicketControllerTest | P2 | ✅ |
| FN-04-015 | 開始處理 | §5-6 | SRS-05-006 | SD-04 | — | RepairTicketService | RepairTicketServiceTest | P2 | ⚠️ |
| FN-04-016 | 完工回報 | §5-4 | SRS-05-004 | SD-04 | POST /v1/auth/repair/tickets/{id}/complete | RepairTicketController | RepairTicketControllerTest | P2 | ✅ |
| FN-04-017 | 送審 | §5-11 | SRS-05-011 | SD-04 | — | — | — | — | 🔲 |
| FN-04-018 | 結案審核通過 | §5-12 | SRS-05-012 | SD-04 | (workflow) | RepairClosedListener | RepairClosedListenerTest | P2 | ✅ |
| FN-04-019 | 退回補件 | §5-11 | SRS-05-011 | SD-04 | (workflow) | WorkflowService | WorkflowServiceTest | P2 | ✅ |
| FN-04-020 | 改分轉送 | §5-6 | SRS-05-006 | SD-04 | POST /v1/auth/repair/tickets/{id}/transfer | RepairTicketController | RepairTicketControllerTest | P2 | ✅ |
| FN-04-021 | 匯出案件清冊 | §5-13 | SRS-05-013 | SD-04 | — | — | — | — | 🔲 |
| FN-04-022 | 報修資訊欄位 | §5-7 | SRS-05-007 | SD-04 | (含於 detail) | RepairTicketService | RepairTicketServiceTest | P2 | ✅ |
| FN-04-023 | 維護資訊欄位 | §5-7 | SRS-05-007 | SD-04 | (含於 detail) | RepairTicketService | RepairTicketServiceTest | P2 | ✅ |
| FN-04-024 | 設備維護履歷 | §5-9 | SRS-05-009 | SD-04 | — | — | — | — | 🔲 |
| FN-04-025 | 設備概覽 | §5-8 | SRS-05-008 | SD-04 | (組合查詢) | — | — | — | 🔲 |
| FN-04-026 | 通知機關報告單 | §5-10 | SRS-05-010 | SD-04 | — | — | — | — | 🔲 |
| FN-04-027 | 查報單 | §5-10 | SRS-05-010 | SD-04 | — | — | — | — | 🔲 |
| FN-04-028 | 設計單 | §5-10 | SRS-05-010 | SD-04 | — | — | — | — | 🔲 |
| FN-04-029 | 結案後更新圖資 | §5-10 | SRS-05-010 | SD-04 | (E9 事件) | RepairClosedListener | RepairClosedListenerTest | P2 | ✅ |
| FN-04-030 | 巡查任務列表 | §5-14 | SRS-05-014 | SD-04 | GET /v1/auth/inspection/tasks | InspectionController | InspectionControllerTest | P2 | ✅ |
| FN-04-031 | 新增巡查任務 | §5-14 | SRS-05-014 | SD-04 | POST /v1/auth/inspection/tasks | InspectionController | InspectionControllerTest | P2 | ✅ |
| FN-04-032 | 編輯巡查任務 | §5-14 | SRS-05-014 | SD-04 | PUT /v1/auth/inspection/tasks/{id} | InspectionController | InspectionControllerTest | P2 | ✅ |
| FN-04-033 | 停用巡查任務 | §5-14 | SRS-05-014 | SD-04 | DELETE /v1/auth/inspection/tasks/{id} | InspectionController | InspectionControllerTest | P2 | ✅ |
| FN-04-034 | 巡查紀錄列表 | §5-14 | SRS-05-014 | SD-04 | GET /v1/auth/inspection/tasks/{id}/records | InspectionController | InspectionControllerTest | P2 | ✅ |
| FN-04-035 | 新增巡查紀錄 | §5-14 | SRS-05-014 | SD-04 | POST /v1/auth/inspection/records | InspectionController | InspectionServiceTest | P2 | ✅ |
| FN-04-036 | 編輯巡查紀錄 | §5-14 | SRS-05-014 | SD-04 | — | — | — | — | 🔲 |
| FN-04-037 | 巡查派工 | §5-14 | SRS-05-014 | SD-04 | — | — | — | — | 🔲 |
| FN-04-038 | 非契約案件篩選 | §5-15 | SRS-05-015 | SD-04 | — | — | — | — | 🔲 |
| FN-04-039 | 開放資料匯出 | §5-16 | SRS-05-016 | SD-04 | — | — | — | — | 🔲 |
| FN-04-040 | 里長通知設定 | §5-17 | SRS-05-017 | SD-04 | — | — | — | — | 🔲 |
| FN-04-041 | 里內故障通知 | §5-17 | SRS-05-017 | SD-04 | — | — | — | — | 🔲 |
| FN-04-042 | 維護案件統計 | §5-13 | SRS-05-013 | SD-04 | — | — | — | — | 🔲 |
| FN-04-043 | 維護時間統計 | §5-13 | SRS-05-013 | SD-04 | — | — | — | — | 🔲 |
| FN-04-044 | 通報來源統計 | §5-13 | SRS-05-013 | SD-04 | — | — | — | — | 🔲 |
| FN-04-045 | 故障分類統計 | §5-13 | SRS-05-013 | SD-04 | — | — | — | — | 🔲 |
| FN-04-046 | 故障熱區統計 | §5-13 | SRS-05-013 | SD-04 | — | — | — | — | 🔲 |
| FN-04-047 | 材料換修統計 | §5-13 | SRS-05-013 | SD-04 | — | — | — | — | 🔲 |

---

## SA-05 換裝維護 (35 FN)

| FN | 功能 | Spec | SRS | SD | API | Code | Test | Phase | 狀態 |
|----|------|------|-----|-----|-----|------|------|-------|------|
| FN-05-001 | 號碼牌列表 | §6-1 | SRS-06-001 | SD-05 | GET /v1/auth/replacement/pole-numbers | LightPoleNumberController | LightPoleNumberControllerTest | P4 | ✅ |
| FN-05-002 | 產生號碼牌 | §6-1 | SRS-06-001 | SD-05 | POST /v1/auth/replacement/pole-numbers | LightPoleNumberController | LightPoleNumberControllerTest | P4 | ✅ |
| FN-05-003 | 重製 QR Code | §6-1 | SRS-06-001 | SD-05 | — | — | — | — | 🔲 |
| FN-05-004 | 刪除號碼牌 | §6-1 | SRS-06-001 | SD-05 | — | — | — | — | 🔲 |
| FN-05-005 | 批次匯入資產清冊 | §6-2 | SRS-06-002 | SD-05 | — | — | — | — | 🔲 |
| FN-05-006 | 匯入差異確認 | §6-2 | SRS-06-002 | SD-05 | — | — | — | — | 🔲 |
| FN-05-007 | 換裝工單列表 | §6-6 | SRS-06-006 | SD-05 | GET /v1/auth/replacement/orders | ReplacementOrderController | ReplacementOrderControllerTest | P4 | ✅ |
| FN-05-008 | 新增換裝工單 | §6-6 | SRS-06-006 | SD-05 | POST /v1/auth/replacement/orders | ReplacementOrderController | ReplacementOrderControllerTest | P4 | ✅ |
| FN-05-009 | 查看工單詳情 | §6-6 | SRS-06-006 | SD-05 | GET /v1/auth/replacement/orders/{id} | ReplacementOrderController | ReplacementOrderControllerTest | P4 | ✅ |
| FN-05-010 | 派工 | §6-6 | SRS-06-006 | SD-05 | POST /v1/auth/replacement/orders/{id}/dispatch | ReplacementOrderController | ReplacementOrderControllerTest | P4 | ✅ |
| FN-05-011 | 開工 | §6-6 | SRS-06-006 | SD-05 | POST /v1/auth/replacement/orders/{id}/start-work | ReplacementOrderController | ReplacementOrderControllerTest | P4 | ✅ |
| FN-05-012 | 案件類別管理 | §6-7 | SRS-06-007 | SD-05 | — | — | — | — | 🔲 |
| FN-05-013 | 預匯入路燈編號 | §6-8 | SRS-06-008 | SD-05 | — | — | — | — | 🔲 |
| FN-05-014 | 預匯入地圖查看 | §6-8 | SRS-06-008 | SD-05 | — | — | — | — | 🔲 |
| FN-05-015 | 新增換裝項目 | §6-5 | SRS-06-005 | SD-05 | POST /v1/auth/replacement/orders/{id}/items | ReplacementOrderController | ReplacementItemServiceTest | P4 | ✅ |
| FN-05-016 | 換裝項目列表 | §6-4 | SRS-06-005 | SD-05 | GET /v1/auth/replacement/orders/{id}/items | ReplacementOrderController | ReplacementItemServiceTest | P4 | ✅ |
| FN-05-017 | 編輯換裝項目 | §6-5 | SRS-06-005 | SD-05 | PUT /v1/auth/replacement/orders/{id}/items/{itemId} | ReplacementOrderController | ReplacementItemServiceTest | P4 | ✅ |
| FN-05-018 | 刪除換裝項目 | §6-5 | SRS-06-005 | SD-05 | DELETE /v1/auth/replacement/orders/{id}/items/{itemId} | ReplacementOrderController | ReplacementItemServiceTest | P4 | ✅ |
| FN-05-019 | 合格材料查驗 | §6-5 | SRS-06-005 | SD-05 | (內部) | ReplacementItemService | ReplacementItemServiceTest | P4 | ✅ |
| FN-05-020 | 材料清單批次匯入 | §6-4 | SRS-06-004 | SD-05 | — | — | — | — | 🔲 |
| FN-05-021 | 提交自主檢核 | §6-9 | SRS-06-009 | SD-05 | POST /v1/auth/replacement/orders/{id}/self-check | ReplacementOrderController | ReplacementOrderServiceTest | P4 | ✅ |
| FN-05-022 | 自檢後查看 | §6-9 | SRS-06-009 | SD-05 | (組合查詢) | — | — | P4 | ⚠️ |
| FN-05-023 | 組件替換(自檢) | §6-9 | SRS-06-009 | SD-05 | (含於 self-check) | ReplacementOrderService | ReplacementOrderServiceTest | P4 | ✅ |
| FN-05-024 | 報竣送審 | §6-10 | SRS-06-010 | SD-05 | POST /v1/auth/replacement/orders/{id}/submit-review | ReplacementOrderController | ReplacementOrderServiceTest | P4 | ✅ |
| FN-05-025 | 審核通過 | §6-10 | SRS-06-010 | SD-05 | POST /v1/auth/replacement/orders/{id}/approve | ReplacementOrderController | ReplacementOrderServiceTest | P4 | ✅ |
| FN-05-026 | 退回補件 | §6-10 | SRS-06-010 | SD-05 | POST /v1/auth/replacement/orders/{id}/return | ReplacementOrderController | ReplacementOrderServiceTest | P4 | ✅ |
| FN-05-027 | 補件重送 | §6-10 | SRS-06-010 | SD-05 | POST /v1/auth/replacement/orders/{id}/resubmit | ReplacementOrderController | ReplacementOrderServiceTest | P4 | ✅ |
| FN-05-028 | 結案後補救 | §6-10 | SRS-06-010 | SD-05 | — | — | — | — | 🔲 |
| FN-05-029 | 異動紀錄查詢 | §6-3 | SRS-06-003 | SD-05 | — | — | — | — | 🔲 |
| FN-05-030 | 派工案件統計 | §6-11 | SRS-06-011 | SD-05 | — | — | — | — | 🔲 |
| FN-05-031 | 交付/完成統計 | §6-11 | SRS-06-011 | SD-05 | — | — | — | — | 🔲 |
| FN-05-032 | 匯出派工清冊 | §6-12 | SRS-06-012 | SD-05 | — | — | — | — | 🔲 |
| FN-05-033 | 匯出異動地圖 | §6-12 | SRS-06-012 | SD-05 | — | — | — | — | 🔲 |
| FN-05-034 | 產生竣工清單 | §6-12 | SRS-06-012 | SD-05 | — | — | — | — | 🔲 |
| FN-05-035 | 產生用電申請表 | §6-12 | SRS-06-012 | SD-05 | — | — | — | — | 🔲 |

---

## SA-06 材料管理 (45 FN)

| FN | 功能 | Spec | SRS | SD | API | Code | Test | Phase | 狀態 |
|----|------|------|-----|-----|-----|------|------|-------|------|
| FN-06-001 | 倉庫列表 | §7-1A | SRS-07-001 | SD-06 | GET /v1/auth/material/warehouses | WarehouseController | WarehouseServiceTest | P3 | ✅ |
| FN-06-002 | 新增倉庫 | §7-1A | SRS-07-001 | SD-06 | POST /v1/auth/material/warehouses | WarehouseController | WarehouseServiceTest | P3 | ✅ |
| FN-06-003 | 編輯倉庫 | §7-1A | SRS-07-001 | SD-06 | PUT /v1/auth/material/warehouses/{id} | WarehouseController | WarehouseServiceTest | P3 | ✅ |
| FN-06-004 | 刪除倉庫 | §7-1A | SRS-07-001 | SD-06 | DELETE /v1/auth/material/warehouses/{id} | WarehouseController | WarehouseServiceTest | P3 | ✅ |
| FN-06-005 | 材料規格列表 | §7-1A | SRS-07-001 | SD-06 | GET /v1/auth/material/specs | MaterialSpecController | — | P3 | ⚠️ |
| FN-06-006 | 新增材料規格 | §7-1A | SRS-07-001 | SD-06 | POST /v1/auth/material/specs | MaterialSpecController | — | P3 | ⚠️ |
| FN-06-007 | 編輯材料規格 | §7-1A | SRS-07-001 | SD-06 | PUT /v1/auth/material/specs/{id} | MaterialSpecController | — | P3 | ⚠️ |
| FN-06-008 | 刪除材料規格 | §7-1A | SRS-07-001 | SD-06 | — | — | — | — | 🔲 |
| FN-06-009 | 供應商列表 | §7-1A | SRS-07-001 | SD-06 | GET /v1/auth/material/suppliers | SupplierController | — | P3 | ⚠️ |
| FN-06-010 | 新增供應商 | §7-1A | SRS-07-001 | SD-06 | POST /v1/auth/material/suppliers | SupplierController | — | P3 | ⚠️ |
| FN-06-011 | 編輯供應商 | §7-1A | SRS-07-001 | SD-06 | PUT /v1/auth/material/suppliers/{id} | SupplierController | — | P3 | ⚠️ |
| FN-06-012 | 刪除供應商 | §7-1A | SRS-07-001 | SD-06 | — | — | — | — | 🔲 |
| FN-06-013 | 合格材料列表 | §6-4 | SRS-07-001 | SD-06 | GET /v1/auth/material/approved-materials | ApprovedMaterialController | — | P3 | ⚠️ |
| FN-06-014 | 新增合格材料 | §6-4 | SRS-07-001 | SD-06 | POST /v1/auth/material/approved-materials | ApprovedMaterialController | — | P3 | ⚠️ |
| FN-06-015 | 批次匯入合格材料 | §6-4 | SRS-07-001 | SD-06 | POST /v1/auth/material/approved-materials/import | ApprovedMaterialController | — | P3 | ⚠️ |
| FN-06-016 | 停用合格材料 | §6-4 | SRS-07-001 | SD-06 | — | — | — | — | 🔲 |
| FN-06-017 | 採購單列表 | §7-1B | SRS-07-002 | SD-06 | GET /v1/auth/material/purchase-orders | PurchaseOrderController | PurchaseOrderServiceTest | P3 | ✅ |
| FN-06-018 | 新增採購單 | §7-1B | SRS-07-002 | SD-06 | POST /v1/auth/material/purchase-orders | PurchaseOrderController | PurchaseOrderServiceTest | P3 | ✅ |
| FN-06-019 | 編輯採購單 | §7-1B | SRS-07-002 | SD-06 | PUT /v1/auth/material/purchase-orders/{id} | PurchaseOrderController | PurchaseOrderServiceTest | P3 | ✅ |
| FN-06-020 | 送審採購單 | §7-1B | SRS-07-002 | SD-06 | POST /v1/auth/material/purchase-orders/{id}/submit | PurchaseOrderController | PurchaseOrderServiceTest | P3 | ✅ |
| FN-06-021 | 核准採購單 | §7-1B | SRS-07-002 | SD-06 | — | — | — | — | 🔲 |
| FN-06-022 | 完成採購 | §7-1B | SRS-07-002 | SD-06 | — | — | — | — | 🔲 |
| FN-06-023 | 新增收料紀錄 | §7-1C | SRS-07-003 | SD-06 | POST /v1/auth/material/receiving | ReceivingController | ReceivingServiceTest | P3 | ✅ |
| FN-06-024 | 收料紀錄列表 | §7-1C | SRS-07-003 | SD-06 | GET /v1/auth/material/receiving | ReceivingController | ReceivingServiceTest | P3 | ✅ |
| FN-06-025 | 轉庫作業 | §7-1C | SRS-07-003 | SD-06 | POST /v1/auth/material/adjustments/transfer | InventoryAdjustmentController | InventoryAdjustmentServiceTest | P3 | ✅ |
| FN-06-026 | 庫存總覽 | §7-1D | SRS-07-004 | SD-06 | GET /v1/auth/material/inventory | InventoryController | InventoryServiceTest | P3 | ✅ |
| FN-06-027 | 庫存盤點 | §7-1D | SRS-07-004 | SD-06 | POST /v1/auth/material/adjustments/count | InventoryAdjustmentController | InventoryAdjustmentServiceTest | P3 | ✅ |
| FN-06-028 | 庫存調整 | §7-1D | SRS-07-004 | SD-06 | POST /v1/auth/material/adjustments/correction | InventoryAdjustmentController | InventoryAdjustmentServiceTest | P3 | ✅ |
| FN-06-029 | 安全庫存設定 | §7-1D | SRS-07-004 | SD-06 | — | — | — | — | 🔲 |
| FN-06-030 | 安全庫存預警 | §7-1D | SRS-07-004 | SD-06 | (@Scheduled) | InventoryService | InventoryServiceTest | P3 | ✅ |
| FN-06-031 | 低庫存列表 | §7-1D | SRS-07-004 | SD-06 | GET /v1/auth/material/inventory/alerts | InventoryController | InventoryServiceTest | P3 | ✅ |
| FN-06-032 | 領料申請列表 | §7-1E | SRS-07-005 | SD-06 | GET /v1/auth/material/issue-requests | IssueController | IssueServiceTest | P3 | ✅ |
| FN-06-033 | 新增領料申請 | §7-1E | SRS-07-005 | SD-06 | POST /v1/auth/material/issue-requests | IssueController | IssueServiceTest | P3 | ✅ |
| FN-06-034 | 核准領料 | §7-1E | SRS-07-005 | SD-06 | POST /v1/auth/material/issue-requests/{id}/approve | IssueController | IssueServiceTest | P3 | ✅ |
| FN-06-035 | 確認出料 | §7-1E | SRS-07-005 | SD-06 | POST /v1/auth/material/issue-requests/{id}/issue | IssueController | IssueServiceTest | P3 | ✅ |
| FN-06-036 | 出料紀錄 | §7-1E | SRS-07-005 | SD-06 | — | — | — | — | 🔲 |
| FN-06-037 | 費用支出報表 | §7-1F | SRS-07-006 | SD-06 | — | — | — | — | 🔲 |
| FN-06-038 | 廢品處理 | §7-1F | SRS-07-006 | SD-06 | POST /v1/auth/material/disposals | DisposalController | — | P3 | ⚠️ |
| FN-06-039 | 廢品紀錄 | §7-1F | SRS-07-006 | SD-06 | GET /v1/auth/material/disposals | DisposalController | — | P3 | ⚠️ |
| FN-06-040 | 廠商材料規格 | §7-2 | SRS-07-007 | SD-06 | — | — | — | — | 🔲 |
| FN-06-041 | 廠商用量查詢 | §7-2 | SRS-07-007 | SD-06 | — | — | — | — | 🔲 |
| FN-06-042 | 廠商庫存查詢 | §7-2 | SRS-07-007 | SD-06 | — | — | — | — | 🔲 |
| FN-06-043 | 材料壽命分析 | §7-2 | SRS-07-007 | SD-06 | — | — | — | — | 🔲 |
| FN-06-044 | 材料品質分析 | §7-2 | SRS-07-007 | SD-06 | — | — | — | — | 🔲 |
| FN-06-045 | 備存需求分析 | §7-2 | SRS-07-007 | SD-06 | — | — | — | — | 🔲 |

---

## SA-07 智慧路燈 (43 FN) — 🔲 Phase 5 未實作

| FN | 功能 | Spec | SRS | SD | Phase | 狀態 |
|----|------|------|-----|-----|-------|------|
| FN-07-001 ~ FN-07-043 | IoT 數據串接 / 即時監控 / 告警 / 電表 / 調光 / 連線 / 示警 / 統計 / 隧道 | §8-1~§8-10 | SRS-08-001~010 | SD-07 | P5 | 🔲 全部 |

---

## SA-08 績效管理 (20 FN) — 🔲 Phase 6 未實作

| FN | 功能 | Spec | SRS | SD | Phase | 狀態 |
|----|------|------|-----|-----|-------|------|
| FN-08-001 ~ FN-08-020 | KPI 指標 / 數據匯入 / 自動計算 / 報表 / 期間鎖定 / 廠商端 | §9-1~§9-5 | SRS-09-001~005 | SD-08 | P6 | 🔲 全部 |

---

## SA-09 行動 APP (21 FN) — 🔲 Phase 7 未實作

| FN | 功能 | Spec | SRS | SD | Phase | 狀態 |
|----|------|------|-----|-----|-------|------|
| FN-09-001 ~ FN-09-021 | APP 登入 / 資產清查 / 巡查 / 拍照 / 離線同步 | §10-1~§10-5 | SRS-10-001~005 | SD-09 | P7 | 🔲 全部 |

---

## SA-10 儀表板 (22 FN) — 🔲 Phase 8 未實作

| FN | 功能 | Spec | SRS | SD | Phase | 狀態 |
|----|------|------|-----|-----|-------|------|
| FN-10-001 ~ FN-10-022 | 版面配置 / 各 Widget / GIS / WebSocket | §11-1~§11-10 | SRS-11-001~010 | SD-10 | P8 | 🔲 全部 |

---

## 附錄 A — 測試覆蓋缺口

### 已實作但完全無測試覆蓋的 Controller (6)

| Controller | 模組 | Endpoints | 影響 FN |
|------------|------|-----------|---------|
| PermissionController | rbac | 1 | FN-01-015 |
| AnnouncementController | announcement | 8 | FN-01-038~047 |
| SupplierController | material | 4 | FN-06-009~011 |
| MaterialSpecController | material | 4 | FN-06-005~007 |
| ApprovedMaterialController | material | 5 | FN-06-013~015 |
| DisposalController | material | 2 | FN-06-038~039 |

### 已實作但僅 Service 層測試（無 Controller 測試）的模組

| Controller | Service Test | 影響 FN |
|------------|-------------|---------|
| RoleController | RoleServiceTest | FN-01-010~015 |
| WarehouseController | WarehouseServiceTest | FN-06-001~004 |
| PurchaseOrderController | PurchaseOrderServiceTest | FN-06-017~020 |
| ReceivingController | ReceivingServiceTest | FN-06-023~024 |
| InventoryController | InventoryServiceTest | FN-06-026, 030~031 |
| InventoryAdjustmentController | InventoryAdjustmentServiceTest | FN-06-025, 027~028 |
| IssueController | IssueServiceTest | FN-06-032~035 |

---

## 附錄 B — 文件交叉索引

| 文件目錄 | 文件 | 用途 |
|---------|------|------|
| 01-requirement/ | 北市路燈平台需求.csv | 原始需求 |
| 02-spec/ | 01~11-*.md | 詳細規格書 (§1~§11) |
| 03-srs/ | SRS-01~SRS-11, SRS-NFR | 系統需求規格書 |
| 04-wbs/ | WBS-*.md | 工作分解結構 |
| 05-sa/ | SA-00~SA-10 | SA 功能清單 (本矩陣 FN 來源) |
| 06-sd/ | SD-00~SD-11 | 系統設計 (API/DB/Sequence) |
| **07-traceability/** | **FN-traceability-matrix.md** | **本文件 — 全鏈追溯矩陣** |
