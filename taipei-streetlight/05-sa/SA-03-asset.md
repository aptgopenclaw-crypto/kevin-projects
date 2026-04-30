# SA-03 資產管理 Function List

> **對應需求**：§4-(1) ~ §4-(6)  
> **SRS 對應**：SRS-04-001 ~ SRS-04-022  
> **Spec 來源**：`/02-spec/04-asset-management.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-03-01 | 設備管理 | GOV_ADMIN, OPERATOR | 設備 CRUD + 搜尋篩選 |
| UC-03-02 | 設備地圖檢視 | GOV_ADMIN, GOV_MGR | GIS 地圖顯示設備點位 |
| UC-03-03 | 坐標轉換 | GOV_ADMIN | TWD97/GPS/台電坐標互轉 |
| UC-03-04 | 街景查閱 | GOV_ADMIN | 連結 Google Map 街景 |
| UC-03-05 | 圖資匯入 | GOV_ADMIN | GML/管線資料匯入 |
| UC-03-06 | 圖資匯出 | GOV_ADMIN | GML/資料大平台格式匯出 |
| UC-03-07 | 契約管理 | GOV_ADMIN, GOV_MGR | 契約 CRUD + 資產轉移 |
| UC-03-08 | 回路管理 | GOV_ADMIN | 電力回路 CRUD |
| UC-03-09 | 拓撲檢視 | GOV_ADMIN, OPERATOR | 設備拓撲樹狀結構 |
| UC-03-10 | 障礙工單管理 | GOV_ADMIN, OPERATOR | 障礙工單 CRUD + 關聯偵測 |
| UC-03-11 | 資產異動申請 | GOV_ADMIN | 加帳/除帳/變更 + 審核 |
| UC-03-12 | 設備匯出 | GOV_ADMIN | 資產清冊 ODS/XLS/CSV |
| UC-03-13 | 驗收文件管理 | GOV_ADMIN | 竣工圖/規格文件上傳管理 |
| UC-03-14 | 預防性維護排程 | GOV_ADMIN | 分區分期排程編輯 + 提醒 |

---

## Function List

### 設備管理 (§4-3)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-001 | 設備列表查詢 | R | GOV_ADMIN, OPERATOR | 設備類型、狀態、關鍵字、部門、分頁 | 設備清單 (分頁) | DataScope 限制可見範圍 | SRS-04-007 | §4-3 | GET /v1/auth/devices |
| FN-03-002 | 新增設備 | C | GOV_ADMIN | deviceCode、deviceType、坐標、dept、attributes(JSONB) | 設備資料 | deviceCode 不重複；deviceType 必填；JSONB ≤ 10KB | SRS-04-007 | §4-3 | POST /v1/auth/devices |
| FN-03-003 | 編輯設備 | U | GOV_ADMIN | 設備 ID、修改欄位 | 更新結果 | DECOMMISSIONED 設備不可編輯 | SRS-04-007 | §4-3 | PUT /v1/auth/devices/{id} |
| FN-03-004 | 查看設備詳情 | R | GOV_ADMIN, OPERATOR | 設備 ID | 完整設備資料 (含 attributes 展開) | DataScope 限制 | SRS-04-007 | §4-3 | GET /v1/auth/devices/{id} |
| FN-03-005 | 刪除設備 | D | GOV_ADMIN | 設備 ID | 刪除結果 | 有子設備(DEVICE_HAS_CHILDREN)、有未結案障礙(DEVICE_HAS_OPEN_FAULTS)時不可刪 | SRS-04-007 | §4-3 | DELETE /v1/auth/devices/{id} |
| FN-03-006 | 設備統計概覽 | R | GOV_ADMIN, GOV_MGR | — | 總數/依類型/依狀態/在線率/待修數 | — | SRS-04-007 | §4-3 | GET /v1/auth/devices/stats |
| FN-03-007 | 設備快速搜尋 | R | GOV_ADMIN | 關鍵字 (編號/地址) | 設備清單 | 支援模糊搜尋 + JSONB 內欄位搜尋 | SRS-04-015 | §4-3G | GET /v1/auth/devices/search |
| FN-03-008 | 設備 JSONB 動態欄位管理 | U | GOV_ADMIN | attributes JSONB | 更新結果 | 可自由新增欄位；不需程式修改 | SRS-04-007 | §4-3 | (含於 FN-03-002/003) |
| FN-03-009 | 設備歷程查詢 | R | GOV_ADMIN, OPERATOR | 設備 ID | 歷程清單 (device_events) | 安裝/更換/維修/巡查/認養/除帳 | SRS-04-008 | §4-3A | GET /v1/auth/devices/{id}/events |
| FN-03-010 | 設備組件替換 | U | GOV_ADMIN | 設備 ID、組件類型、新組件資料 | 更新結果 | 寫 device_event (MATERIAL_CHANGE) | SRS-04-008 | §4-3C | PUT /v1/auth/devices/{id}/replace-component |

### 設備拓撲 (§4-3 D-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-011 | 拓撲樹查詢 | R | GOV_ADMIN, OPERATOR | 根設備 ID / 全部 | 樹狀結構 | recursive CTE；含在線狀態 | SRS-04-009 | §4-3 D-2 | GET /v1/auth/devices/topology |
| FN-03-012 | 設定父設備 | U | GOV_ADMIN | 子設備 ID、父設備 ID | 更新結果 | 拓撲循環防護 (DEVICE_CIRCULAR_REFERENCE) | SRS-04-009 | §4-3 D-2 | PUT /v1/auth/devices/{id} |

### GIS 地理資訊 (§4-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-013 | 設備地圖查詢 | R | GOV_ADMIN, GOV_MGR | 邊界框(bbox)、設備類型、狀態 | GeoJSON 點位集合 | 依縮放等級聚合顯示（cluster） | SRS-04-001 | §4-1A | GET /v1/auth/devices/map |
| FN-03-014 | 坐標轉換 | P | GOV_ADMIN | 坐標值、來源格式、目標格式 | 轉換結果 | TWD97↔WGS84↔TWD67(台電) | SRS-04-002 | §4-1B | POST /v1/auth/devices/coordinate-convert |
| FN-03-015 | 街景連結 | R | GOV_ADMIN | 經緯度 | Google Street View URL | — | SRS-04-003 | §4-1C | (前端計算) |
| FN-03-016 | 底圖切換 | R | GOV_ADMIN | 圖層 ID | 底圖資料 | ArcGIS / OpenLayers / Google Map | SRS-04-004 | §4-1D | (前端設定) |
| FN-03-017 | 分區範圍圖層 | R | GOV_ADMIN | 分區類型(行政區/分隊/台電/廠商) | GeoJSON 邊界 | — | SRS-04-004 | §4-1D | GET /v1/auth/map/zones |
| FN-03-018 | 管線圖層 | R | GOV_ADMIN | 範圍 | 管線 GeoJSON | 含架空管線+地下管線 | SRS-04-004 | §4-1D | GET /v1/auth/map/pipelines |

### 圖資匯入匯出 (§4-1 F)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-019 | GML 匯入 | I | GOV_ADMIN | GML 檔案 | 匯入結果 (成功/差異/錯誤) | 比對現有資料→差異需人工確認 | SRS-04-005 | §4-1F | POST /v1/auth/devices/import/gml |
| FN-03-020 | GML 匯出 | E | GOV_ADMIN | 區域範圍、設備類型 | GML 檔案 | OGC GML 標準格式 | SRS-04-005 | §4-1F | GET /v1/auth/devices/export/gml |
| FN-03-021 | 工務局管線資料匯入 | I | GOV_ADMIN | 管線資料檔案 | 匯入結果 | 依工務局格式解析 | SRS-04-005 | §4-1F | POST /v1/auth/map/pipelines/import |
| FN-03-022 | 資料大平台匯出 | E | GOV_ADMIN | 篩選條件 | 規定格式 CSV | 符合「臺北市資料大平臺」格式 | SRS-04-005 | §4-1F | GET /v1/auth/devices/export/open-data |
| FN-03-023 | 圖資匯出至 CAD | E | GOV_ADMIN | 區域、比例尺、尺寸 | DXF/DWG | 含比例尺+圖面尺寸設定 | SRS-04-005 | §4-1F | GET /v1/auth/devices/export/cad |

### 回路管理 (§4-3 D-3)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-024 | 回路列表查詢 | R | GOV_ADMIN | 分電箱 ID、回路編號、分頁 | 回路清單 | — | SRS-04-010 | §4-3 D-3 | GET /v1/auth/circuits |
| FN-03-025 | 新增回路 | C | GOV_ADMIN | 分電箱 ID、回路編號、台電電號、用電類別 | 回路資料 | tenant_id + circuit_number 唯一 | SRS-04-010 | §4-3 D-3 | POST /v1/auth/circuits |
| FN-03-026 | 編輯回路 | U | GOV_ADMIN | 回路 ID、修改欄位 | 更新結果 | — | SRS-04-010 | §4-3 D-3 | PUT /v1/auth/circuits/{id} |
| FN-03-027 | 刪除回路 | D | GOV_ADMIN | 回路 ID | 刪除結果 | 有設備綁定時不可刪 (CIRCUIT_HAS_DEVICES) | SRS-04-010 | §4-3 D-3 | DELETE /v1/auth/circuits/{id} |
| FN-03-028 | 查看回路設備 | R | GOV_ADMIN | 回路 ID | 設備清單 | — | SRS-04-010 | §4-3 D-3 | GET /v1/auth/circuits/{id}/devices |

### 契約管理 (§4-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-029 | 契約列表查詢 | R | GOV_ADMIN, GOV_MGR | 狀態、關鍵字、分頁 | 契約清單 | — | SRS-04-006 | §4-2A | GET /v1/auth/contracts |
| FN-03-030 | 新增契約 | C | GOV_ADMIN | 名稱、廠商、期限、保固年限、預算年度、採購案號 | 契約資料 | 預設狀態 ACTIVE | SRS-04-006 | §4-2A | POST /v1/auth/contracts |
| FN-03-031 | 編輯契約 | U | GOV_ADMIN | 契約 ID、修改欄位 | 更新結果 | — | SRS-04-006 | §4-2A | PUT /v1/auth/contracts/{id} |
| FN-03-032 | 刪除契約 | D | GOV_ADMIN | 契約 ID | 刪除結果 | 有資產綁定時不可刪 | SRS-04-006 | §4-2A | DELETE /v1/auth/contracts/{id} |
| FN-03-033 | 契約資產明細 | R | GOV_ADMIN | 契約 ID、分頁 | 綁定設備清單 | — | SRS-04-006 | §4-2B | GET /v1/auth/contracts/{id}/devices |
| FN-03-034 | 契約資產批次轉移 | U | GOV_ADMIN | 來源契約 ID、目標契約 ID、設備 ID 清單 | 轉移結果 | 保固到期→權責轉移；寫稽核日誌 | SRS-04-006 | §4-2D | POST /v1/auth/contracts/transfer |
| FN-03-035 | 保固到期提醒 | N | 系統 | — | 通知 | 保固日 ≤ 30 天 → 推送提醒 | SRS-04-006 | §4-2C | (系統排程) |
| FN-03-036 | 預防性維護排程管理 | C/U | GOV_ADMIN | 分區、周期/日期、內容 | 排程資料 | — | SRS-04-006 | §4-2C | POST/PUT /v1/auth/contracts/schedules |
| FN-03-037 | 維護排程提醒 | N | 系統 | — | 通知 | 到期前 N 天提醒 | SRS-04-006 | §4-2C | (系統排程) |

### 障礙工單 (§4-3 D-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-038 | 障礙工單列表 | R | GOV_ADMIN, OPERATOR | 狀態、來源、設備 ID、分頁 | 工單清單 | — | SRS-04-011 | §4-3 D-5 | GET /v1/auth/faults |
| FN-03-039 | 新增障礙工單 | C | GOV_ADMIN, OPERATOR | 設備 ID、描述、來源 | 工單資料 | 自動回填 circuit_id；觸發關聯偵測 | SRS-04-011 | §4-3 D-5 | POST /v1/auth/faults |
| FN-03-040 | 查看障礙詳情 | R | GOV_ADMIN | 工單 ID | 完整資料 + 關聯資訊 | — | SRS-04-011 | §4-3 D-5 | GET /v1/auth/faults/{id} |
| FN-03-041 | 解決障礙 | U | GOV_ADMIN, OPERATOR | 工單 ID | 更新結果 | 狀態→RESOLVED | SRS-04-011 | §4-3 D-5 | PUT /v1/auth/faults/{id}/resolve |
| FN-03-042 | 關聯障礙列表 | R | GOV_ADMIN | 分頁 | 關聯障礙清單 | — | SRS-04-011 | §4-3 D-5 | GET /v1/auth/fault-correlations |
| FN-03-043 | 確認關聯障礙 | U | GOV_ADMIN | 關聯 ID | 更新結果 | DETECTED→CONFIRMED | SRS-04-011 | §4-3 D-5 | PUT /v1/auth/fault-correlations/{id}/confirm |
| FN-03-044 | 解決關聯障礙 | U | GOV_ADMIN | 關聯 ID | 更新結果 | CONFIRMED→RESOLVED；子工單批次解決 | SRS-04-011 | §4-3 D-5 | PUT /v1/auth/fault-correlations/{id}/resolve |
| FN-03-045 | 回路關聯偵測（被動） | P | 系統 | 新工單建立 | 關聯結果 | 同回路 30 分鐘內 ≥3 筆 → 建 fault_correlation (CIRCUIT) | SRS-04-011 | §4-3 D-5 | (內部) |
| FN-03-046 | Gateway 心跳偵測（主動） | P | 系統 | 每 5 分鐘排程 | 告警/關聯 | Gateway 心跳超時 → AUTO_ALERT 工單 + fault_correlation (GATEWAY) | SRS-04-011 | §4-3 D-5 | (排程) |
| FN-03-047 | SIM 到期預警 | P | 系統 | 每日 09:00 排程 | 推送通知 | sim_expiry ≤ 30 天 → 預警（不建工單） | SRS-04-011 | §4-3 D-5 | (排程) |

### 資產異動 (§4-4)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-048 | 資產加帳申請 | C | GOV_ADMIN | 設備資料 | 異動申請 | 啟動 ASSET_CHANGE 流程 | SRS-04-012 | §4-4 | POST /v1/auth/asset-changes |
| FN-03-049 | 資產除帳申請 | C | GOV_ADMIN | 設備 ID 清單、原因 | 異動申請 | 啟動 ASSET_CHANGE 流程 | SRS-04-012 | §4-4 | POST /v1/auth/asset-changes |
| FN-03-050 | 資產變更申請 | C | GOV_ADMIN | 設備 ID、變更內容 | 異動申請 | before/after snapshot；啟動 ASSET_CHANGE 流程 | SRS-04-012 | §4-4 | POST /v1/auth/asset-changes |
| FN-03-051 | 批次異動申請 | C | GOV_ADMIN | 設備 ID 清單、異動類型、內容 | 批次結果 | — | SRS-04-012 | §4-4 | POST /v1/auth/asset-changes/batch |
| FN-03-052 | 異動申請列表 | R | GOV_ADMIN, GOV_MGR | 狀態、類型、分頁 | 申請清單 | — | SRS-04-012 | §4-4 | GET /v1/auth/asset-changes |

### 設備匯出 (§4-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-053 | 資產清冊匯出 (CSV) | E | GOV_ADMIN | 篩選條件 | CSV 檔案 | 全欄位含 JSONB 展開；UTF-8 BOM | SRS-04-013 | §4-5 | GET /v1/auth/devices/export?format=csv |
| FN-03-054 | 資產清冊匯出 (XLSX) | E | GOV_ADMIN | 篩選條件 | XLSX 檔案 | 同上 | SRS-04-013 | §4-5 | GET /v1/auth/devices/export?format=xlsx |
| FN-03-055 | 資產清冊匯出 (ODS) | E | GOV_ADMIN | 篩選條件 | ODS 檔案 | 同上 | SRS-04-013 | §4-5 | GET /v1/auth/devices/export?format=ods |

### 驗收接管文件 (§4-6)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-056 | 上傳驗收文件 | C | GOV_ADMIN | 契約 ID、檔案(竣工圖/規格文件/手冊) | 文件資料 | 以連結方式儲存；病毒掃描 | SRS-04-014 | §4-6 | POST /v1/auth/contracts/{id}/documents |
| FN-03-057 | 驗收文件列表 | R | GOV_ADMIN | 契約 ID | 文件清單 | — | SRS-04-014 | §4-6 | GET /v1/auth/contracts/{id}/documents |
| FN-03-058 | 下載驗收文件 | R | GOV_ADMIN | 文件 ID | 檔案 | — | SRS-04-014 | §4-6 | GET /v1/auth/documents/{id}/download |
| FN-03-059 | 刪除驗收文件 | D | GOV_ADMIN | 文件 ID | 刪除結果 | — | SRS-04-014 | §4-6 | DELETE /v1/auth/documents/{id} |

### 統計 (§4-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-03-060 | 設備分類統計 | R | GOV_ADMIN, GOV_MGR | 維度(類型/狀態/部門/契約) | 統計數據 | — | SRS-04-013 | §4-5 | GET /v1/auth/devices/statistics |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 設備管理 | /admin/asset/devices | 設備 CRUD + 篩選 + expandable | FN-03-001~010 |
| 設備地圖 | /admin/asset/map | GIS 地圖 + 圖層 + 搜尋 | FN-03-013~018 |
| 回路管理 | /admin/asset/circuits | 回路 CRUD | FN-03-024~028 |
| 拓撲檢視 | /admin/asset/topology | 樹狀結構 + 在線狀態 | FN-03-011~012 |
| 契約管理 | /admin/asset/contracts | 契約 CRUD + 資產明細 | FN-03-029~037 |
| 障礙工單 | /admin/asset/faults | 障礙工單 + 關聯障礙 | FN-03-038~047 |
| 資產異動 | /admin/asset/changes | 異動申請 + 審核 | FN-03-048~052 |
| 驗收文件 | /admin/asset/documents | 文件上傳下載 | FN-03-056~059 |
