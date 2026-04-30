# SA-08 績效管理 Function List

> **對應需求**：§9-(1) ~ §9-(5)  
> **SRS 對應**：SRS-09-001 ~ SRS-09-005  
> **Spec 來源**：`/02-spec/09-performance-management.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-08-01 | KPI 指標管理 | GOV_ADMIN, GOV_MGR | 定義+維護績效指標 |
| UC-08-02 | 績效數據管理 | GOV_ADMIN | 匯入/接收計算用原始數據 |
| UC-08-03 | 自動績效計算 | 系統 | 依公式+數據計算 KPI |
| UC-08-04 | 績效報表查閱 | GOV_ADMIN, GOV_MGR, GOV_CHIEF | 查閱+匯出 |
| UC-08-05 | 期間鎖定 | GOV_MGR | 鎖定已確認期間 |
| UC-08-06 | 廠商績效查詢 | CONTRACTOR | 查詢自身績效 |

---

## Function List

### KPI 指標定義 (§9-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-08-001 | KPI 指標列表 | R | GOV_ADMIN | 類別、分頁 | 指標清單 | — | SRS-09-001 | §9-1 | GET /v1/auth/kpi/indicators |
| FN-08-002 | 新增 KPI 指標 | C | GOV_ADMIN | 名稱、類別、公式(SpEL/GraalJS)、目標值、權重、資料來源 | 指標資料 | 公式語法驗證；動態指標可後續新增 | SRS-09-001 | §9-1 | POST /v1/auth/kpi/indicators |
| FN-08-003 | 編輯 KPI 指標 | U | GOV_ADMIN | 指標 ID、修改欄位 | 更新結果 | 已鎖定期間不影響歷史值 | SRS-09-001 | §9-1 | PUT /v1/auth/kpi/indicators/{id} |
| FN-08-004 | 刪除 KPI 指標 | D | GOV_ADMIN | 指標 ID | 刪除結果 | 有歷史數據時僅可停用 | SRS-09-001 | §9-1 | DELETE /v1/auth/kpi/indicators/{id} |
| FN-08-005 | 公式預覽測試 | R | GOV_ADMIN | 公式、測試數據 | 計算結果 | 沙箱執行(GraalJS sandbox) | SRS-09-001 | §9-1 | POST /v1/auth/kpi/indicators/test-formula |

### 績效數據匯入 (§9-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-08-006 | 手動數據匯入 | I | GOV_ADMIN | Excel(指標代碼+值+期間) | 匯入結果 | 欄位驗證+重複覆蓋 | SRS-09-002 | §9-2 | POST /v1/auth/kpi/data/import |
| FN-08-007 | 自動數據收集 | I | 系統 | IoT(§8)/報修(§5)/材料(§7) | 原始數據 | 排程(每日/每月)從各模組 API 收集 | SRS-09-002 | §9-2 | (排程) |
| FN-08-008 | 原始數據查詢 | R | GOV_ADMIN | 指標、期間 | 原始數據清單 | — | SRS-09-002 | §9-2 | GET /v1/auth/kpi/data |

### 自動績效計算 (§9-3)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-08-009 | 排程自動計算 | P | 系統 | 指標+原始數據 | 績效結果 | 每月自動計算；SpEL/GraalJS 引擎 | SRS-09-003 | §9-3 | (排程) |
| FN-08-010 | 手動觸發計算 | P | GOV_ADMIN | 指標 ID、期間 | 績效結果 | 已鎖定期間不可重算 | SRS-09-003 | §9-3 | POST /v1/auth/kpi/calculate |
| FN-08-011 | 計算結果查詢 | R | GOV_ADMIN, GOV_MGR | 期間、契約、廠商 | 績效結果清單 | — | SRS-09-003 | §9-3 | GET /v1/auth/kpi/results |
| FN-08-012 | 異常計算結果告警 | N | 系統 | 計算偏差 | 告警通知 | 數據異常值偵測→E-mail 通知 | SRS-09-003 | §9-3 | (內部) |

### 績效報表 (§9-4)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-08-013 | 月績效報表 | R | GOV_ADMIN, GOV_MGR | 月份、契約 | 月報表 | — | SRS-09-004 | §9-4 | GET /v1/auth/kpi/reports/monthly |
| FN-08-014 | 年度績效報表 | R | GOV_ADMIN, GOV_MGR | 年度、契約 | 年報表 | 含趨勢圖 | SRS-09-004 | §9-4 | GET /v1/auth/kpi/reports/yearly |
| FN-08-015 | 績效比較報表 | R | GOV_MGR, GOV_CHIEF | 廠商/契約比較 | 比較表 | 多廠商/契約對比 | SRS-09-004 | §9-4 | GET /v1/auth/kpi/reports/compare |
| FN-08-016 | 報表匯出 | E | GOV_ADMIN | 報表類型、期間 | ODS/XLS/CSV | — | SRS-09-004 | §9-4 | GET /v1/auth/kpi/reports/export |

### 期間鎖定 (§9-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-08-017 | 鎖定期間 | U | GOV_MGR | 年月 | 鎖定結果 | 鎖定後不可修改數據+重算 | SRS-09-005 | §9-5 | PUT /v1/auth/kpi/periods/{yearMonth}/lock |
| FN-08-018 | 解鎖期間 | U | GOV_CHIEF | 年月 | 解鎖結果 | 需主管權限；記錄解鎖原因 | SRS-09-005 | §9-5 | PUT /v1/auth/kpi/periods/{yearMonth}/unlock |
| FN-08-019 | 期間狀態查詢 | R | GOV_ADMIN | — | 各期間鎖定狀態 | — | SRS-09-005 | §9-5 | GET /v1/auth/kpi/periods |

### 廠商端

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-08-020 | 廠商績效查詢 | R | CONTRACTOR | 期間 | 自身績效結果 | 僅顯示該廠商相關 | SRS-09-004 | §9-4 | GET /v1/auth/kpi/contractor/results |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| KPI 指標管理 | /admin/kpi/indicators | 指標 CRUD+公式測試 | FN-08-001~005 |
| 績效數據 | /admin/kpi/data | 匯入+查詢 | FN-08-006~008 |
| 績效計算 | /admin/kpi/calculate | 手動觸發+結果 | FN-08-009~012 |
| 績效報表 | /admin/kpi/reports | 月報/年報/比較+匯出 | FN-08-013~016 |
| 期間管理 | /admin/kpi/periods | 鎖定/解鎖 | FN-08-017~019 |
| 廠商績效 | /contractor/kpi | 查詢自身績效 | FN-08-020 |
