# SA-10 儀表板 Function List

> **對應需求**：§11-(1) ~ §11-(10)  
> **SRS 對應**：SRS-11-001 ~ SRS-11-010  
> **Spec 來源**：`/02-spec/11-dashboard.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-10-01 | 儀表板配置 | GOV_ADMIN | 自訂版面配置+Widget 管理 |
| UC-10-02 | 即時數據展示 | GOV_ADMIN, GOV_MGR, GOV_CHIEF | 瀏覽各類即時統計 |
| UC-10-03 | Widget 互動 | GOV_ADMIN | Widget 點選→明細頁 |

---

## Function List

### 儀表板基礎 (§11 共通)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-001 | 儀表板版面查詢 | R | GOV_ADMIN | 使用者 ID | 版面配置 JSON | 個人化版面；預設配置 | SRS-11-001 | §11 | GET /v1/auth/dashboard/layout |
| FN-10-002 | 儀表板版面儲存 | U | GOV_ADMIN | 版面配置 JSON (vue-grid-layout) | 更新結果 | Widget 位置+大小+排序 | SRS-11-001 | §11 | PUT /v1/auth/dashboard/layout |
| FN-10-003 | 版面重置為預設 | U | GOV_ADMIN | — | 預設版面 | — | SRS-11-001 | §11 | POST /v1/auth/dashboard/layout/reset |

### 養護統計 Widget (§11-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-004 | 養護統計數據 | R | GOV_ADMIN | 時間範圍 | 報修/維修/巡查次數 | — | SRS-11-001 | §11-1 | GET /v1/auth/dashboard/widgets/maintenance |
| FN-10-005 | 養護趨勢圖 | R | GOV_ADMIN | 時間範圍 | ECharts 資料 | 月/季/年趨勢 | SRS-11-001 | §11-1 | GET /v1/auth/dashboard/widgets/maintenance/trend |

### 停電告警 Widget (§11-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-006 | 停電統計 | R | GOV_ADMIN | — | 當前停電區域+數量 | 即時(WebSocket 推送) | SRS-11-002 | §11-2 | GET /v1/auth/dashboard/widgets/outage |
| FN-10-007 | 停電歷史趨勢 | R | GOV_ADMIN | 時間範圍 | 停電次數+恢復時間 | — | SRS-11-002 | §11-2 | GET /v1/auth/dashboard/widgets/outage/trend |

### 故障 Heatmap Widget (§11-3)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-008 | 故障熱力圖數據 | R | GOV_ADMIN | 時間範圍、區域 | GeoJSON heatmap | 依故障密度著色 | SRS-11-003 | §11-3 | GET /v1/auth/dashboard/widgets/fault-heatmap |
| FN-10-009 | 故障分類統計 | R | GOV_ADMIN | 時間範圍 | 故障類型分布 | 圓餅圖/長條圖 | SRS-11-003 | §11-3 | GET /v1/auth/dashboard/widgets/fault-category |

### KPI 績效 Widget (§11-4)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-010 | KPI 摘要卡片 | R | GOV_ADMIN, GOV_MGR | 期間 | 核心 KPI 值 | 妥善率/回應時間/完成率 | SRS-11-004 | §11-4 | GET /v1/auth/dashboard/widgets/kpi |
| FN-10-011 | KPI 趨勢圖 | R | GOV_MGR | 指標 ID、時間 | 趨勢圖 | — | SRS-11-004 | §11-4 | GET /v1/auth/dashboard/widgets/kpi/trend |

### 路燈數量 Widget (§11-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-012 | 路燈數量統計 | R | GOV_ADMIN | 區域/行政區 | 各類路燈數量 | 依類型/行政區分組 | SRS-11-005 | §11-5 | GET /v1/auth/dashboard/widgets/lamp-count |
| FN-10-013 | 路燈在線/離線統計 | R | GOV_ADMIN | — | 在線/離線數量 | 即時 IoT 狀態 | SRS-11-005 | §11-5 | GET /v1/auth/dashboard/widgets/lamp-status |

### 配電箱 Widget (§11-6)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-014 | 配電箱用電統計 | R | GOV_ADMIN | 區域 | 各配電箱用電 | — | SRS-11-006 | §11-6 | GET /v1/auth/dashboard/widgets/panel-box |
| FN-10-015 | 配電箱異常告警 | R | GOV_ADMIN | — | 異常配電箱清單 | 跳脫/過載 | SRS-11-006 | §11-6 | GET /v1/auth/dashboard/widgets/panel-box/alerts |

### 附件(檔案) Widget (§11-7)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-016 | 附件統計 | R | GOV_ADMIN | — | 檔案數量+容量 | 依類型分組 | SRS-11-007 | §11-7 | GET /v1/auth/dashboard/widgets/attachments |

### 電費 Widget (§11-8)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-017 | 月電費統計 | R | GOV_ADMIN | 月份 | 電費金額 | 依區域/契約 | SRS-11-008 | §11-8 | GET /v1/auth/dashboard/widgets/electricity-cost |
| FN-10-018 | 電費趨勢圖 | R | GOV_ADMIN | 時間範圍 | 月度趨勢 | 含節能比較 | SRS-11-008 | §11-8 | GET /v1/auth/dashboard/widgets/electricity-cost/trend |

### 智慧電表 Widget (§11-9)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-019 | 電表即時讀數 | R | GOV_ADMIN | — | 各電表最新讀數 | — | SRS-11-009 | §11-9 | GET /v1/auth/dashboard/widgets/meter |
| FN-10-020 | 電表用電趨勢 | R | GOV_ADMIN | 電表 ID、時間 | 趨勢圖 | — | SRS-11-009 | §11-9 | GET /v1/auth/dashboard/widgets/meter/trend |

### GIS 總覽 Widget (§11-10)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-10-021 | GIS 總覽地圖 | R | GOV_ADMIN, GOV_CHIEF | bounds | GeoJSON 聚合 | 多圖層疊加(路燈+故障+停電) | SRS-11-010 | §11-10 | GET /v1/auth/dashboard/widgets/gis |
| FN-10-022 | Widget 鑽取(drill-down) | R | GOV_ADMIN | Widget ID、點擊項 | 跳轉明細頁 | 各 Widget 可 click→明細 | SRS-11-001 | §11 | (前端路由) |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 儀表板主頁 | /admin/dashboard | vue-grid-layout + Widget 配置 | FN-10-001~003 |
| 養護統計 Widget | (嵌入) | 報修/維修/巡查統計 | FN-10-004~005 |
| 停電告警 Widget | (嵌入) | 停電即時+趨勢 | FN-10-006~007 |
| 故障熱力圖 Widget | (嵌入) | Heatmap + 分類 | FN-10-008~009 |
| KPI 績效 Widget | (嵌入) | KPI 卡片+趨勢 | FN-10-010~011 |
| 路燈數量 Widget | (嵌入) | 數量+在線/離線 | FN-10-012~013 |
| 配電箱 Widget | (嵌入) | 用電+異常 | FN-10-014~015 |
| 電費 Widget | (嵌入) | 電費+趨勢 | FN-10-017~018 |
| 電表 Widget | (嵌入) | 讀數+趨勢 | FN-10-019~020 |
| GIS 總覽 Widget | (嵌入) | 多圖層地圖 | FN-10-021~022 |
