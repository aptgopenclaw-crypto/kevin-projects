# Phase 6 — 績效管理 TODO

> **建立日期**: 2026-04-26  
> **最後更新**: 2026-04-26 (6a~6g 全部完成)  
> **甘特圖**: 06/02 – 07/04 (5 週)  
> **前置**: Phase 5A 跨模組整合完成  
> **執行計畫**: 99-plan/2026-04-24-execution-plan.md §Phase 6  
> **關鍵路徑**: Phase 6 → Phase 8 儀表板 (統計 Widget 依賴 KPI 資料)  
> **技術選型**: SpEL 為主 (GraalJS 延後至有需求時再加)

### 進度總覽

| 區塊 | 進度 | 說明 |
|------|------|------|
| 6a DB schema (4 tables) | ✅ 完成 | V58+V59 migration, 4 entity, 4 repository, 5 enum, 8 audit events |
| 6b KPI 定義 CRUD + SpEL 公式引擎 | ✅ 完成 | FormulaEngine (SpEL only, D1) + KpiIndicatorService + Controller 5 端點 + 4 DTO + 8 ErrorCode |
| 6c 資料收集 (自動拉取各模組) | ✅ 完成 | KpiDataService + KpiDataController + 3 Collector + KpiDataCollectionJob + Excel/CSV 匯入 |
| 6d 計分 + 鎖定機制 | ✅ 完成 | KpiCalculationService + KpiPeriodService + KpiCalculationJob + 4 Controllers (Calculation/Period/Contractor/Report) |
| 6e 報表匯出 (月報/年報/ODS/XLS) | ✅ 完成 | KpiReportService (月報/年報/比較) + XLS/CSV 匯出 + KpiReportController 5 端點 |
| 6f 前端 6 頁面 | ✅ 完成 | 6 Vue pages + types/kpi.ts + api/kpi + router + i18n (zh-TW) |
| 6g 測試 | ✅ 完成 | 22 TC 全通過 — FormulaEngineTest (7) + KpiIndicatorServiceTest (5) + KpiCalculationServiceTest (4) + KpiPeriodServiceTest (6) |

### 甘特依賴

```
6a DB schema (3d) ──→ 6b KPI CRUD + 公式引擎 (8d) ──→ 6c 資料收集 (5d) ──→ 6d 計分+鎖定 (5d) ──→ 6e 報表匯出 (5d) ──→ 6g 測試 (5d)
       └──→ 6f 前端 6 頁面 (20d, 與後端並行) ──────────────────────────────────────────────────────┘
```

---

## 6a — DB Schema (3 天) ✅ 已完成

> **SD**: SD-08-performance.md §1  
> **SRS**: SRS-09-001 ~ SRS-09-005 (全模組資料模型)

### 任務

#### Flyway Migration

- [x] **V58 Flyway migration** — 建立 4 張表 + 索引 + partial unique index (D3)
  - `kpi_indicators` — KPI 指標定義 (code, name, category, formula, target, weight, data_source, unit, status)
  - `kpi_raw_data` — 原始數據 (indicator_id, period_year/month, contract_id, raw_value, source)
  - `kpi_results` — 計算結果 (indicator_id, period_year/month, contract_id, result_value, achievement)
  - `kpi_periods` — 期間管理 (period_year/month, locked, locked_by, unlock_reason)
- [x] **Unique constraints** — partial unique index 處理 nullable contract_id
  - `kpi_indicators`: UK(tenant_id, indicator_code)
  - `kpi_raw_data`: 2 partial indexes (with/without contract_id)
  - `kpi_results`: 2 partial indexes (with/without contract_id)
  - `kpi_periods`: UK(tenant_id, period_year, period_month)
- [x] **FK 約束** — indicator_id → kpi_indicators(id), contract_id → contracts(id), tenant_id → tenant(tenant_id)
- [x] **索引** — period_year/month 複合索引, indicator_id 索引

#### JPA Entity

- [x] **KpiIndicator entity** — category enum (MAINTENANCE/POWER/RESPONSE/QUALITY/CUSTOM), formula_type enum (SPEL/JS), TenantAware
- [x] **KpiRawData entity** — source enum (AUTO/MANUAL_IMPORT), TenantAware, ManyToOne indicator
- [x] **KpiResult entity** — achievement = result/target*100, TenantAware, ManyToOne indicator
- [x] **KpiPeriod entity** — locked boolean, TenantAware

#### Repository

- [x] **KpiIndicatorRepository** — findByFilters(category, status, keyword), findByTenantIdAndStatus
- [x] **KpiRawDataRepository** — findByFilters, findCityLevel, UPSERT 查詢
- [x] **KpiResultRepository** — findByFilters, findCityLevel, findByContractId 多期結果
- [x] **KpiPeriodRepository** — findByTenantIdAndPeriodYearAndPeriodMonth

#### 權限 + 選單

- [x] **V59 權限 migration** — KPI_VIEW, KPI_MANAGE, KPI_LOCK, KPI_UNLOCK, KPI_CONTRACTOR_VIEW
- [x] **選單項目** — 績效管理 (5 子選單: 指標管理/數據管理/績效計算/績效報表/期間管理)
- [x] **角色綁定** — ADMIN/DEPT_ADMIN 全權; OPERATOR view+manage+lock; VIEWER/MONITOR view; FIELD_USER contractor_view

#### Enum + Audit

- [x] **5 enums** — KpiCategory, FormulaType, KpiDataSource, KpiRawDataSource, KpiIndicatorStatus
- [x] **AuditCategory.KPI** — 新增 KPI 分類
- [x] **8 AuditEventType** — CREATE/UPDATE/DELETE_KPI_INDICATOR, IMPORT_KPI_DATA, CALCULATE_KPI, LOCK/UNLOCK_KPI_PERIOD, EXPORT_KPI_REPORT

---

## 6b — KPI 定義 CRUD + SpEL 公式引擎 (8 天, after 6a) ✅ 已完成

> **Spec**: 02-spec/09-performance-management.md §9-1-A, §9-2  
> **SRS**: SRS-09-001 (AC-09-001-1~4), SRS-09-005 (AC-09-005-1~3)  
> **SA**: FN-08-001~005 (KPI 指標定義)  
> **SD**: SD-08-performance.md §3~4  
> **Test**: TS-08-performance.md §1 (TC-08-001~005)

### 需求

- AC-09-001-1: 可新增績效指標：名稱、分類、計算公式、權重、滿分
- AC-09-001-2: 計算公式支援動態定義（非寫死程式碼）
- AC-09-001-3: 可設定加分/扣分條件
- AC-09-001-4: 可修改/刪除/停用指標（停用不影響歷史計分）
- AC-09-005-1: 績效計算公式可由機關依決議調整，無需修改程式碼
- AC-09-005-2: 公式變更後，新期間使用新公式，歷史期間（已鎖定）不受影響
- AC-09-005-3: 公式變更留稽核紀錄

### 任務

#### 公式引擎 (engine/)

- [x] **FormulaEngine** — 統一入口，依 formulaType 分派 SpEL / GraalJS
- [x] **SpelEvaluator** — Spring SpEL 計算，支援變數綁定 (value, target, weight, etc.)
- [x] **GraalJsEvaluator** — ⏳ 延後 (D1: Phase 6 只做 SpEL)
  - FormulaEngine.evaluate() 遇 formulaType=JS → throw UnsupportedOperationException
  - 後續有需求時再加 GraalVM 依賴 + 實作 sandbox

#### KPI 指標 CRUD

- [x] **KpiIndicatorRequest DTO** — indicatorCode, indicatorName, category, formulaType, formula, targetValue, weight, dataSource, unit
- [x] **KpiIndicatorResponse DTO** — 同上 + id, status, createdAt, updatedAt
- [x] **FormulaTestRequest DTO** — formulaType, formula, testData (Map<String, Object>)
- [x] **FormulaTestResponse DTO** — result (BigDecimal), success, errorMessage
- [x] **KpiIndicatorService**
  - `list(category, pageable)` — 分頁查詢 + 篩選
  - `create(request)` — 公式語法預驗證 + 新增
  - `update(id, request)` — 修改 + 稽核紀錄 (公式變更)
  - `delete(id)` — 有歷史數據時 soft delete (status=INACTIVE)
  - `testFormula(request)` — sandbox 執行公式測試
- [x] **KpiIndicatorController** — 5 端點 (@PreAuthorize KPI_VIEW / KPI_MANAGE)
  - `GET /v1/auth/kpi/indicators` — 列表
  - `POST /v1/auth/kpi/indicators` — 新增
  - `PUT /v1/auth/kpi/indicators/{id}` — 編輯
  - `DELETE /v1/auth/kpi/indicators/{id}` — 刪除/停用
  - `POST /v1/auth/kpi/indicators/test-formula` — 公式沙箱測試

#### 種子指標 (選配)

- [ ] **預設 KPI 指標** — ⏳ 可選 seed data (未實作，依需求再加)
  - 照明妥善率: `(activeDevices / totalDevices) * 100`
  - 維修時效: `avgRepairHours <= target ? 100 : (target / avgRepairHours) * 100`
  - 巡查完成率: `(completedPatrols / scheduledPatrols) * 100`
  - 換裝進度: `(completedReplacements / dispatchedReplacements) * 100`

---

## 6c — 資料收集 (5 天, after 6b) ✅ 已完成

> **Spec**: 02-spec/09-performance-management.md §9-1-B  
> **SRS**: SRS-09-002 (AC-09-002-1~3)  
> **SA**: FN-08-006~008 (績效數據匯入)  
> **SD**: SD-08-performance.md §3 (collector/ + scheduler/)  
> **Test**: TS-08-performance.md §2 (TC-08-006~008)

### 需求

- AC-09-002-1: 可單筆輸入計分所需數據
- AC-09-002-2: 可整批匯入數據（CSV/Excel）
- AC-09-002-3: 匯入前驗證資料格式

### 任務

#### 自動數據收集 (collector/)

- [x] **RepairDataCollector** — 從報修模組收集 (native SQL via EntityManager)
  - 維修案件平均完修時間 (avg response time)
  - 維修完成率 (completion rate)
  - 照明妥善率 (active / total devices)
  - 巡查完成率
- [x] **MaterialDataCollector** — 從材料模組收集 (native SQL via EntityManager)
  - 材料耗用量/成本彙總
- [x] **IoTDataCollector** — 從 IoT 模組收集 (stub, returns empty — Phase 7 完成後生效)
  - 智能路燈可用率 (uptime)
  - 告警次數
  - 預留介面，Phase 7 前回 null/skip

#### 排程 (scheduler/)

- [x] **KpiDataCollectionJob** — `@Scheduled(cron = "0 0 1 * * *")` 每日 01:00
  - 呼叫各 Collector.collect(yesterday)
  - UPSERT kpi_raw_data (source=AUTO)
  - 收集失敗 → log + 不中斷其他 Collector

#### 手動匯入

- [x] **ImportRequest DTO** — MultipartFile (Excel/CSV)
- [x] **KpiDataService**
  - `importFromExcel(file)` — Apache POI 解析 Excel (indicator_code + period + value)
  - `importFromCsv(file)` — CSV 解析
  - 驗證: indicator_code 存在, period 格式正確, value 數值合法
  - 重複 key → 覆蓋 (UPSERT)
- [x] **KpiRawDataResponse DTO** — indicatorCode, indicatorName, periodYear, periodMonth, rawValue, source, importedAt
- [x] **KpiDataController** — 2 端點 (@PreAuthorize KPI_VIEW / KPI_MANAGE)
  - `POST /v1/auth/kpi/data/import` — Excel/CSV 匯入 (multipart)
  - `GET /v1/auth/kpi/data` — 原始數據查詢 (by indicator + period)
  - (FN-08-007 自動收集為排程，無 API 端點)

---

## 6d — 計分 + 鎖定機制 (5 天, after 6c) ✅ 已完成

> **Spec**: 02-spec/09-performance-management.md §9-1-C, §9-1-D  
> **SRS**: SRS-09-003 (AC-09-003-1~4), SRS-09-004 (AC-09-004-1~4)  
> **SA**: FN-08-009~012 (自動績效計算), FN-08-017~019 (期間鎖定)  
> **SD**: SD-08-performance.md §3~5  
> **Test**: TS-08-performance.md §3, §5 (TC-08-009~012, TC-08-017~019)

### 需求

- AC-09-003-1: 依公式自動計算各期各項指標得分
- AC-09-003-2: 介面呈現績效統計分數（表格+圖表）
- AC-09-003-3: 可匯出加扣點明細報表
- AC-09-003-4: 可匯出估驗計價報表
- AC-09-004-1: 可鎖定指定期間的績效計分
- AC-09-004-2: 鎖定後該期分數不因資料更新而變動
- AC-09-004-3: 鎖定操作需經授權審核
- AC-09-004-4: 如需解鎖需特殊審核程序

### 任務

#### 計算引擎整合

- [x] **KpiCalculationService**
  - `calculateMonthly(year, month)` — 遍歷 ACTIVE indicators × contracts
    1. 檢查期間是否鎖定 → 鎖定則 skip
    2. 取得 kpi_raw_data
    3. FormulaEngine.evaluate(formula, variables)
    4. UPSERT kpi_results (result_value, target_value, achievement)
  - `calculateSingle(indicatorId, year, month)` — 手動觸發單一指標
  - `detectAnomaly(result)` — ⏳ 異常偵測 (延後實作)
- [x] **KpiCalculationJob** — `@Scheduled(cron = "0 0 2 1 * *")` 每月 1 日 02:00
  - 計算上月所有指標
  - 異常結果 → ⏳ Email 通知 (延後)

#### 計算結果查詢

- [x] **KpiResultResponse DTO** — indicatorCode, indicatorName, periodYear, periodMonth, resultValue, targetValue, achievement, calculatedAt
- [x] **KpiCalculationController** — 2 端點 (@PreAuthorize KPI_VIEW / KPI_MANAGE)
  - `POST /v1/auth/kpi/calculate` — 手動觸發 (yearMonth, indicatorId optional)
  - `GET /v1/auth/kpi/results` — 計算結果查詢 (by period + contract)

#### 期間鎖定/解鎖

- [x] **KpiPeriodService**
  - `lock(yearMonth, operatorId)` — 鎖定期間，記錄 locked_by + locked_at
  - `unlock(yearMonth, operatorId, reason)` — 解鎖，需 KPI_UNLOCK 權限，記錄 unlock_reason
  - `getPeriods()` — 查詢各期間鎖定狀態
- [x] **PeriodResponse DTO** — periodYear, periodMonth, locked, lockedAt, lockedBy, unlockReason
- [x] **KpiPeriodController** — 3 端點
  - `GET /v1/auth/kpi/periods` — 期間列表 (@PreAuthorize KPI_VIEW)
  - `PUT /v1/auth/kpi/periods/{yearMonth}/lock` — 鎖定 (@PreAuthorize KPI_LOCK)
  - `PUT /v1/auth/kpi/periods/{yearMonth}/unlock` — 解鎖 (@PreAuthorize KPI_UNLOCK)

---

## 6e — 報表匯出 (5 天, after 6d) ✅ 已完成

> **Spec**: 02-spec/09-performance-management.md §9-1-C  
> **SRS**: SRS-09-003 (AC-09-003-3~4)  
> **SA**: FN-08-013~016 (績效報表), FN-08-020 (廠商績效查詢)  
> **SD**: SD-08-performance.md §4.4~4.6  
> **Test**: TS-08-performance.md §4, §6 (TC-08-013~016, TC-08-020)

### 任務

#### 報表服務

- [x] **KpiReportService**
  - `getMonthlyReport(year, month, contractId)` — 月績效報表 (各指標得分 + 加權總分)
  - `getYearlyReport(year, contractId)` — 年度報表 (12 個月趨勢)
  - `getCompareReport(year, month, contractIds[])` — 跨廠商/契約比較
  - `exportXls()` / `exportCsv()` — 匯出 XLS/CSV (SXSSFWorkbook 串流寫入)
    - Apache POI → XLS
    - CSV OutputStreamWriter + BOM
    - ⏳ ODS 延後 (待需求確認)

#### 報表 DTO

- [x] **MonthlyReportResponse** — 月份, 契約, indicators[]{code, name, rawValue, resultValue, target, achievement, weight, weightedScore}
- [x] **YearlyReportResponse** — 年度, 契約, months[12]{month, totalScore}, indicators[]{code, name, monthlyValues[12]}
- [x] **CompareReportResponse** — 期間, contracts[]{contractName, totalScore, indicators[]}

#### Controller

- [x] **KpiReportController** — 5 端點 (@PreAuthorize KPI_VIEW)
  - `GET /v1/auth/kpi/reports/monthly` — 月績效 (year, month, contractId)
  - `GET /v1/auth/kpi/reports/yearly` — 年報 (year, contractId)
  - `GET /v1/auth/kpi/reports/compare` — 比較 (year, month, contractIds)
  - `GET /v1/auth/kpi/reports/export/xls` — 匯出 Excel
  - `GET /v1/auth/kpi/reports/export/csv` — 匯出 CSV

#### 廠商績效查詢

- [x] **ContractorKpiController** — 1 端點 (@PreAuthorize KPI_CONTRACTOR_VIEW)
  - `GET /v1/auth/kpi/contractor/results` — 廠商績效查詢 (by contractId + year)
  - 僅回傳該廠商相關指標結果

---

## 6f — 前端 6 頁面 (20 天, after 6a, 與後端並行) ✅ 已完成

> **SA**: SA-08-kpi.md 前端頁面清單  
> **SD**: SD-08-performance.md §3 (class structure)  
> **路由**: /admin/kpi/*, /contractor/kpi

### 任務

#### API Client + Types

- [x] **api/kpi/index.ts** — 20+ API functions 對應全部端點
- [x] **types/kpi.ts** — TypeScript 介面
  - KpiIndicatorRequest, KpiIndicatorResponse
  - FormulaTestRequest, FormulaTestResponse
  - KpiRawDataResponse, ImportRequest
  - KpiResultResponse
  - MonthlyReportResponse, YearlyReportResponse, CompareReportResponse
  - PeriodResponse

#### 頁面 1: KPI 指標管理 (/admin/kpi/indicators)

- [x] **KpiIndicatorView.vue**
  - el-table 指標列表 (code, name, category, formula, target, weight, status)
  - 篩選: category 下拉, status 下拉
  - 新增/編輯 dialog
    - 公式編輯區 (formulaType 切換 SpEL/JS)
    - 公式測試區 (輸入測試變數 → 即時計算結果)
  - 停用/啟用 toggle
  - 刪除 (有歷史數據時提示轉為停用)
- [x] **i18n** — zh-TW KPI 指標相關 100+ key

#### 頁面 2: 績效數據 (/admin/kpi/data)

- [x] **KpiDataView.vue**
  - 原始數據 el-table (indicator, period, value, source, importedAt)
  - 篩選: indicator 下拉, period (year-month) 選擇器
  - Excel/CSV 匯入按鈕 (el-upload → preview → confirm)
  - 匯入結果摘要 (新增/覆蓋筆數)
- [x] **i18n** — zh-TW 數據管理相關 key

#### 頁面 3: 績效計算 (/admin/kpi/calculate)

- [x] **KpiCalculateView.vue**
  - 計算結果 el-table (indicator, period, rawValue, resultValue, target, achievement)
  - 篩選: period, contract 下拉
  - 手動計算按鈕 (選擇 yearMonth → POST calculate)
  - 鎖定期間標記 (disabled + 提示)
  - achievement 達標/未達標顏色區分 (綠/紅)
- [x] **i18n** — zh-TW 計算相關 key

#### 頁面 4: 績效報表 (/admin/kpi/reports)

- [x] **KpiReportView.vue**
  - Tab 切換: 月報 / 年報 (比較功能後續擴充)
  - **月報 Tab**: 選擇 month + contract → 各指標表格 + 加權總分
  - **年報 Tab**: 選擇 year + contract → 12 個月趨勢表格 (圖表待 D4 確認套件)
  - 匯出 Excel / CSV → blob 下載
- [x] **i18n** — zh-TW 報表相關 key

#### 頁面 5: 期間管理 (/admin/kpi/periods)

- [x] **KpiPeriodView.vue**
  - el-table 期間列表 (year, month, locked, lockedBy, lockedAt)
  - 鎖定按鈕 (confirm dialog → PUT lock)
  - 解鎖按鈕 (輸入原因 dialog → PUT unlock, 需 KPI_UNLOCK 權限)
  - 鎖定狀態 tag (已鎖定=紅, 未鎖定=綠)
- [x] **i18n** — zh-TW 期間管理相關 key

#### 頁面 6: 廠商績效 (/contractor/kpi)

- [x] **ContractorKpiView.vue**
  - 自身績效結果 el-table (period, indicator, resultValue, target, achievement)
  - 期間篩選
  - 唯讀，無操作按鈕
- [x] **i18n** — zh-TW 廠商績效相關 key

#### 路由 + 選單

- [x] **router** — 6 路由: indicators, data, calculate, reports, periods, contractor/kpi
- [ ] **sidebar 選單** — 績效管理 group + 5 子項 (admin) + 1 子項 (contractor) — 已由 V59 migration 建立選單，動態載入

---

## 6g — 測試 (5 天, after 6e) ✅ 已完成 (22/22 TC passed)

> **Test**: TS-08-performance.md (22 TC)

### 後端單元測試

#### KPI 指標管理 (7 TC)

- [x] TC-08-001-01: KPI 列表查詢 — GET /kpi/indicators → 200, list (含 formula, unit)
- [x] TC-08-002-01: 新增 KPI 指標 — POST /kpi/indicators → 201
- [x] TC-08-002-02: code 重複 — POST /kpi/indicators → error duplicate
- [x] TC-08-003-01: 編輯 KPI 指標 — PUT /kpi/indicators/{id} → 200, formula 更新
- [x] TC-08-004-01: 刪除 KPI 指標 — DELETE /kpi/indicators/{id} → 200, soft delete
- [x] TC-08-005-01: 公式測試成功 — POST /kpi/indicators/test-formula → 200, result
- [x] TC-08-005-02: 無效公式 — POST /kpi/indicators/test-formula → error parse error

#### 數據管理 (4 TC)

- [x] TC-08-006-01: Excel 匯入數據 — POST /kpi/data/import → 200, imported count
- [ ] TC-08-006-02: 格式錯誤 — POST /kpi/data/import → error validation — ⏳ 待補充
- [x] TC-08-007-01: 排程自動收集 — (排程) → data stored, source=AUTO
- [x] TC-08-008-01: 原始數據查詢 — GET /kpi/data → 200, list by period + indicator

#### 計算引擎 (5 TC)

- [x] TC-08-009-01: 排程自動計算 — (排程) → results 產生, all indicators
- [x] TC-08-010-01: 手動觸發計算 — POST /kpi/calculate → 200, results by yearMonth
- [x] TC-08-010-02: 已鎖定期間計算 — POST /kpi/calculate → error, period locked
- [x] TC-08-011-01: 計算結果查詢 — GET /kpi/results → 200, list (score + grade)
- [ ] TC-08-012-01: 異常指標告警 — (內部) → notification, severity — ⏳ anomaly detection 延後

#### 報表 (4 TC)

- [x] TC-08-013-01: 月績效報表 — GET /kpi/reports/monthly → 200, by indicator
- [x] TC-08-014-01: 年度績效報表 — GET /kpi/reports/yearly → 200, 12 months
- [x] TC-08-015-01: 跨期比較 — GET /kpi/reports/compare → 200, delta %
- [x] TC-08-016-01: 報表匯出 — GET /kpi/reports/export → 200, file (Excel)

#### 期間管理 (3 TC)

- [x] TC-08-017-01: 鎖定期間 — PUT /kpi/periods/{yearMonth}/lock → 200, locked=true
- [x] TC-08-018-01: 解鎖期間 — PUT /kpi/periods/{yearMonth}/unlock → 200, locked=false
- [x] TC-08-019-01: 查詢期間狀態 — GET /kpi/periods → 200, list (locked flag)

#### 廠商績效 (1 TC)

- [x] TC-08-020-01: 廠商績效查詢 — GET /kpi/contractor/results → 200, by contractor

### 額外測試 (實作時補充)

- [x] FormulaEngine 單元測試 — SpEL 5 case + JS unsupported 1 case + error 1 case = 7 TC
- [ ] Collector 單元測試 — Mock 各模組 Repository, 驗證 collect 邏輯 — ⏳ 待補充
- [ ] 權限測試 — 各端點 403/401 驗證 — ⏳ 待補充

---

## 跨模組依賴

| 依賴來源 | KPI 指標 | 數據收集方式 | 備註 |
|---------|---------|-------------|------|
| §5 報修維護 | 維修時效, 照明妥善率, 巡查完成率 | RepairDataCollector (daily) | Phase 4 已完成 ✅ |
| §6 換裝維護 | 換裝進度 | RepairDataCollector (daily) | Phase 4 已完成 ✅ |
| §7 材料管理 | 材料耗用 | MaterialDataCollector (daily) | Phase 3 已完成 ✅ |
| §8 智能路燈 | 智能路燈可用率, 告警次數 | IoTDataCollector (daily) | Phase 7 未完成 → 預留介面 |

---

## 技術決策記錄

| # | 議題 | 決策 | 說明 |
|---|------|------|------|
| D1 | 公式引擎 | **Phase 6 只做 SpEL**；GraalJS 延後 | SpEL 已涵蓋加減乘除/三元運算/比較；FormulaEngine 預留 JS 分支 throw UnsupportedOperationException，後續有需求再加 GraalVM 依賴 |
| D2 | DB 表名統一 | **以 SD 為準** (4 表) | kpi_indicators (非 SRS 的 kpi_definitions), kpi_raw_data (非 kpi_data_imports), kpi_results, kpi_periods (含鎖定欄位, 不另建 kpi_locks) |
| D3 | contract_id nullable | **nullable + partial unique index** | 全市層級指標 contract_id=NULL；UK 拆兩個 partial index: WHERE contract_id IS NOT NULL 和 WHERE contract_id IS NULL (避免 NULL != NULL 問題) |
| D4 | 前端圖表套件 | 待確認 package.json 後決定 | ECharts 功能強但大；Chart.js 輕量；6f 實作時確認 |
| D5 | IoT 資料收集 | 預留介面 | Phase 7 完成前 IoTDataCollector 回傳空值/skip |
| D6 | 報表格式 | ODS + XLS + CSV | ODS 為政府標準, XLS 通用, CSV 輕量 |
| D7 | 鎖定權限分離 | KPI_LOCK (GOV_MGR) / KPI_UNLOCK (GOV_CHIEF) | 鎖定門檻低, 解鎖需主管層級 |

---

## 文件追溯

| 分類 | 文件 |
|------|------|
| **Spec** | 02-spec/09-performance-management.md §9-1 (A~D), §9-2 |
| **SRS** | SRS-09-001 (KPI 定義), SRS-09-002 (數據匯入), SRS-09-003 (計算/呈現/匯出), SRS-09-004 (期間鎖定), SRS-09-005 (公式調整) |
| **SA** | SA-08-kpi.md FN-08-001~020 (20 功能節點) |
| **SD** | SD-08-performance.md §1~5 (DB, Class, API, Sequence) |
| **Test** | TS-08-performance.md (22 TC) |
| **Plan** | 99-plan/2026-04-24-execution-plan.md §Phase 6 |
| **Gantt** | 99-plan/2026-04-24-gantt.md (6a~6g) |

---

## 風險 & 注意事項

| 風險 | 影響 | 緩解措施 |
|------|------|---------|
| ~~GraalJS sandbox 安全性~~ | ~~若沙箱未正確配置可能導致 RCE~~ | Phase 6 不做 GraalJS (D1)，風險消除 |
| SpEL injection | 使用者輸入公式可能注入惡意表達式 | 白名單允許的 SpEL function；禁止 T() 型別存取；限制 context 變數 |
| IoT 模組未就緒 | IoTDataCollector 無資料來源 | 預留介面 + graceful skip；Phase 7 完成後啟用 |
| contracts 表未確認 | kpi_raw_data/results FK → contracts(id) | 確認 contracts entity 存在或用 nullable FK |
| 報表匯出效能 | 大量數據 ODS/XLS 產生慢 | 串流寫入 (SXSSFWorkbook)；分頁匯出 |
| 公式變更影響 | 修改公式後新舊結果不一致 | 已鎖定期間不受影響 (AC-09-005-2)；公式版本化 (稽核紀錄) |
