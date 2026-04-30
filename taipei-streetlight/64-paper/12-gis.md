# 12. GIS 地理資訊模組 (GIS Module)

## 1. 模組概述

GIS 模組為台北市路燈管理系統提供空間資料查詢與管理能力，基於 PostGIS 擴充實現設備點位的地理查詢、分區管理、座標轉換，以及 GML/CSV 格式的匯入匯出功能。

本模組直接操作 `devices` 表的空間欄位（`geom`），不額外定義設備實體，而是透過 Native SQL + PostGIS 函數進行空間運算。分區管理使用獨立的 `map_zones` 表儲存多邊形幾何（Polygon）。

**套件路徑：** `com.taipei.iot.gis`

**子套件結構：**
- `entity` — JPA 實體（MapZone, ZoneType）
- `dto` — 資料傳輸物件（GeoJsonResponse, GmlExportRow, GmlImportDiff, BoundsRequest）
- `service` — 業務邏輯層（GisService, MapZoneService, CoordinateService, GmlExportService, GmlImportService）
- `controller` — REST API 控制層（GisController）

---

## 2. 資料表結構

### 2.1 map_zones（地圖分區）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR | NOT NULL | 租戶識別碼 |
| zone_type | VARCHAR(30) | NOT NULL | 分區類型（ZoneType） |
| zone_code | VARCHAR(50) | NOT NULL | 分區代碼 |
| zone_name | VARCHAR(100) | NOT NULL | 分區名稱 |
| geom | GEOMETRY | PostGIS 原生幾何欄位 | 分區多邊形（不映射到 JPA，透過 Native Query 操作） |
| properties | JSONB | | 擴充屬性 |
| created_at | TIMESTAMP | 不可更新 | 建立時間 |

### 2.2 devices 表相關空間欄位（屬 device 模組，GIS 模組讀取）

| 欄位 | 型別 | 說明 |
|------|------|------|
| geom | GEOMETRY(Point, 4326) | PostGIS 空間索引欄位 (WGS84) |
| lng | DECIMAL | 經度 (WGS84) |
| lat | DECIMAL | 緯度 (WGS84) |
| twd97_x | DECIMAL | TWD97 橫座標 (EPSG:3826) |
| twd97_y | DECIMAL | TWD97 縱座標 (EPSG:3826) |
| device_code | VARCHAR | 設備編碼 |
| device_name | VARCHAR | 設備名稱 |
| device_type | VARCHAR | 設備類型 |
| status | VARCHAR | 設備狀態 |
| address | VARCHAR | 地址 |
| installed_at | TIMESTAMP | 安裝日期 |
| dept_id | BIGINT | 所屬部門 |

---

## 3. 實體關聯

```
MapZone ──(geom 空間包含)──> Device(s)
    │
    └── zone_type 分類：ADMIN_DISTRICT / SQUAD / TAIPOWER / VENDOR

Device.geom ←── PostGIS 空間索引
    │
    ├── ST_Intersects (bounding box 查詢)
    ├── ST_DWithin (鄰近查詢)
    └── ST_Contains (分區內設備查詢)
```

**跨模組關聯：**
- GIS 模組讀取 `devices` 表（屬 device 模組）
- GML 匯入時直接 INSERT/UPDATE `devices` 表

---

## 4. API 端點摘要

所有端點前綴：`/v1/gis`

### 4.1 設備點位查詢

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/devices` | GIS_VIEW | 取得所有設備 GeoJSON（可篩選 deviceType） |
| GET | `/devices/bounds` | GIS_VIEW | 矩形範圍查詢（minLng, minLat, maxLng, maxLat, deviceType, zoom） |
| GET | `/devices/nearby` | GIS_VIEW | 鄰近查詢（lng, lat, radius 預設 500m） |

### 4.2 分區查詢

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/zones` | GIS_VIEW | 依類型取得分區（含設備數量統計） |
| GET | `/zones/{id}/devices` | GIS_VIEW | 取得指定分區內的設備 |

### 4.3 匯出

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/devices/export/gml` | GIS_VIEW | 匯出 GML 3.2 格式（EPSG:3826），可篩選 deviceType, district |
| GET | `/devices/export/open-data` | GIS_VIEW | 匯出台北市開放資料平台 CSV 格式（UTF-8 BOM） |

### 4.4 匯入

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| POST | `/devices/import/gml` | GIS_MANAGE | 上傳 GML 檔案，回傳差異比對結果（預覽） |
| POST | `/devices/import/gml/confirm` | GIS_MANAGE | 確認匯入，執行新增/更新 |

---

## 5. 業務邏輯

### 5.1 空間查詢

**矩形範圍查詢（Bounding Box）：**
- 使用 `ST_Intersects(geom, ST_MakeEnvelope(minLng, minLat, maxLng, maxLat, 4326))`
- 支援 zoom level 優化：`zoom < 14` 時僅回傳精簡欄位（id, deviceType, lng, lat），減少傳輸量

**鄰近查詢：**
- 使用 `ST_DWithin(geom::geography, point::geography, radius)` 以公尺為單位
- 結果以 `geom <-> point` 距離排序

**分區內設備查詢：**
- 使用 `ST_Contains(zone.geom, device.geom)` 判斷包含關係
- 分區列表額外計算 `device_count`（子查詢）

### 5.2 座標轉換（CoordinateService）

支援三種座標系統互轉：

| SRID | 座標系統 | 說明 |
|------|---------|------|
| 4326 | WGS84 | GPS 經緯度（國際通用） |
| 3826 | TWD97 | 台灣大地座標 1997（政府標準） |
| 3828 | TWD67 | 台灣大地座標 1967（舊系統相容） |

**自動填充邏輯（autoFill）：**
- 優先順序：WGS84 > TWD97 > TWD67
- 提供任一組座標，自動透過 PostGIS `ST_Transform` 補齊其餘座標系統
- WGS84 精度：7 位小數；TWD97/TWD67 精度：3 位小數

### 5.3 GML 匯出

- 輸出符合 OGC GML 3.2 標準的 XML
- 座標使用 EPSG:3826（TWD97），符合政府機關規範
- 命名空間：`sl` = `http://taipei.gov.tw/streetlight`
- XML 特殊字元自動跳脫（&, <, >, ", '）

### 5.4 開放資料 CSV 匯出

- 依台北市開放資料平台格式輸出
- CSV 檔案前置 UTF-8 BOM（`0xEF 0xBB 0xBF`）以相容 Excel
- 欄位：編號, 設備編碼, 設備名稱, 設備類型, 狀態, 經度, 緯度, TWD97_X, TWD97_Y, 地址, 安裝日期

### 5.5 GML 匯入（兩階段）

**第一階段：解析與差異比對（parseAndDiff）**
1. 解析 GML XML（啟用 XXE 防護）
2. 擷取 `featureMember` 內的 deviceCode, deviceName, deviceType, status 及座標
3. 座標系統自動判斷：X > 1000 視為 TWD97，否則視為 WGS84
4. 與資料庫現有設備（同租戶，以 deviceCode 匹配）比對，產出三類差異：
   - `toAdd` — 新增（DB 中不存在的 deviceCode）
   - `toUpdate` — 更新（存在但欄位有變動，記錄變動欄位名稱）
   - `toDelete` — 候選刪除（DB 中存在但 GML 中沒有）

**第二階段：確認匯入（applyImport）**
1. 新增項目：呼叫 `CoordinateService.autoFill` 補齊座標後 INSERT
2. 更新項目：同樣補齊座標後 UPDATE
3. 刪除項目：不自動執行，需人工處理
4. 回傳處理筆數

### 5.6 GeoJSON 回應格式

所有空間查詢統一回傳 GeoJSON FeatureCollection：

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": { "type": "Point", "coordinates": [121.5, 25.03] },
      "properties": { "id": 1, "deviceCode": "SL-001", ... }
    }
  ]
}
```

分區查詢的 geometry 由 PostGIS `ST_AsGeoJSON` 直接輸出原始 JSON（透過 `RawJson` 包裝避免二次序列化）。

---

## 6. 資料流

### 6.1 地圖載入流程

```
前端地圖初始化
    │
    ▼
GET /v1/gis/devices/bounds?minLng=...&zoom=15
    │
    ▼
GisService.findDevicesInBounds()
    ├─ zoom >= 14 → 完整欄位（id, code, name, type, status, lng, lat, deptId）
    └─ zoom < 14  → 精簡欄位（id, type, lng, lat）
    │
    ▼
回傳 GeoJSON FeatureCollection → 前端渲染標記點
```

### 6.2 GML 匯入流程

```
使用者上傳 GML 檔案
    │
    ▼
POST /devices/import/gml
    │
    ▼
GmlImportService.parseAndDiff()
    ├─ 1. XML 解析（XXE 防護）
    ├─ 2. 擷取 featureMember
    ├─ 3. 座標系統判斷（TWD97 or WGS84）
    └─ 4. 與 DB 比對 → 回傳 GmlImportDiff{toAdd, toUpdate, toDelete}
    │
    ▼
前端顯示差異預覽，使用者確認
    │
    ▼
POST /devices/import/gml/confirm
    │
    ▼
GmlImportService.applyImport()
    ├─ toAdd  → CoordinateService.autoFill → INSERT devices
    └─ toUpdate → CoordinateService.autoFill → UPDATE devices
    │
    ▼
回傳處理筆數
```

### 6.3 座標轉換流程

```
輸入任一組座標（WGS84 / TWD97 / TWD67）
    │
    ▼
CoordinateService.autoFill()
    │
    ├─ 有 WGS84 → ST_Transform(4326→3826) → TWD97
    │            → ST_Transform(4326→3828) → TWD67
    │
    ├─ 有 TWD97 → ST_Transform(3826→4326) → WGS84
    │            → ST_Transform(3826→3828) → TWD67
    │
    └─ 有 TWD67 → ST_Transform(3828→4326) → WGS84
                 → ST_Transform(3828→3826) → TWD97
    │
    ▼
回傳 CoordinateSet（lng, lat, twd97X, twd97Y, twd67X, twd67Y）
```

### 6.4 分區查詢流程

```
GET /v1/gis/zones?type=ADMIN_DISTRICT
    │
    ▼
MapZoneService.getZonesByType()
    ├─ 查詢 map_zones WHERE zone_type = ?
    ├─ ST_AsGeoJSON(geom) → 多邊形 GeoJSON
    └─ 子查詢 COUNT(devices WHERE ST_Contains(zone.geom, device.geom))
    │
    ▼
回傳含 geometry + deviceCount 的 FeatureCollection

GET /v1/gis/zones/{id}/devices
    │
    ▼
MapZoneService.findDevicesInZone()
    └─ SELECT devices WHERE ST_Contains(zone.geom, device.geom)
    │
    ▼
回傳設備點位 FeatureCollection
```

---

## 7. 列舉值定義

### ZoneType（分區類型）

| 值 | 說明 |
|---|------|
| ADMIN_DISTRICT | 行政區（如：中正區、大安區） |
| SQUAD | 分隊分區（路燈維護分隊轄區） |
| TAIPOWER | 台電區處（台電供電區域） |
| VENDOR | 廠商分區（維護廠商負責區域） |

### 座標系統常數（CoordinateService）

| 常數名稱 | SRID | 說明 |
|---------|------|------|
| SRID_WGS84 | 4326 | WGS84 經緯度座標系統 |
| SRID_TWD97 | 3826 | TWD97 TM2 投影座標系統 |
| SRID_TWD67 | 3828 | TWD67 TM2 投影座標系統 |

### 權限代碼

| 權限 | 說明 |
|------|------|
| GIS_VIEW | 檢視地圖與空間資料 |
| GIS_MANAGE | 管理空間資料（GML 匯入） |
