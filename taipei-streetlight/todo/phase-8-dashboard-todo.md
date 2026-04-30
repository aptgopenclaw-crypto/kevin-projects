# Phase 8 — 儀表板 (Dashboard) TODO

> **建立日期**: 2026-04-27  
> **最後更新**: 2026-04-28  
> **甘特圖**: 08/03 – 09/05 (5 週)  
> **前置**: Phase 6 KPI 完成 ✅、Phase 7 IoT（部分 Widget 依賴）、Phase 5C GIS 基礎  
> **執行計畫**: 99-plan/2026-04-24-execution-plan.md §Phase 8  
> **關鍵路徑**: 8a → 8d-1 → 8b（後端先行）；8d-2/8d-3 ∥ 8c（並行）→ 8e → 8f → 8g  
> **里程碑**: m4 — 2026-09-05

### 技術決策 (已確認 2026-04-27)

| # | 決策項目 | 結論 | 說明 |
|---|---------|------|------|
| D1 | Grid Layout 套件 | **grid-layout-plus** | Vue 3 原生支援，社群最活躍，vue-grid-layout 官方 Vue 3 fork |
| D2 | WebSocket 框架 | **Spring WebSocket + STOMP** | tenant 級 topic 分發 `/topic/tenant/{id}/dashboard`，spring-boot-starter-websocket 已在 pom.xml |
| D3 | Widget 數據快取 | **Spring Cache (ConcurrentMapCache)** | 先用 Spring 內建快取 @Cacheable + TTL 5 分鐘，後續有 Redis 再切換 |
| D4 | Stub Service 模式 | **條件判斷 + 提示** | 未啟用模組的 Widget 顯示「此功能需待智能路燈模組啟用」，非空白或假資料 |
| D5 | 地圖引擎 | **沿用 OpenLayers** | Phase 5C 已選定，PostGIS 已啟用 (V49) |

### 進度總覽

| 區塊 | 進度 | 說明 |
|------|------|------|
| 8a DB + Entity + Enum | ✅ 完成 | V60+V61 migration, 2 entity, 2 repo, 權限+選單, WidgetType enum, ErrorCode, AuditEvent |
| 8d-1 LayoutService + Controller | ✅ 完成 | 版面 CRUD 3 端點 + DashboardLayoutService (GET/PUT/POST reset) |
| 8d-2 7 Widget Services (真實數據) | ✅ 完成 | Maintenance/Fault/Kpi/Device/Outage/Attachment + WidgetDataController 18 端點 |
| 8d-3 4 Stub Widget Services | ✅ 完成 | PanelBox/Electricity/Meter/GIS stub 7 端點 + WidgetUnavailableResponse |
| 8b 前端框架 + DashboardView | ✅ 完成 | grid-layout-plus + WidgetContainer + widgetRegistry + 8 widget 元件 + API + types + i18n + router |
| 8c 11 Widget 元件 | ⬜ 未開始 | 進階功能: drill-down, 多圖表 tab, 篩選器, 共用元件 (ChartWrapper/StatCard/TimeRangeFilter) |
| 8e 自訂版面進階功能 | ⬜ 未開始 | Widget 選擇器 + 分頁 + 配色主題 |
| 8f WebSocket 即時推送 | ⬜ 未開始 | STOMP /ws/dashboard + 3 類事件推送 |
| 8g 測試 | ⬜ 未開始 | 24 TC |

### 調整後執行順序 (後端先行)

```
8a DB + Entity + Enum (2d) ✅ done 04/27
  ├→ 8d-1 LayoutService + Controller (2d) ✅ done 04/27
  ├→ 8d-2 7 個可用 Widget Service (5d) ✅ done 04/27
  └→ 8d-3 4 個 stub Widget Service (1d) ✅ done 04/27
  ├→ 8b 前端框架 + DashboardView (3d) ✅ done 04/28
  ├→ 8c Widget 元件進階功能 (10d) ⬜
  ├→ 8e 自訂版面進階功能 (3d) ⬜
  ├→ 8f WebSocket (3d) ⬜
  └→ 8g 測試 (3d) ⬜
```

### Widget 可用性分類

| 分類 | Widget | 數據源 | 備註 |
|------|--------|--------|------|
| ✅ 真實數據 | maintenance-stats | repair_tickets + inspection_records | 欄位齊全 |
| ✅ 真實數據 | fault-heatmap | fault_tickets JOIN devices(lat/lng) | PostGIS V49 |
| ✅ 真實數據 | kpi-summary | kpi_results + kpi_indicators | Phase 6 完成 |
| ✅ 真實數據 | lamp-count | devices GROUP BY type/status | 欄位齊全 |
| ✅ 真實數據 | outage-alert | fault_correlations (POWER_OUTAGE) | 表已存在 |
| ✅ 真實數據 | attachments | ticket_attachments | 欄位齊全 |
| ✅ 降級可用 | lamp-status | devices.status + lastHeartbeatAt | IoT 完成前用靜態狀態 |
| ⏳ Stub 提示 | panel-box | 無 telemetry | 顯示「模組未啟用」 |
| ⏳ Stub 提示 | electricity-cost | 無電費表 | 顯示「模組未啟用」 |
| ⏳ Stub 提示 | meter | 無 meter_readings | 顯示「模組未啟用」 |
| ⚠️ 部分可用 | gis-overview | 靜態圖層可用，即時圖層待 IoT | 設備+故障圖層先做 |

### 跨模組依賴

| Widget | 依賴模組 | 依賴狀態 | 備註 |
|--------|---------|---------|------|
| maintenance-stats | Phase 4 報修 (repair_tickets) | ✅ 已完成 | 查詢 repair + inspection |
| outage-alert | fault_correlations (已存在) | ✅ 降級可用 | rootCauseType=POWER_OUTAGE 查詢 |
| fault-heatmap | Phase 4 報修 + Phase 5C GIS | ⬜ Phase 5C | GeoJSON 熱力圖需 PostGIS 空間查詢 |
| kpi-summary | Phase 6 KPI (kpi_results) | ✅ 已完成 | 直接查詢 kpi_results 表 |
| lamp-count | Phase 4 資產 (devices) | ✅ 已完成 | GROUP BY type, district |
| lamp-status | devices.status + lastHeartbeatAt | ✅ 降級可用 | IoT 完成前用靜態 status 統計 |
| panel-box | Phase 7 IoT (telemetry) | ⬜ Stub | 顯示「此功能需待智能路燈模組啟用」(D4) |
| attachments | Phase 4 通用 (ticket_attachments) | ✅ 已完成 | 統計檔案數量/容量 |
| electricity-cost | Phase 7 IoT (telemetry) + 外部匯入 | ⬜ Stub | 顯示「此功能需待智能路燈模組啟用」(D4) |
| meter | Phase 7 IoT (telemetry) | ⬜ Stub | 顯示「此功能需待智能路燈模組啟用」(D4) |
| gis-overview | Phase 5C GIS + Phase 4 + Phase 7 | ⬜ Phase 5C/7 | 多圖層 GeoJSON 聚合 |

---

## 8a — DB Schema (2 天, 08/03–08/04)

> **SD**: SD-10-dashboard.md §1  
> **SRS**: SRS-11-001 (AC-11-001-1~6)  
> **SA**: FN-10-001~003 (版面管理)

### 任務

#### Flyway Migration

- [x] **V60 Flyway migration** — 建立 2 張表 + 索引 ✅ 04/27
  - `dashboard_layouts` — 使用者個人版面 (tenant_id, user_id, layout_json JSONB, is_default)
    - UK(tenant_id, user_id) — 每位使用者一組版面
    - layout_json 格式: `[{i, x, y, w, h, widget}]` (vue-grid-layout)
  - `dashboard_default_layouts` — 預設版面範本 (tenant_id, role_type, layout_json JSONB)
    - role_type: ADMIN / MANAGER / CHIEF / null=global
- [x] **FK 約束** — tenant_id → tenant(tenant_id) ✅ 04/27
- [x] **索引** — dashboard_layouts(tenant_id, user_id), dashboard_default_layouts(tenant_id, role_type) ✅ 04/27

#### JPA Entity

- [x] **DashboardLayout entity** — @Filter("tenantFilter"), TenantAware ✅ 04/27
  - id, tenantId, userId, layoutJson (String, @Column(columnDefinition="jsonb")), isDefault
  - @EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
  - @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
- [x] **DashboardDefaultLayout entity** — @Filter("tenantFilter"), TenantAware ✅ 04/27
  - id, tenantId, roleType (String), layoutJson (String), createdAt

#### Repository

- [x] **DashboardLayoutRepository** — findByTenantIdAndUserId(tenantId, userId) ✅ 04/27
- [x] **DashboardDefaultLayoutRepository** — findByTenantIdAndRoleType(tenantId, roleType) ✅ 04/27

#### 權限 + 選單

- [x] **V61 權限 migration** — DASHBOARD_VIEW, DASHBOARD_MANAGE (版面管理) ✅ 04/27
- [x] **選單項目** — 儀表板 (主頁面，/admin/dashboard) ✅ 04/27
- [x] **角色綁定** — 所有角色 DASHBOARD_VIEW; ADMIN/DEPT_ADMIN DASHBOARD_MANAGE ✅ 04/27

#### Enum + Audit

- [x] **WidgetType enum** — MAINTENANCE_STATS, OUTAGE_ALERT, FAULT_HEATMAP, KPI_SUMMARY, LAMP_COUNT, LAMP_STATUS, PANEL_BOX, ATTACHMENTS, ELECTRICITY_COST, METER, GIS_OVERVIEW ✅ 04/27
- [x] **AuditEventType** — UPDATE_DASHBOARD_LAYOUT, RESET_DASHBOARD_LAYOUT ✅ 04/27
- [x] **AuditCategory** — 新增 DASHBOARD 分類 ✅ 04/27
- [x] **ErrorCode** — DASHBOARD_LAYOUT_NOT_FOUND, DASHBOARD_DEFAULT_NOT_FOUND ✅ 04/27

#### 預設版面種子資料

- [ ] **V60 seed data** — 至少 3 種預設版面範本
  - ADMIN 版面: 全部 11 Widget (2×6 grid)
  - MANAGER 版面: KPI + 養護 + 路燈 + 電費 (重點指標)
  - CHIEF 版面: KPI 摘要 + GIS 總覽 + 養護統計 (高階總覽)

---

## 8b — vue-grid-layout 框架 (5 天, after 8a)

> **SRS**: SRS-11-001 (AC-11-001-1~6)  
> **SA**: FN-10-001~003  
> **SD**: SD-10-dashboard.md §3~4  
> **技術**: vue-grid-layout (Vue 3 compatible) + ECharts

### 任務

#### 依賴安裝

- [x] **npm install grid-layout-plus** — Vue 3 拖曳式格線佈局 (D1 已確認) ✅ 04/28 v1.1.1
- [x] **echarts + vue-echarts** — 已在 package.json (echarts 6 + vue-echarts 8) ✅

#### 前端基礎架構

- [x] **types/dashboard.ts** — TypeScript 介面定義 ✅ 04/28
  - `WidgetConfig` — { i: string, x: number, y: number, w: number, h: number, widget: WidgetType }
  - `DashboardLayout` — { widgets: WidgetConfig[], theme?: string }
  - `WidgetType` — enum 字串聯合型別 (11 種)
  - 各 Widget 數據 Response 型別 (MaintenanceStats, OutageAlert, FaultHeatmap, KpiSummary, LampCount, LampStatus, PanelBox, AttachmentStats, ElectricityCost, MeterReading, GisOverview)
- [x] **api/dashboard/index.ts** — API 函式 ✅ 04/28
  - `getLayout()` — GET /v1/auth/dashboard/layout
  - `saveLayout(layout)` — PUT /v1/auth/dashboard/layout
  - `resetLayout()` — POST /v1/auth/dashboard/layout/reset
  - 18 個 Widget 數據 API 函式 (對應 18 端點)
- [x] **i18n** — zh-TW dashboard 相關鍵值 (~70 key) ✅ 04/28
  - widget 名稱、圖表標籤、篩選器、工具提示、操作按鈕

#### 儀表板主頁面

- [x] **views/admin/dashboard/DashboardView.vue** — 主頁面 ✅ 04/28
  - vue-grid-layout 容器 (拖曳 + 調整大小)
  - 載入流程: getLayout() → 渲染 grid → 各 Widget 平行載入數據
  - 編輯模式 toggle (拖曳啟用/鎖定)
  - 儲存版面按鈕 → saveLayout()
  - 重置按鈕 → resetLayout()
- [x] **Widget 容器元件** — `WidgetContainer.vue` ✅ 04/28
  - 統一外框 (el-card + header + loading state + error state)
  - slot 機制: 各 Widget 實作插入
  - 全螢幕按鈕 (el-dialog 展開)
  - 刷新按鈕
  - drill-down 點擊 handler → router.push 至明細頁
- [x] **Widget 註冊機制** — `widgetRegistry.ts` ✅ 04/28
  - 動態元件載入 (defineAsyncComponent)
  - WidgetType → Component 映射
  - 各 Widget 預設尺寸 (defaultW, defaultH, minW, minH)

#### 路由

- [x] **router** — /admin/dashboard 路由 (lazy import DashboardView) ✅ 04/28
  - 設為登入後預設首頁（如需求確認）

> **✅ 8b 完成確認**: `vue-tsc --noEmit` 零錯誤通過 (04/28)

---

## 8c — 10 類 Widget 元件 (15 天, after 8b)

> **SRS**: SRS-11-002~010 (各 Widget 驗收準則)  
> **SA**: FN-10-004~022 (Widget 功能)  
> **SD**: SD-10-dashboard.md §2 Widget 清單

### 任務

#### Widget 1: 養護統計 (maintenance-stats) — 3d

- [~] **MaintenanceStatsWidget.vue** — FN-10-004~005 (基礎版已建立 04/28，待進階功能)
  - 數字卡片: 本年/本月報修數量、完修數量、待修數量、完修率
  - 平均修復時間 (hr)
  - 照明妥善率 (%)
  - 報修來源分布 (圓餅圖 ECharts)
  - 故障分類分布 (長條圖 ECharts)
  - 可區分廠商 (contract 下拉篩選)
  - 趨勢圖 tab (月/季/年切換)
  - **數據來源**: GET /widgets/maintenance + /widgets/maintenance/trend

#### Widget 2: 停電告警 (outage-alert) — 1d

- [~] **OutageAlertWidget.vue** — FN-10-006~007 (基礎版已建立 04/28，待趨勢圖+WebSocket)
  - 當前停電區域數量 (即時卡片)
  - 停電區域列表 (區域名 + 影響範圍)
  - 歷史趨勢圖 (月停電次數折線圖)
  - 即時推送 via WebSocket (widget="outage-alert")
  - **數據來源**: GET /widgets/outage + /widgets/outage/trend + WebSocket

#### Widget 3: 故障熱力圖 (fault-heatmap) — 2d

- [~] **FaultHeatmapWidget.vue** — FN-10-008~009 (基礎版已建立 04/28，用圓餅圖暫代熱力圖，待 OpenLayers)
  - 地圖嵌入 (OpenLayers / Leaflet heatmap layer)
  - GeoJSON 熱力數據渲染 (依故障密度著色)
  - 時間範圍篩選器 (el-date-picker range)
  - 故障分類統計 (側欄圓餅圖)
  - drill-down: 點擊區域 → 故障列表
  - **數據來源**: GET /widgets/fault-heatmap + /widgets/fault-category
  - **注意**: 依賴 Phase 5C GIS (PostGIS 空間查詢)；降級方案用 el-table 列表

#### Widget 4: KPI 績效卡片 (kpi-summary) — 1d

- [~] **KpiSummaryWidget.vue** — FN-10-010~011 (基礎版已建立 04/28，待儀表圖+趨勢圖)
  - KPI 核心指標卡片 (妥善率 / 回應時間 / 完成率)
  - 達標/未達標色彩 (綠/紅/黃 漸層)
  - 進度條 / 儀表圖 (ECharts gauge)
  - 趨勢圖 (近 6 個月折線圖)
  - drill-down: 點擊 → /admin/kpi/reports
  - **數據來源**: GET /widgets/kpi + /widgets/kpi/trend
  - **注意**: 依賴 Phase 6 KPI ✅ 已完成

#### Widget 5: 路燈數量 (lamp-count) — 1d

- [~] **LampCountWidget.vue** — FN-10-012 (基礎版已建立 04/28，待多圖表+drill-down)
  - 路燈總盞數 (大數字卡片)
  - 各廠商維護盞數及比例 (圓餅圖)
  - 一般路燈 vs 智能路燈 (圓環圖)
  - 路燈/園燈/其他照明設施 (堆疊長條圖)
  - 各類光源 (LED/鈉燈/水銀燈...) 盞數 (長條圖)
  - drill-down: 點擊 → /admin/asset/devices
  - **數據來源**: GET /widgets/lamp-count

#### Widget 6: 路燈在線/離線 (lamp-status) — 1d

- [~] **LampStatusWidget.vue** — FN-10-013 (基礎版已建立 04/28，待趨勢圖+WebSocket)
  - 在線/離線數量 (數字 + 百分比)
  - 在線率趨勢 (折線圖)
  - 即時推送 via WebSocket (widget="lamp-status")
  - **數據來源**: GET /widgets/lamp-status + WebSocket
  - **注意**: 依賴 Phase 7 IoT；降級方案：devices 表 status 欄位

#### Widget 7: 配電箱 (panel-box) — 1d

- [ ] **PanelBoxWidget.vue** — FN-10-014~015
  - 各配電箱用電統計 (kWh 長條圖)
  - 異常告警列表 (跳脫/過載/通訊失敗)
  - 告警等級色彩標記 (severity: critical/warning/info)
  - **數據來源**: GET /widgets/panel-box + /widgets/panel-box/alerts
  - **注意**: 依賴 Phase 7 IoT telemetry

#### Widget 8: 附件統計 (attachments) — 0.5d

- [~] **AttachmentStatsWidget.vue** — FN-10-016 (基礎版已建立 04/28，待 drill-down)
  - 檔案總數 + 總容量 (數字卡片)
  - 依類型分組 (照片/文件/其他) 圓餅圖
  - **數據來源**: GET /widgets/attachments

#### Widget 9: 電費 (electricity-cost) — 1d

- [ ] **ElectricityCostWidget.vue** — FN-10-017~018
  - 月電費金額 (數字卡片 NTD)
  - 近 3 年月電費趨勢 (多折線圖)
  - 包燈 vs 表燈比例 (圓餅圖)
  - 年同期比較 (差異 %)
  - **數據來源**: GET /widgets/electricity-cost + /widgets/electricity-cost/trend
  - **注意**: 依賴 Phase 7 telemetry 或外部匯入

#### Widget 10: 智慧電表 (meter) — 1d

- [ ] **MeterWidget.vue** — FN-10-019~020
  - 各電表最新讀數 (kW + kWh 表格)
  - 用電日趨勢 (折線圖)
  - **數據來源**: GET /widgets/meter + /widgets/meter/trend
  - **注意**: 依賴 Phase 7 IoT telemetry

#### Widget 11: GIS 總覽 (gis-overview) — 2d

- [ ] **GisOverviewWidget.vue** — FN-10-021~022
  - 多圖層地圖 (OpenLayers)
    - 路燈圖層 (設備點位)
    - 故障圖層 (故障標記)
    - 停電圖層 (停電區域)
  - 圖層切換 toggle
  - 地圖縮放 + 拖曳 + 聚合 (WebGL 16 萬點)
  - 點擊設備 → popup 詳情
  - 即時推送 via WebSocket (widget="gis-overview", 設備上下線 + 停電區域)
  - **數據來源**: GET /widgets/gis + WebSocket
  - **注意**: 依賴 Phase 5C GIS (OpenLayers + PostGIS)；降級方案：簡化 marker 列表

#### 共用元件

- [ ] **ChartWrapper.vue** — ECharts 統一封裝 (resize observer + loading + theme)
- [ ] **StatCard.vue** — 數字卡片元件 (title, value, unit, trend icon, color)
- [ ] **TimeRangeFilter.vue** — 時間範圍篩選器 (今日/本週/本月/本季/本年/自訂)

---

## 8d — 後端統計 API (10 天, after 8a, 與 8b/8c 並行)

> **SA**: FN-10-004~022 (Widget 數據端點)  
> **SD**: SD-10-dashboard.md §3~4  
> **Test**: TS-10-dashboard.md §2~9

### 任務

#### Controller (2)

- [x] **DashboardLayoutController** — FN-10-001~003 ✅ 04/27
  - `GET /v1/auth/dashboard/layout` — 取得個人版面 (@PreAuthorize DASHBOARD_VIEW)
  - `PUT /v1/auth/dashboard/layout` — 儲存版面 (@PreAuthorize DASHBOARD_VIEW)
  - `POST /v1/auth/dashboard/layout/reset` — 重置為預設 (@PreAuthorize DASHBOARD_VIEW)
  - @AuditEvent: UPDATE_DASHBOARD_LAYOUT, RESET_DASHBOARD_LAYOUT
- [x] **WidgetDataController** — FN-10-004~021 ✅ 04/27
  - 18 個 GET 端點 (各 Widget 數據)
  - 所有端點 @PreAuthorize DASHBOARD_VIEW
  - 統一時間範圍參數: startDate, endDate (LocalDate)
  - 部分端點: contractId, district 等篩選參數

#### DTO

- [x] **LayoutRequest** — layoutJson (String, @NotNull) ✅ 04/27
- [x] **LayoutResponse** — id, layoutJson, isDefault, updatedAt ✅ 04/27
- [x] **MaintenanceStatsResponse** — totalRepairs, completedRepairs, pendingRepairs, completionRate, avgRepairHours, illuminationRate, sourceDistribution (Map), faultCategoryDistribution (Map) ✅ 04/27
- [x] **MaintenanceTrendResponse** — months[] { month, repairCount, completionRate } ✅ 04/27
- [x] **OutageAlertResponse** — currentOutageCount, outageZones[] { zone, affectedCount, since } ✅ 04/27
- [x] **OutageTrendResponse** — months[] { month, outageCount, avgRecoveryHours } ✅ 04/27
- [ ] **FaultHeatmapResponse** — GeoJSON FeatureCollection (type, features[]) — 用 Map<String,Object> 暫代
- [x] **FaultCategoryResponse** — categories[] { category, count, percentage } ✅ 04/27
- [x] **KpiSummaryResponse** — indicators[] { code, name, value, target, achievement, grade } ✅ 04/27
- [x] **KpiTrendResponse** — months[] { month, indicators[] { code, value } } ✅ 04/27
- [x] **LampCountResponse** — total, byContractor (Map), byType (Map), byLightSource (Map), byFacilityType (Map) ✅ 04/27
- [x] **LampStatusResponse** — online, offline, onlineRate, updatedAt ✅ 04/27
- [ ] **PanelBoxResponse** — boxes[] { boxId, name, kWh, status } — stub
- [ ] **PanelBoxAlertResponse** — alerts[] { boxId, name, alertType, severity, detectedAt } — stub
- [x] **AttachmentStatsResponse** — totalCount, totalSizeMB, byType (Map) ✅ 04/27
- [ ] **ElectricityCostResponse** — currentMonthCost, contractLampCost, meterLampCost, ratio — stub
- [ ] **ElectricityCostTrendResponse** — years[] { year, months[] { month, cost } } — stub
- [ ] **MeterReadingResponse** — meters[] { meterId, name, kW, kWh, lastReadAt } — stub
- [ ] **MeterTrendResponse** — days[] { date, totalKWh } — stub
- [ ] **GisOverviewResponse** — GeoJSON FeatureCollection (multi-layer), layerMeta[] — stub
- [x] **WidgetUnavailableResponse** — widgetType, available, message (stub 統一回應) ✅ 04/27

#### Service — 版面管理

- [x] **DashboardLayoutService** ✅ 04/27
  - `getLayout(userId)` — 查詢個人版面；無則回傳角色預設版面；無角色預設則回傳全域預設
  - `saveLayout(userId, request)` — UPSERT 個人版面 (layout_json)
  - `resetLayout(userId)` — 刪除個人版面，回傳預設版面
  - `getDefaultLayout(roleType)` — 查詢預設版面

#### Service — 養護統計

- [x] **WidgetMaintenanceService** — FN-10-004~005 ✅ 04/27
  - `getStats(startDate, endDate, contractId)` — 查詢 repair_tickets 統計
    - COUNT(status=COMPLETED), COUNT(status!=CLOSED), AVG(completed_at - created_at)
    - GROUP BY source, GROUP BY fault_category
  - `getTrend(startDate, endDate, contractId)` — 月度趨勢
  - `getIlluminationRate(contractId)` — 照明妥善率 = active_devices / total_devices

#### Service — 停電

- [x] **WidgetOutageService** — FN-10-006~007 ✅ 04/27
  - `getCurrentOutages()` — 查詢 fault_correlations (active)
  - `getOutageTrend(startDate, endDate)` — 月停電次數 + 平均恢復時間
  - **注意**: Phase 7 完成前用 stub/mock 回空

#### Service — 故障

- [x] **WidgetFaultService** — FN-10-008~009 ✅ 04/27
  - `getHeatmapData(startDate, endDate, bounds)` — 空間查詢 → GeoJSON FeatureCollection
    - Phase 5C 完成前降級: 依行政區分組回傳計數
  - `getCategoryStats(startDate, endDate)` — GROUP BY fault_category

#### Service — KPI

- [x] **WidgetKpiService** — FN-10-010~011 ✅ 04/27
  - `getSummary(periodYear, periodMonth)` — 查詢 kpi_results (核心指標)
  - `getTrend(indicatorId, months)` — 近 N 個月趨勢
  - **直接查詢**: Phase 6 已建立的 kpi_results 表 ✅

#### Service — 設備

- [x] **WidgetDeviceService** — FN-10-012~013 ✅ 04/27
  - `getLampCount(district, contractId)` — devices GROUP BY type, light_source, facility_type, contract_id
  - `getLampStatus()` — 在線/離線統計
    - Phase 7 完成前: 從 devices.status 統計

#### Service — 配電箱

- [ ] **WidgetPanelBoxService** — FN-10-014~015
  - `getUsageStats(district)` — 配電箱用電 (kWh)
  - `getAlerts()` — 異常告警 (severity + type)
  - **注意**: 依賴 Phase 7 IoT telemetry；Phase 7 前用 stub

#### Service — 附件

- [x] **WidgetAttachmentService** — FN-10-016 ✅ 04/27
  - `getStats()` — COUNT(*) + SUM(file_size) GROUP BY file_type

> **✅ 8d 後端完成確認**: `mvn compile -q` BUILD SUCCESS (04/27)

#### Service — 電費

- [ ] **WidgetElectricityService** — FN-10-017~018
  - `getMonthlyCost(year, month)` — 月電費 (包燈/表燈)
  - `getCostTrend(years)` — 近 N 年月度趨勢
  - **注意**: 依賴 Phase 7 或外部匯入

#### Service — 電表

- [ ] **WidgetMeterService** — FN-10-019~020
  - `getReadings()` — 各電表最新讀數
  - `getTrend(meterId, startDate, endDate)` — 日趨勢
  - **注意**: 依賴 Phase 7 IoT

#### Service — GIS

- [ ] **WidgetGisService** — FN-10-021
  - `getOverview(bounds, layers[])` — 多圖層 GeoJSON 聚合
    - 路燈圖層: DeviceService.findByBounds()
    - 故障圖層: FaultTicketService.findOpenByBounds()
    - 停電圖層: FaultCorrelationService.findActiveByBounds()
  - merge → FeatureCollection (multi-layer)
  - **注意**: 依賴 Phase 5C GIS；降級方案: 簡化為列表

---

## 8e — 使用者自訂版面 (5 天, after 8c)

> **SRS**: SRS-11-001 (AC-11-001-1~6)  
> **SA**: FN-10-001~003  
> **SD**: SD-10-dashboard.md §5.1

### 任務

#### 版面個人化流程

- [x] **首次訪問** — 無個人版面 → 載入角色預設版面 → 自動顯示 ✅ 04/28 (DashboardView fallback 已實作)
- [x] **拖曳調整** — vue-grid-layout drag + resize → 即時更新 grid ✅ 04/28 (editing mode)
- [x] **儲存版面** — 編輯模式下拖曳後 → 點擊「儲存」 → PUT /dashboard/layout ✅ 04/28
- [x] **版面重置** — 點擊「重置」 → confirm dialog → POST /dashboard/layout/reset → 回到預設 ✅ 04/28
- [x] **Widget 新增/移除** — Widget 選擇器 drawer ✅ 04/28 (基礎版)
  - 可用 Widget 列表 (11 種)
  - 勾選/取消 → 新增/移除 Widget
  - 新增 Widget 自動放置 (找到空位)
- [x] **分頁管理** — 多個儀表板頁面 tab (AC-11-001-4) ✅ 04/26
  - 新增/刪除/重命名分頁 (el-tabs + ElMessageBox.prompt)
  - 每頁獨立 grid layout (PageTab key-based)
  - layoutJson v2 格式 (backward compat v1 flat array)
  - 編輯取消 → snapshot restore

#### 配色/風格選擇

- [x] **主題選擇** — 至少 3 種配色方案供機關選定 (AC-11-001-1) ✅ 04/26
  - 預設主題 (淺色) — 預設 CSS variables
  - 深色主題 (dark mode) — theme-dark CSS variable overrides
  - 機關自訂主題 (primary color 可調) — el-color-picker + color-mix()
- [x] **ECharts 主題同步** — 圖表配色隨儀表板主題切換 ✅ 04/26
  - provide/inject ECHARTS_THEME_KEY → ChartWrapper → VChart :theme

---

## 8f — WebSocket 即時推送 (3 天, after 8d)

> **SRS**: SRS-11-001 技術設計 (WebSocket/SSE)  
> **SD**: SD-10-dashboard.md §5.3  
> **ADR**: ADR-001-polling-vs-websocket.md

### 任務

#### 後端 WebSocket 端點

- [x] **DashboardPushService** — STOMP 推送服務 ✅ 04/26
  - 共用既有 WebSocketConfig (`/ws` endpoint, STOMP + SockJS)
  - 推送到 `/topic/tenant/{tenantId}/dashboard`
  - DashboardPushMessage: `{widget, data, timestamp}`
- [x] **StompAuthInterceptor 增強** — 提取 tenantId 至 credentials ✅ 04/26
  - CONNECT 時解析 JWT tenantId claim → 存入 auth.credentials
- [x] **DashboardEvent + DashboardEventListener** — Spring ApplicationEvent ✅ 04/26
  - @Async @EventListener → DashboardPushService.pushToTenant()
  - 業務服務 publish DashboardEvent(tenantId, WidgetType, data)

#### 事件監聯 + 推送 (事件發送端 — 待各模組整合)

- [x] **停電事件** — DashboardEvent(OUTAGE_ALERT, outageInfo) ✅ 04/26 (push 機制就緒，發送端待整合)
- [x] **設備狀態變更** — DashboardEvent(LAMP_STATUS, {online, offline}) ✅ 04/26 (同上)
- [x] **GIS 更新** — DashboardEvent(GIS_OVERVIEW, features) ✅ 04/26 (同上，GIS stub widget 待啟用)

#### 前端 WebSocket 連線

- [x] **useDashboardWebSocket composable** — @stomp/stompjs Client ✅ 04/26
  - 自動連線 / 斷線重連 (reconnectDelay 3s, heartbeat 10s)
  - 訊息路由: 依 widget key 分發到 callbacks Map
  - 連線狀態指示 (connected / connecting / reconnecting / disconnected)
  - DashboardView provide → DASHBOARD_WS_KEY inject
- [x] **Widget 即時更新** — outage-alert, lamp-status ✅ 04/26
  - useDashboardWsInject() → onWidgetUpdate(type, callback)
  - 收到推送 → 更新 reactive ref → 自動重渲染
  - 視覺提示: pulse animation (1.5s box-shadow)
  - WS 狀態指示燈 (header 小圓點: 綠/橙/紅)

---

## 8g — 測試 (5 天, after 8e)

> **Test**: TS-10-dashboard.md (24 TC)

### 後端單元測試

#### 版面管理 (4 TC)

- [ ] TC-10-001-01: 載入用戶版面 — GET /dashboard/layout → 200, layout JSON
- [ ] TC-10-001-02: 首次使用 → 回傳預設版面 — GET /dashboard/layout → 200, default layout
- [ ] TC-10-002-01: 拖曳後儲存版面 — PUT /dashboard/layout → 200, layout persisted
- [ ] TC-10-003-01: 重置為預設 — POST /dashboard/layout/reset → 200, default

#### 養護 Widget (2 TC)

- [x] TC-10-004-01: 養護統計數據 — getStats() → total/completed/rate ✅ 04/26
- [x] TC-10-005-01: 養護趨勢圖 — getTrend() → monthly points ✅ 04/26
- [x] TC-10-004-01b: 帶合約篩選 — getStats(start,end,contractId) ✅ 04/26

#### 停電 Widget (2 TC)

- [x] TC-10-006-01: 停電統計 — getCurrentOutages() → zones ✅ 04/26
- [x] TC-10-006-02: 無停電 — getCurrentOutages() → empty ✅ 04/26
- [x] TC-10-007-01: 停電趨勢 — getOutageTrend() → monthly ✅ 04/26

#### KPI Widget (2 TC)

- [x] TC-10-010-01: KPI 摘要卡片 — getSummary() → indicators + grade ✅ 04/26
- [x] TC-10-010-02: KPI 無數據 — getSummary() → empty ✅ 04/26
- [x] TC-10-011-01: KPI 趨勢 — getTrend() → multi-month ✅ 04/26
- [x] TC-10-011-02: KPI 跨年 — getTrend(2025,1,3) → 2024-11~2025-01 ✅ 04/26

#### 路燈 Widget (2 TC)

- [x] TC-10-012-01: 路燈數量統計 — getLampCount() → by contractor/type/light/facility ✅ 04/26
- [x] TC-10-013-01: 在線/離線統計 — getLampStatus() → online/offline/rate ✅ 04/26
- [x] TC-10-013-02: 零設備 — getLampStatus() → rate=0 ✅ 04/26

#### WebSocket 推送 (3 TC)

- [x] TC-10-WS-01: pushToTenant 正確推送 — 驗證 destination + payload ✅ 04/26
- [x] TC-10-WS-02: pushToTenant outage-alert — 驗證 widget key ✅ 04/26
- [x] TC-10-WS-03: pushToTenant 異常不傳播 — template 拋例外 → 不中斷 ✅ 04/26

#### 版面管理 (4 TC)

- [x] TC-10-001-01: 載入用戶版面 → 有個人版面 → 回傳 ✅ 04/26
- [x] TC-10-001-02: 首次使用 → 回傳預設版面 ✅ 04/26
- [x] TC-10-001-03: 首次使用 + 無預設 → 回傳空 ✅ 04/26
- [x] TC-10-002-01: 儲存版面 — 新建 ✅ 04/26
- [x] TC-10-002-02: 儲存版面 — 更新既有 ✅ 04/26
- [x] TC-10-003-01: 重置為預設 — 有預設 ✅ 04/26
- [x] TC-10-003-02: 重置 — 無預設 ✅ 04/26

#### Stub Widget / 前端測試 (待 Phase 7 完成後補充)

- [ ] TC-10-014~015: 配電箱 — stub 模式，待 IoT 模組啟用
- [ ] TC-10-016-01: 附件統計 — 待補充 WidgetAttachmentServiceTest
- [ ] TC-10-017~020: 電費/電表 — stub 模式，待 IoT 模組啟用
- [ ] TC-10-021-01: GIS 總覽 — stub 模式，待 GIS 模組啟用
- [ ] TC-10-022-01: Widget 鑽取 — 前端 E2E 測試

### 測試結果

```
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0 ✅ BUILD SUCCESS
```

### 測試策略

- **後端**: Service 層 @ExtendWith(MockitoExtension.class) 隔離測試 (mock EntityManager + Repository)
- **前端**: 各 Widget 元件 snapshot 測試 + API mock (待補)
- **WebSocket**: DashboardPushService 單元測試 (mock SimpMessagingTemplate) ✅
- **整合**: 全流程 (登入 → 儀表板載入 → Widget 渲染 → 拖曳 → 儲存) (待 E2E)

---

## 文件追溯

| 文件 | 路徑 | 用途 |
|------|------|------|
| 需求規格 | 02-spec/11-dashboard.md | 原始需求 §11-(1)~(2) |
| SRS | 03-srs/SRS-11-dashboard.md | 驗收準則 AC-11-001 ~ AC-11-010 |
| SA | 05-sa/SA-10-dashboard.md | Function List FN-10-001 ~ FN-10-022 |
| SD | 06-sd/SD-10-dashboard.md | DB Schema + Class Structure + API Contract |
| Test | 09-test-spec/TS-10-dashboard.md | 24 TC |
| ADR | 99-adr/ADR-001-polling-vs-websocket.md | WebSocket vs Polling 決策 |
| 甘特圖 | 99-plan/2026-04-24-gantt.md | Phase 8 時程 |

---

## 風險與待決事項

| # | 風險/議題 | 影響 | 緩解方案 |
|---|---------|------|---------|
| R1 | Phase 7 IoT 未完成：panel-box/electricity-cost/meter Widget 無數據 | 3 個 Widget 顯示提示 | D4 已確認：stub service + 前端「模組未啟用」提示，Phase 7 完成後替換 |
| R2 | Phase 5C GIS 未完成時，fault-heatmap 和 gis-overview 無空間查詢 | 2 個 Widget 無地圖 | 降級方案: 用行政區分組表格替代熱力圖，延後地圖功能 |
| R3 | vue-grid-layout Vue 3 相容性 | — | D1 已確認 grid-layout-plus，風險已消除 ✅ |
| R4 | 11 個 Widget 各有不同數據源，後端查詢效能 | 儀表板首頁載入慢 | D3 已確認：Spring Cache @Cacheable TTL 5min；Widget 平行載入 |
| R5 | WebSocket 大量連線記憶體消耗 | 100+ 同時連線壓力 | 連線池限制 + 事件節流 (throttle 5s) |
| R6 | 16 萬路燈點位 GIS 渲染效能 | GIS Widget 卡頓 | WebGL 聚合 (cluster) + 只載入可視範圍 |

### 技術決策 (已確認 ✅)

| # | 決策項目 | 結論 | 確認日 |
|---|---------|------|--------|
| D1 | Grid Layout 套件 | **grid-layout-plus** | 2026-04-27 |
| D2 | WebSocket 框架 | **Spring WebSocket + STOMP** | 2026-04-27 |
| D3 | Widget 數據快取 | **Spring Cache (ConcurrentMapCache)** → 後續切 Redis | 2026-04-27 |
| D4 | Stub Service 模式 | **條件判斷 + 「模組未啟用」提示** | 2026-04-27 |
| D5 | 地圖引擎 | **沿用 OpenLayers** (Phase 5C 已選) | 2026-04-27 |
