# Phase 5C — GIS 地圖基礎建設 TODO

> **建立日期**: 2026-04-25  
> **最後更新**: 2026-04-25 (5c5 完成)  
> **甘特圖**: 05/05 – 06/08 (5 週, 與 5A/5B 並行)  
> **前置**: Phase 1~4 全部完成 ✅, PostGIS 已啟用 (V49) ✅  
> **執行計畫**: 99-plan/2026-04-24-execution-plan.md §Phase 5C  
> **關鍵路徑**: ⚠️ Phase 7 IoT 與 Phase 8 儀表板 依賴 5C 完成  
> **技術選型**: 99-adr/ADR-003-gis-open-source.md

### 進度總覽

| 區塊 | 進度 | 說明 |
|------|------|------|
| 5c1 PostGIS 啟用 + 空間欄位 | ✅ 完成 | V49 migration, GiST index, 自動同步觸發器 |
| 5c2 OpenLayers PoC + NLSC WMTS | ✅ 完成 | GisMapView.vue, 3 API 端點, WMTS + OSM fallback |
| 5c3 Proj4js 坐標轉換整合 | ✅ 完成 | CoordinateService + coordinateUtils.ts + 三坐標 UI |
| 5c4 設備點位圖層 (16萬點 WebGL) | ✅ 完成 | WebGLPointsLayer + Cluster + DragBox 框選 |
| 5c5 分區範圍 + 空間查詢 | ✅ 完成 | 行政區 polygon + 分區查詢 + 半徑搜尋 UI |
| 5c6 GML 匯出匯入 | ✅ 完成 | GML 3.2 匯出 + CSV Open Data 匯出 + GML 匯入 (diff + confirm) |
| 5c7 街景連結 | ✅ 完成 | Google Street View URL 外連 (Popup + Drawer)，無需 API Key |

### 甘特依賴

```
5c1 PostGIS (3d) ✅ ─┐
                     ├──→ 5c3 Proj4js (3d) ✅ ──→ 5c4 WebGL 點位 (5d) ✅ ──→ 5c5 分區+空間查詢 (5d) ✅ ──→ 5c6 GML (5d) ✅
5c2 OpenLayers (5d) ✅┘                            └──→ 5c7 街景 (2d) ✅
```

---

## 5c1 — PostGIS 啟用 + 空間欄位 (3 天) ✅ 已完成

> **Spec**: 02-spec/04-asset-management.md §4-1-E 坐標規範  
> **SRS**: SRS-04-005 坐標資訊規範  
> **SA**: FN-03-013 設備地圖查詢  
> **SD**: SD-03-asset.md §6.1  
> **Test**: TS-03-asset.md (TC-03-013-01~13)

### 已完成 (Phase 4 / gis-todo-20260424)

- [x] **V49 Flyway migration** — `CREATE EXTENSION IF NOT EXISTS postgis`
- [x] **devices 表空間欄位** — `ALTER TABLE devices ADD COLUMN geom geometry(Point, 4326)`
- [x] **GiST 空間索引** — `CREATE INDEX idx_devices_geom ON devices USING gist(geom)`
- [x] **自動同步觸發器** — `fn_devices_sync_geom()` 在 lng/lat 變更時自動更新 geom
- [x] **既有資料回填** — `UPDATE devices SET geom = ST_SetSRID(ST_MakePoint(lng, lat), 4326)`
- [x] **V50 GIS 選單 + 權限** — GIS_VIEW / GIS_MANAGE 權限, 選單項目
- [x] **GisService** — 3 個空間查詢方法 (Native SQL + PostGIS)
  - `findAllDevices(deviceType)` — 全部設備 GeoJSON
  - `findDevicesInBounds(...)` — ST_Intersects + ST_MakeEnvelope
  - `findDevicesNearby(...)` — ST_DWithin + `<->` 距離排序
- [x] **GisController** — 3 個端點 (@PreAuthorize GIS_VIEW)
- [x] **GeoJsonResponse DTO** — FeatureCollection Java record
- [x] **GisControllerTest** — 10 個單元測試

---

## 5c2 — OpenLayers PoC + NLSC WMTS (5 天) ✅ 已完成

> **Spec**: 02-spec/04-asset-management.md §4-1-A 路燈地圖, §4-1-D 基本圖資  
> **SRS**: SRS-04-001 路燈地圖, SRS-04-004 基本圖資管理  
> **SA**: FN-03-013, FN-03-016 底圖切換  
> **SD**: SD-03-asset.md §6.7  
> **Test**: TS-03-asset.md (FN-03-016 ✅ 前端已實作)

### 已完成 (Phase 4 / gis-todo-20260424)

- [x] **npm 套件** — `ol@^10.9.0`, `proj4@^2.20.8`, `@types/proj4`
- [x] **GisMapView.vue** (~290 行)
  - NLSC WMTS 底圖 (千分之一地形圖) + OSM fallback
  - 設備 VectorLayer + GeoJSON 渲染
  - DeviceType 顏色區分 (6 種)
  - 點擊 Popup 顯示設備資訊
  - DeviceType 篩選器
  - Zoom-to-all 按鈕
  - Load-by-viewport-bounds 功能
  - 圖例 Legend
- [x] **API client** — `gis/index.ts` (3 functions)
- [x] **TypeScript 型別** — `types/gis.ts`
- [x] **路由** — `/admin/gis/map`

---

## 5c3 — Proj4js 坐標轉換整合 (3 天, after 5c2) ✅ 已完成

> **Spec**: 02-spec/04-asset-management.md §4-1-B 坐標資料整合  
> **SRS**: SRS-04-002 (AC-04-002-1~4)  
> **SA**: FN-03-014 坐標轉換  
> **SD**: SD-03-asset.md §6  
> **Test**: TS-03-asset.md (FN-03-014 ✅ 已實作)

### 設計決策

| 議題 | 決策 | 說明 |
|------|------|------|
| taipowerCoord 欄位格式 | 新增 `twd67_x`/`twd67_y` 數值欄位 | 與 TWD97 X/Y 對稱，可直接數學轉換。保留 `taipowerCoord` 作為台電原始參考碼 |
| 後端獨立轉換 API | 不建獨立 API | 前端 proj4 即時轉換 + 後端 DeviceService 存檔時自動補齊。批次匯入 (5c6) 時再加 |
| 自動同步方向 | 前端 + 後端雙保險 | 前端 proj4 即時預覽三組坐標 (UX)；後端 CoordinateService.autoFill() 在 save 前以 PostGIS ST_Transform 補齊 (資料完整性) |

### 需求

- AC-04-002-1: 儲存 TWD97 二度分帶坐標 ✅
- AC-04-002-2: 儲存 GPS 經緯度 (WGS84) ✅
- AC-04-002-3: 支援台電坐標 (TWD67 TM2) ✅
- AC-04-002-4: 提供三種坐標系統間相互轉換功能 ✅

### 現有實作

| 項目 | 狀態 |
|------|------|
| Device 欄位 `twd97X`, `twd97Y`, `lng`, `lat` | ✅ 已有 |
| Device 欄位 `twd67X`, `twd67Y` | ✅ V56 新增 |
| `taipowerCoord` (台電坐標參考碼) | ✅ 已有 (保留) |
| npm `proj4` 套件 | ✅ 已安裝 |
| PostGIS `ST_Transform` | ✅ 可用 |

### 已完成

#### 後端 — 坐標自動補齊

- [x] **V56 Flyway migration** — `twd67_x`/`twd67_y` 欄位 + EPSG:3826/3828 spatial_ref_sys 確認 + 既有資料回填
- [x] **Device entity** — 新增 `twd67X`/`twd67Y` BigDecimal 欄位
- [x] **DeviceRequest / DeviceResponse DTO** — 新增 `twd67X`/`twd67Y`
- [x] **CoordinateService** — PostGIS `ST_Transform` 伺服器端轉換
  - `transform(x, y, fromSrid, toSrid)` — 通用轉換
  - `autoFill(lng, lat, twd97X, twd97Y, twd67X, twd67Y)` — 從任一已有坐標自動補齊全部三組
  - 優先順序: WGS84 > TWD97 > TWD67
- [x] **DeviceService 整合** — `create()`/`update()` 在 save 前呼叫 `coordinateService.autoFill()`

#### 前端 — Proj4js 整合

- [x] **proj4 定義 TWD97/TWD67** — 註冊 EPSG:3826, EPSG:3828 投影定義
- [x] **coordinateUtils.ts** — 6 個純前端轉換 function
  - `wgs84ToTwd97(lng, lat)`, `twd97ToWgs84(x, y)`
  - `wgs84ToTwd67(lng, lat)`, `twd67ToWgs84(x, y)`
  - `twd97ToTwd67(x, y)`, `twd67ToTwd97(x, y)`
- [x] **設備詳情 drawer** — 三種坐標同步顯示 (WGS84 / TWD97 / TWD67)
- [x] **設備編輯 dialog** — 三組坐標輸入區，focus 任一組後 change 自動計算另外兩組
  - 台電坐標參考碼 + 高程獨立輸入
- [x] **GisMapView popup** — 顯示 WGS84 坐標
- [x] **TypeScript 型別** — `DeviceResponse`/`DeviceRequest` 新增 `twd67X`/`twd67Y`
- [x] **i18n** — 3 語系新增 coordSection / twd97Label / twd67Label / elevation / taipowerCoord

#### 測試 (8 個)

- [x] TC-03-014-01: transform same SRID returns original
- [x] TC-03-014-02: transform WGS84→TWD97 calls PostGIS
- [x] TC-03-014-03: autoFill all null returns all null
- [x] TC-03-014-04: autoFill from WGS84 fills TWD97 + TWD67
- [x] TC-03-014-05: autoFill from TWD97 fills WGS84 + TWD67
- [x] TC-03-014-06: autoFill from TWD67 fills WGS84 + TWD97
- [x] TC-03-014-07: autoFill WGS84+TWD97 only fills TWD67
- [x] TC-03-014-08: autoFill all provided → no transform called

---

## 5c4 — 設備點位圖層 16 萬點 WebGL (5 天, after 5c3) ✅ 已完成

> **Spec**: 02-spec/04-asset-management.md §4-1-A 單一畫面顯示全數點位  
> **SRS**: SRS-04-001 (AC-04-001-3: 單一畫面 1/1000 地形圖全數點位)  
>          SRS-04-019 (AC-04-019-1~4: 圖形化搜尋)  
> **SA**: FN-03-013 設備地圖查詢  
> **SD**: SD-03-asset.md §6.7  
> **Test**: TS-03-asset.md (FN-03-013, FN-03-019)

### 設計決策

| 議題 | 決策 | 說明 |
|------|------|------|
| 大量點位渲染策略 | 方案 B: 後端 zoom-aware payload + 前端 Cluster | 後端依 zoom 決定 simplified/full payload；前端 zoom < 14 用 `ol/source/Cluster` 聚合，zoom ≥ 14 用 `ol/layer/WebGLPoints` GPU 渲染個別點位 |
| DragBox 框選結果呈現 | 方案 B: 底部浮動面板 | `Transition slide-up` 底部面板顯示框選設備清單 (el-table)，點擊 row 飛到該設備並開啟 popup。不佔用地圖空間 |

### 需求

- AC-04-001-3: 單一畫面需顯示千分之一地形圖上全數點位 (約 16 萬) ✅
- AC-04-001-4: 點擊點位查看設備基本資料與維護履歷 ✅
- AC-04-001-5: 不同設備類型以不同圖示區分 ✅
- AC-04-019-1: 圖形化介面辨識圖示 ✅
- AC-04-019-2: 框選搜尋 ✅
- AC-04-019-3: 複合篩選 ✅

### 已完成

#### 後端 — zoom-aware 查詢

- [x] **GisService 擴充: Simplified payload**
  - `findDevicesInBounds()` 新增 `Integer zoom` 參數
  - zoom < 14: `toSimplifiedFeatures()` 只回 `id`, `deviceType`, `lng`, `lat` (最小 payload)
  - zoom ≥ 14: 回完整 properties (deviceCode, deviceName, status, etc.)
- [x] **GisController** — `/v1/gis/devices/bounds` 新增 `@RequestParam(required = false) Integer zoom`
- [x] **GisControllerTest** — 更新 mock 6 params + 新增 `devicesInBounds_withZoom_returns200` 測試

#### 前端 — WebGL 渲染 + Cluster + DragBox

- [x] **WebGLPointsLayer** (`ol/layer/WebGLPoints`) — GPU 加速渲染
  - FlatStyle expression: `circle-fill-color` match by `deviceColorNum` (7 種設備類型)
  - `circle-opacity` match by `status` (ACTIVE=1.0, 其他=0.5)
  - zoom ≥ 14 時啟用
- [x] **Cluster Source** (`ol/source/Cluster`, distance: 40)
  - zoom < 14 時啟用
  - `clusterStyle()`: 單點顯示設備類型色彩，多點顯示聚合圓圈 + 數量文字
  - 點擊多點 cluster → 自動 zoom in 到展開
- [x] **moveend 自動載入**
  - `map.on('moveend')` → debounce 300ms → `loadDevicesByBounds()`
  - 自動帶入 zoom 參數決定 simplified/full payload
  - loading 指示器
- [x] **DragBox 框選** (`ol/interaction/DragBox`)
  - `platformModifierKeyOnly` 條件 (⌘/Ctrl + 拖曳)
  - 底部浮動面板 (`Transition slide-up`) 顯示框選結果 el-table
  - 表格欄位: deviceCode, deviceName, deviceType, status
  - 點擊 row → `flyToDevice()` 飛到該設備 + 開啟 popup
- [x] **API** — `getGisDevicesBounds` 新增 `zoom?: number` 參數
- [x] **i18n** — 3 語系新增: boxSelectHint, dragToSelect, boxSelectResult, deviceName, panelBox, powerEquipment, attachment

---

## 5c5 — 分區範圍 + 空間查詢 (5 天, after 5c4) ✅ 已完成

> **Spec**: 02-spec/04-asset-management.md §4-1-D 分區範圍, §4-3-G 快速搜尋  
> **SRS**: SRS-04-004 (AC-04-004-3: 分區範圍)  
>          SRS-04-019 (AC-04-019-1~4: 圖形化搜尋)  
> **SA**: FN-03-017 分區範圍圖層, FN-03-018 管線圖層  
> **SD**: SD-03-asset.md §6

### 設計決策

| 議題 | 決策 | 說明 |
|------|------|------|
| 分區種子資料策略 | 方案 A: 只做行政區 | Schema + API 支援 4 種 zone type (ADMIN_DISTRICT/SQUAD/TAIPOWER/VENDOR)，種子資料只放臺北市 12 行政區簡化邊界。分隊/台電/廠商分區屬業務資料，留待管理介面或匯入功能後補 |
| 空間查詢結果 UI | 方案 A: 擴展底部面板 | 分區查詢、半徑搜尋結果都複用 5c4 底部浮動面板 (el-table + row-click flyToDevice)，保持 UX 一致 |
| 管線圖層 + 路線查詢 | 方案 A: 本期不做 | FN-03-018 管線圖層及路線緩衝區查詢需要管線 GIS 資料，目前無來源，留待後續有資料時再做 |

### 需求

- AC-04-004-3: 分區範圍 — 行政區里、分隊、台電配電區、廠商維護區 ✅
- AC-04-019-4: 圖形化搜尋 — 地圖/清冊同步呈現 ✅

### 已完成

#### 後端 — 分區 Polygon 管理

- [x] **V57 Flyway migration** — `map_zones` 表 + GiST 索引 + UK (tenant_id, zone_type, zone_code)
- [x] **12 行政區種子資料** — 簡化邊界 Polygon (WGS84)，含 population/area_km2 JSONB
- [x] **ZoneType enum** — ADMIN_DISTRICT / SQUAD / TAIPOWER / VENDOR
- [x] **MapZone entity** — JPA 實體，TenantAware，geom 由 native query 操作
- [x] **MapZoneService**
  - `getZonesByType(zoneType)` — ST_AsGeoJSON 回傳 Polygon GeoJSON + 每區 deviceCount (ST_Contains 子查詢)
  - `findDevicesInZone(zoneId)` — ST_Contains 查詢該區內設備
- [x] **GeoJsonResponse 擴充** — Feature.ofRawGeometry() + RawJson record 支援 Polygon GeoJSON
- [x] **GisController 新增端點**
  - `GET /v1/gis/zones?type=ADMIN_DISTRICT` — 分區 GeoJSON FeatureCollection
  - `GET /v1/gis/zones/{id}/devices` — 該分區內設備點位

#### 前端 — 分區圖層 + 半徑搜尋 UI

- [x] **API client** — `getGisZones(type)`, `getGisZoneDevices(zoneId)`
- [x] **GeoJSON 型別** — Geometry.type 支援 'Point' | 'Polygon'
- [x] **分區圖層** (VectorLayer)
  - 圖層控制 checkbox toggle + zoneType 下拉選单
  - 4 種分區類型各自顏色 (半透明填充 + 邊界線)
  - 區名文字標籤 (Text style with stroke halo)
  - 點擊分區 → API 查詢該區設備 → 底部面板顯示
- [x] **半徑搜尋 UI**
  - toggle 按鈕啟用 nearby mode
  - 半徑輸入 (100~5000m, step 100)
  - 點擊地圖 → 畫虛線圓圈 + 呼叫 `getGisDevicesNearby` API
  - 結果顯示在底部面板
- [x] **底部面板通用化** — bottomPanelTitle 動態顯示框選/分區/半徑查詢結果

#### 測試 (11 個新增)

- [x] MapZoneServiceTest: getZonesByType_returnsFeatureCollection
- [x] MapZoneServiceTest: getZonesByType_emptyResult
- [x] MapZoneServiceTest: findDevicesInZone_returnsDeviceFeatures
- [x] MapZoneServiceTest: findDevicesInZone_emptyResult
- [x] GisControllerTest: zones_authenticated_returns200
- [x] GisControllerTest: zones_missingType_returns400
- [x] GisControllerTest: zones_noToken_returns401
- [x] GisControllerTest: devicesInZone_authenticated_returns200
- [x] GisControllerTest: devicesInZone_noToken_returns401
- [x] GisControllerTest: zones_noPermission_returns403
- [x] i18n: 3 語系新增 zoneLayer/adminDistrict/squad/taipower/vendor/nearbySearch 等 10 個 key

---

## 5c6 — GML 匯出匯入 (5 天, after 5c5) ✅

> **Spec**: 02-spec/04-asset-management.md §4-1-F 圖資匯入匯出  
> **SRS**: SRS-04-006 (AC-04-006-1~6)  
> **SA**: FN-03-019 GML 匯入, FN-03-020 GML 匯出, FN-03-022 資料大平台匯出  
> **SD**: SD-03-asset.md §6  
> **Test**: TS-03-asset.md (FN-03-019~020, FN-03-022)

### 需求

- AC-04-006-1: 匯入圖資 → 比對差異 → 人工確認 ✅
- AC-04-006-2: 匯出 GML 格式圖資 ✅
- AC-04-006-3: 匯出符合臺北市資料大平臺格式 ✅
- AC-04-006-4: 匯入工務局管線資料 — 略 (無資料來源)
- AC-04-006-5: CAD 匯出 — 略 (無資料來源)
- AC-04-006-6: 施工圖資整合 — 略 (無資料來源)

### 設計決策

| 議題 | 決策 | 理由 |
|------|------|------|
| 實作範圍 | AC-04-006-1/2/3, 略過 4/5/6 | 4/5/6 需外部資料來源 (工務局管線、CAD 檔案、施工系統), 目前皆無, Phase 6 再整合 |
| XML 產生方式 | 方案 A: StringBuilder 模板 | GML 結構固定, JAXB/DOM 太重; StringBuilder 輕量可控, XSS 防護用 escapeXml |
| 匯入 UI | 方案 A: 簡化 (摘要差異+整批確認) | 不做逐筆勾選, 降低 UI 複雜度; 顯示新增/更新/刪除摘要 + tabs 明細表 |

### 已完成項目

#### 後端 — GML 匯出

- [x] **GmlExportService** — `exportAsGml(deviceType, district)` → OGC GML 3.2 XML
  - StringBuilder 模板, 坐標 EPSG:3826 (TWD97)
  - Feature 屬性: deviceCode, deviceName, deviceType, status, coordinates, address, installedAt
  - escapeXml() 防護 XSS
- [x] **GmlExportService** — `exportAsOpenDataCsv(deviceType)` → CSV
  - 臺北市資料大平臺格式: 編號,設備編碼,設備名稱,設備類型,狀態,經度,緯度,TWD97_X,TWD97_Y,地址,安裝日期
  - BOM header 確保 Excel 中文正確顯示
- [x] **GmlExportRow** — record DTO

#### 後端 — GML 匯入

- [x] **GmlImportService** — `parseAndDiff(InputStream)` → GmlImportDiff
  - DOM parser + XXE 防護 (disallow-doctype-decl, disable external entities)
  - 自動偵測座標系 (TWD97 vs WGS84, 依數值大小判斷)
  - 差異比對: 依 deviceCode 比對 DB 現有資料
- [x] **GmlImportService** — `applyImport(GmlImportDiff)` → int
  - CoordinateService.autoFill 自動補齊座標轉換
  - INSERT 新增 + UPDATE 更新, 刪除僅標記不執行
- [x] **GmlImportDiff** — record DTO (toAdd, toUpdate, toDelete, totalParsed) + inner ImportRow

#### 後端 — Controller

- [x] `GET /v1/gis/devices/export/gml?deviceType=&district=` → application/xml attachment
- [x] `GET /v1/gis/devices/export/open-data?deviceType=` → text/csv attachment (UTF-8 BOM)
- [x] `POST /v1/gis/devices/import/gml` (multipart) → BaseResponse\<GmlImportDiff\> — @PreAuthorize GIS_MANAGE
- [x] `POST /v1/gis/devices/import/gml/confirm` → BaseResponse\<Integer\> — @PreAuthorize GIS_MANAGE

#### 前端

- [x] **匯出下拉選單** — GisMapView 工具列, GML + CSV 兩選項, blob 下載
- [x] **匯入按鈕** — el-upload 接受 .gml/.xml, 上傳後顯示差異 dialog
- [x] **匯入差異 dialog** — 摘要 (新增/更新/刪除 tag) + tabs 明細表 + 確認按鈕
- [x] **API 層** — exportGisGml, exportGisOpenData, importGisGmlPreview, importGisGmlConfirm
- [x] **Types** — GmlImportDiff, GmlImportRow 介面
- [x] **i18n** — zh-TW/en/zh-CN 各 17 個 key

#### 測試 (641 tests total, +22 new)

- [x] GmlExportServiceTest — 7 tests (XML headers, device data, empty, CSV headers, CSV data, escapeXml, comma escape)
- [x] GmlImportServiceTest — 6 tests (parse features, invalid XML, new device, changed device, delete candidate, empty GML)
- [x] GisControllerTest — 29 tests (+9 new: export GML/CSV, import preview/confirm, permission 403)

---

## 5c7 — 街景連結 (2 天, after 5c3) ✅

> **Spec**: 02-spec/04-asset-management.md §4-1-C 街景及圖資介接  
> **SRS**: SRS-04-003 (P2 優先)  
> **SA**: FN-03-015 街景連結  
> **SD**: SD-03-asset.md §6

### 設計決策

| 議題 | 決策 | 理由 |
|------|------|------|
| 實作方式 | 方案 A: 外連 URL (window.open) | 無需 Google Cloud API Key/帳號，零成本；公開 URL 無使用限制 |
| URL 格式 | `https://www.google.com/maps/@?api=1&map_action=pano&viewpoint={lat},{lng}` | Google Maps 公開免費格式 |
| 嵌入位置 | GIS Popup + 設備管理 Drawer | 兩個主要查看設備資訊的入口 |
| 無座標處理 | v-if 隱藏按鈕 | 無經緯度時不顯示街景連結 |

### 已完成項目

- [x] **GisMapView.vue** — Popup 內新增「🛣️ 街景」連結按鈕 (v-if 有座標時顯示)
- [x] **DeviceManagementView.vue** — 設備詳情 Drawer 加入街景連結 (v-if 有座標時顯示)
- [x] **i18n** — zh-TW/en/zh-CN: streetView, openStreetView
- [x] **vue-tsc** — 編譯通過無錯誤
- [x] 純前端實作，無需後端 API，無需 Google 帳號/API Key

---

## 現有程式碼盤點

### 已完成的 GIS 檔案

| 分類 | 檔案 | 行數 | 說明 |
|------|------|------|------|
| Backend | `gis/controller/GisController.java` | 59 | 3 GET 端點 (devices/bounds/nearby) |
| Backend | `gis/dto/GeoJsonResponse.java` | 37 | FeatureCollection record |
| Backend | `gis/dto/BoundsRequest.java` | 10 | 未使用 (dead code) |
| Backend | `gis/service/GisService.java` | 131 | Native SQL + PostGIS 空間查詢 |
| Backend | `gis/service/CoordinateService.java` | 89 | PostGIS ST_Transform 坐標轉換 + autoFill |
| Backend | `V49__gis__enable_postgis.sql` | 45 | PostGIS + geom column + trigger |
| Backend | `V50__gis__menus_permissions.sql` | 49 | GIS 選單 + 權限 |
| Backend | `V56__gis__twd67_columns.sql` | 35 | TWD67 欄位 + EPSG 定義 + 回填 |
| Test | `GisControllerTest.java` | 187 | 10 個 WebMvc 測試 |
| Test | `CoordinateServiceTest.java` | 140 | 8 個單元測試 |
| Frontend | `views/admin/gis/GisMapView.vue` | ~290 | OpenLayers + NLSC WMTS |
| Frontend | `api/gis/index.ts` | 23 | 3 API functions |
| Frontend | `types/gis.ts` | 24 | GeoJSON TypeScript 型別 |
| Frontend | `utils/coordinateUtils.ts` | 42 | proj4 前端坐標轉換 (6 functions) |

### 已安裝的 GIS 依賴

| 套件 | 版本 | 用途 |
|------|------|------|
| `ol` | ^10.9.0 | OpenLayers 地圖引擎 |
| `proj4` | ^2.20.8 | 坐標投影轉換 |
| `@types/proj4` | ^2.5.6 | proj4 TypeScript 型別 |
| PostGIS | 3.4+ | PostgreSQL 空間擴充 |

### 已知待改善項目

| 項目 | 說明 | 影響 |
|------|------|------|
| BoundsRequest DTO 未使用 | Controller 用 @RequestParam | 清理 dead code |
| Device 無 JPA geom 欄位 | geom 只在 DB 層, Native SQL 查詢 | 設計決策, 不影響功能 |
| getGisDevicesNearby 未串接 | API 存在但前端未呼叫 | 5c5 半徑搜尋時串接 |
| 無 moveend 自動載入 | 需手動點按鈕載入 | 5c4 實作 |
| 無 Cluster 機制 | 大量點位可能卡頓 | 5c4 實作 |
| 無 Heatmap 圖層 | Phase 8 儀表板需要 | Phase 8 實作 |

---

## 文件追溯

| 分類 | 文件 |
|------|------|
| **Spec** | 02-spec/01-basic-requirements.md §1-3, 04-asset-management.md §4-1 (A~F), §4-3-G |
| **SRS** | SRS-04-001 (路燈地圖), SRS-04-002 (坐標整合), SRS-04-003 (街景), SRS-04-004 (基本圖資), SRS-04-005 (坐標規範), SRS-04-006 (圖資匯入匯出), SRS-04-019 (圖形化搜尋) |
| **SA** | SA-03 FN-03-013~023 (GIS 全部功能節點) |
| **SD** | SD-03-asset.md §6 (PostGIS, GeoJSON, API, 前端架構) |
| **Test** | TS-03-asset.md (TC-03-013~023) |
| **ADR** | 99-adr/ADR-003-gis-open-source.md |
| **Plan** | 99-plan/2026-04-24-execution-plan.md §Phase 5C |
| **Gantt** | 99-plan/2026-04-24-gantt.md (5c1~5c7) |
| **Done** | todo/gis-todo-20260424-done.md (5c1+5c2 設計+實作) |

## 風險 & 注意事項

| 風險 | 影響 | 緩解措施 |
|------|------|---------|
| NLSC WMTS 需申請 | 底圖可能受限 | OSM fallback 已就位; tx1 外部申請追蹤 |
| 16 萬點效能 | 前端渲染卡頓 | WebGLPointsLayer + Cluster + viewport 分頁 |
| GML 格式規範 | 不符政府標準 | 參考「公共設施管線資料標準」XML Schema |
| 行政區 polygon 資料 | 需外部取得 | 政府開放資料平臺下載 → Flyway seed |
| TWD67 投影參數 | ~~proj4 預設無 EPSG:3828~~ | ✅ 已在 V56 + coordinateUtils.ts 手動註冊 |
