# TS-08 績效管理 — Test Specification (Forward Design)

> **對應 SA**：SA-08-performance (FN-08-001 ~ FN-08-020)  
> **對應 SD**：SD-08-performance  
> **Test Classes**：4 (FormulaEngineTest, KpiIndicatorServiceTest, KpiCalculationServiceTest, KpiPeriodServiceTest)  
> **Phase**：Phase 6 — 績效管理 ✅ 已完成

---

## 使用方式

本文件為 **前瞻設計 TC**，用於 Phase 6 實作時的驗收標準。  
22/22 TC 已通過 (mvn test 2026-04-26)。

---

## 1. KPI 指標管理 (FN-08-001 ~ FN-08-005)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-08-001-01 | FN-08-001 KPI 列表 | 查詢 KPI 指標 | GET /kpi/indicators | 200, list | 含 formula, unit |
| TC-08-002-01 | FN-08-002 新增 KPI | 新增指標 | POST /kpi/indicators | 201 | id + formula |
| TC-08-002-02 | FN-08-002 | code 重複 | POST /kpi/indicators | error | duplicate |
| TC-08-003-01 | FN-08-003 編輯 KPI | 更新公式 | PUT /kpi/indicators/{id} | 200 | formula 更新 |
| TC-08-004-01 | FN-08-004 刪除 KPI | 刪除指標 | DELETE /kpi/indicators/{id} | 200 | soft delete |
| TC-08-005-01 | FN-08-005 公式測試 | 預覽計算結果 | POST /kpi/indicators/test-formula | 200, result | 公式有效 |
| TC-08-005-02 | FN-08-005 | 無效公式 | POST /kpi/indicators/test-formula | error | parse error |

---

## 2. 數據管理 (FN-08-006 ~ FN-08-008)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-08-006-01 | FN-08-006 手動匯入 | Excel 匯入數據 | POST /kpi/data/import | 200, count | imported count |
| TC-08-006-02 | FN-08-006 | 格式錯誤 | POST /kpi/data/import | error | validation |
| TC-08-007-01 | FN-08-007 自動收集 | 排程收集 IoT 數據 | (排程) | data stored | source=AUTO |
| TC-08-008-01 | FN-08-008 數據查詢 | 原始數據列表 | GET /kpi/data | 200, list | by period + indicator |

---

## 3. 計算引擎 (FN-08-009 ~ FN-08-012)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-08-009-01 | FN-08-009 排程計算 | 月底自動計算 | (排程) | results 產生 | all indicators |
| TC-08-010-01 | FN-08-010 手動計算 | 手動觸發 | POST /kpi/calculate | 200, results | by yearMonth |
| TC-08-010-02 | FN-08-010 | 已鎖定期間 | POST /kpi/calculate | error | period locked |
| TC-08-011-01 | FN-08-011 結果查詢 | 查詢計算結果 | GET /kpi/results | 200, list | score + grade |
| TC-08-012-01 | FN-08-012 異常告警 | 指標低於閾值 | (內部) | notification | severity |

---

## 4. 報表 (FN-08-013 ~ FN-08-016)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-08-013-01 | FN-08-013 月報 | 月績效報表 | GET /kpi/reports/monthly | 200, report | by indicator |
| TC-08-014-01 | FN-08-014 年報 | 年度績效報表 | GET /kpi/reports/yearly | 200, report | 12 months |
| TC-08-015-01 | FN-08-015 比較 | 跨期比較 | GET /kpi/reports/compare | 200, comparison | delta % |
| TC-08-016-01 | FN-08-016 匯出 | 報表匯出 | GET /kpi/reports/export | 200, file | Excel |

---

## 5. 期間管理 (FN-08-017 ~ FN-08-019)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-08-017-01 | FN-08-017 鎖定 | 鎖定期間 | PUT /kpi/periods/{yearMonth}/lock | 200, locked=true | 不可再計算 |
| TC-08-018-01 | FN-08-018 解鎖 | 解鎖期間 | PUT /kpi/periods/{yearMonth}/unlock | 200, locked=false | 可再計算 |
| TC-08-019-01 | FN-08-019 狀態 | 查詢期間狀態 | GET /kpi/periods | 200, list | locked flag |

---

## 6. 廠商績效 (FN-08-020)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-08-020-01 | FN-08-020 廠商績效 | 廠商績效查詢 | GET /kpi/contractor/results | 200, list | by contractor |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ✅ 已通過 | 22 |
| ⏳ 延後 (anomaly detection, collector) | 2 |
| **總 TC 數** | **22** |
