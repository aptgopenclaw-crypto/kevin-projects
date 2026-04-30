# TS-03 資產管理 — Test Specification

> **對應 SA**：SA-03-asset (FN-03-001 ~ FN-03-060)  
> **對應 SD**：SD-03-asset  
> **Test Classes**：9 classes, 66 test methods  
> **最新驗證**：2026-04-24 · 全部 PASS

---

## 使用方式

本文件是 **AI 實作 prompt 的驗收標準**。請 AI 實作或修改某個 FN 時，附上對應段落：

```
請實作 FN-03-002 新增設備
【SA】SA-03 §設備管理
【SD】SD-03 §4 POST /v1/auth/devices
【TC】（貼 FN-03-002 的 Test Cases 表）
請確保所有 TC PASS。
```

---

## 1. 設備管理 (FN-03-001 ~ FN-03-012)

### FN-03-001 設備列表查詢

**SA**: SA-03 §設備管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/devices`  
**Service**: `DeviceService.listDevices()` | **SRS**: SRS-04-007 | **Spec**: §4-3

**商業規則**：
- Data Scope 過濾（依部門權限）
- 多租戶隔離
- 支援分頁 / 搜尋 / 排序

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-001-01 | API | 列表查詢 | authenticated, perm=DEVICE_VIEW | GET /devices | 200 | HTTP 200 | ✅ DeviceControllerTest.list |
| TC-03-001-02 | Error | 未登入 | no token | GET /devices | 401 | HTTP 401 | ✅ DeviceControllerTest.list_noToken |
| TC-03-001-03 | Error | 無權限 | no DEVICE_VIEW | GET /devices | 403 | HTTP 403 | ✅ DeviceControllerTest.list_noPermission |
| TC-03-001-04 | Happy | DataScope 過濾 | deptAdmin | listDevices() | 僅管轄 dept 設備 | deptId 過濾 | ✅ DeviceServiceTest.listDevices_dataScope |
| TC-03-001-05 | Edge | 空 deptIds | deptIds=[] | listDevices() | 空結果 | list empty | ✅ DeviceServiceTest.listDevices_emptyDeptIds |

---

### FN-03-002 新增設備

**SA**: SA-03 §設備管理 | **SD**: SD-03 §4 | **API**: `POST /v1/auth/devices`  
**Service**: `DeviceService.create()` | **SRS**: SRS-04-007 | **Spec**: §4-3

**商業規則**：
- deviceCode 不可重複
- 支援 JSONB 動態欄位（FN-03-008）
- 不可建立循環父子關係

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-002-01 | API | 新增設備 | perm=DEVICE_MANAGE | POST /devices | 200 | HTTP 200 | ✅ DeviceControllerTest.create |
| TC-03-002-02 | Error | 無權限 | no DEVICE_MANAGE | POST /devices | 403 | HTTP 403 | ✅ DeviceControllerTest.create_noPermission |
| TC-03-002-03 | Happy | 正常新增 | valid device data | create() | device created | id 非 null | ✅ DeviceServiceTest.create_success |
| TC-03-002-04 | Error | deviceCode 重複 | code 已存在 | duplicate code | error | errorCode | ✅ DeviceServiceTest.create_duplicateCode |
| TC-03-002-05 | Error | 循環父設備 | parentId = self | create() | 拋錯 | circular ref | ✅ DeviceServiceTest.create_withCircularParent |

---

### FN-03-003 編輯設備

**SA**: SA-03 §設備管理 | **SD**: SD-03 §4 | **API**: `PUT /v1/auth/devices/{id}`  
**Service**: `DeviceService.update()` | **SRS**: SRS-04-007 | **Spec**: §4-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-003-01 | Error | 循環參照 | update parentId → circular | PUT /devices/{id} | 拋錯 | circular ref | ✅ DeviceServiceTest.update_circularReference |

---

### FN-03-004 查看設備詳情

**SA**: SA-03 §設備管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/devices/{id}`  
**Service**: `DeviceService.getById()` | **SRS**: SRS-04-007 | **Spec**: §4-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-004-01 | API | 查詢設備 | perm=DEVICE_VIEW | GET /devices/{id} | 200 | HTTP 200 | ✅ DeviceControllerTest.getById |
| TC-03-004-02 | Error | 不存在 | invalid id | GET /devices/{id} | 404 | HTTP 404 | ✅ DeviceControllerTest.getById_notFound |
| TC-03-004-03 | Happy | Service 查詢 | device exists | getById() | device detail | 含 components | ✅ DeviceServiceTest.getById_found |
| TC-03-004-04 | Error | Service 不存在 | invalid id | getById() | not found error | errorCode | ✅ DeviceServiceTest.getById_notFound |
| TC-03-004-05 | Happy | 含組件查詢 | 有 components | getByIdWithComponents() | 含 component list | components 非空 | ✅ DeviceServiceTest.getByIdWithComponents |

---

### FN-03-005 刪除設備

**SA**: SA-03 §設備管理 | **SD**: SD-03 §4 | **API**: `DELETE /v1/auth/devices/{id}`  
**Service**: `DeviceService.delete()` | **SRS**: SRS-04-007 | **Spec**: §4-3

**商業規則**：
- 有子設備不可刪

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-005-01 | API | 刪除設備 | perm=DEVICE_MANAGE | DELETE /devices/{id} | 200 | HTTP 200 | ✅ DeviceControllerTest.delete |
| TC-03-005-02 | Error | 有子設備 | children exist | delete() | 拋錯 | 不可刪 | ✅ DeviceServiceTest.delete_withChildren |
| TC-03-005-03 | Happy | 無子設備 | no children | delete() | 200 | 已刪除 | ✅ DeviceServiceTest.delete_noChildren |

---

### FN-03-006 設備統計概覽

**SA**: SA-03 §設備管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/devices/stats`  
**SRS**: SRS-04-007 | **Spec**: §4-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-006-01 | Happy | 統計概覽 | devices exist | GET /devices/stats | 200, stats | 各分類計數 | ⬜ 待實作 |

---

### FN-03-007 設備快速搜尋

**SA**: SA-03 §設備搜尋 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/devices/search`  
**SRS**: SRS-04-015 | **Spec**: §4-3G

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-007-01 | Happy | keyword 搜尋 | devices exist | keyword="路燈" | 200, matched list | 含 keyword | ⬜ 待實作 |

---

### FN-03-008 設備 JSONB 動態欄位管理

**SA**: SA-03 §設備管理 | **SD**: SD-03 §4 | **API**: (含於 FN-03-002/003)  
**Service**: `DeviceService` | **SRS**: SRS-04-007 | **Spec**: §4-3

**商業規則**：
- JSONB 欄位大小限制

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-008-01 | Error | JSONB 超大 | — | oversized JSON | 拋錯 | size 限制 | ✅ DeviceServiceTest.create_oversizedJsonb |

---

### FN-03-009 設備歷程查詢

**SA**: SA-03 §設備歷程 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/devices/{id}/events`  
**SRS**: SRS-04-008 | **Spec**: §4-3A

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-009-01 | Happy | 查詢歷程 | device 有事件紀錄 | GET /devices/{id}/events | 200, event list | 時間排序 | ⬜ 待實作 |

---

### FN-03-010 設備組件替換

**SA**: SA-03 §設備組件 | **SD**: SD-03 §4 | **API**: `PUT /v1/auth/devices/{id}/replace-component`  
**Service**: `DeviceService.getActiveComponents()` | **SRS**: SRS-04-008 | **Spec**: §4-3C

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-010-01 | Happy | 查詢現有組件 | device 有 components | getActiveComponents() | component list | active 的 | ✅ DeviceServiceTest.getActiveComponents |
| TC-03-010-02 | Happy | 替換組件 | component exists | replace-component | 200 | 新組件 | ⬜ 待補 |

---

### FN-03-011 拓撲樹查詢

**SA**: SA-03 §拓撲 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/devices/topology`  
**SRS**: SRS-04-009 | **Spec**: §4-3 D-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-011-01 | Happy | 拓撲樹 | 有父子設備 | GET /devices/topology | 200, tree | parent-children | ⬜ 待實作 |

---

### FN-03-012 設定父設備

**SA**: SA-03 §拓撲 | **SD**: SD-03 §4 | **API**: `PUT /v1/auth/devices/{id}`  
**Service**: `DeviceService.update()` — parentId 欄位 | **SRS**: SRS-04-009 | **Spec**: §4-3 D-2

→ 含於 FN-03-003 編輯設備 (TC-03-003-01)

---

## 2. GIS 地圖 (FN-03-013 ~ FN-03-023)

### FN-03-013 設備地圖查詢

**SA**: SA-03 §GIS 地理資訊 | **SD**: SD-03 §6 | **API**: `GET /v1/gis/devices`, `GET /v1/gis/devices/bounds`, `GET /v1/gis/devices/nearby`  
**SRS**: SRS-04-001 | **Spec**: §4-1A  
**Controller**: `GisController` | **Service**: `GisService` | **Test**: `GisControllerTest`

#### Test Cases — 全部設備查詢

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-013-01 | Happy | 全部設備查詢 | devices 有 lng/lat | GET /v1/gis/devices | 200, GeoJSON FeatureCollection | type, features[].geometry.type=Point | ✅ GisControllerTest.allDevices_authenticated_returns200 |
| TC-03-013-02 | Happy | 設備類型篩選 | devices 有 lng/lat | GET /v1/gis/devices?deviceType=STREETLIGHT | 200, GeoJSON | features 過濾正確 | ✅ GisControllerTest.allDevices_withDeviceTypeFilter_returns200 |
| TC-03-013-03 | Auth | 未登入 | no token | GET /v1/gis/devices | 401 | HTTP 401 | ✅ GisControllerTest.allDevices_noToken_returns401 |
| TC-03-013-04 | Auth | 無 GIS_VIEW 權限 | token 無 GIS_VIEW | GET /v1/gis/devices | 403 | HTTP 403 | ✅ GisControllerTest.allDevices_noPermission_returns403 |

#### Test Cases — 邊界框查詢

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-013-05 | Happy | bbox 查詢 | devices 有 geom | GET /v1/gis/devices/bounds?minLng=...&minLat=...&maxLng=...&maxLat=... | 200, GeoJSON | ST_Intersects 過濾 | ✅ GisControllerTest.devicesInBounds_authenticated_returns200 |
| TC-03-013-06 | Error | 缺少必要參數 | authenticated | GET /v1/gis/devices/bounds?minLng=121.51 | 400 | HTTP 400 | ✅ GisControllerTest.devicesInBounds_missingParam_returns400 |
| TC-03-013-07 | Auth | 未登入 | no token | GET /v1/gis/devices/bounds | 401 | HTTP 401 | ✅ GisControllerTest.devicesInBounds_noToken_returns401 |
| TC-03-013-08 | Auth | 無權限 | token 無 GIS_VIEW | GET /v1/gis/devices/bounds | 403 | HTTP 403 | ✅ GisControllerTest.devicesInBounds_noPermission_returns403 |

#### Test Cases — 鄰近查詢

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-013-09 | Happy | 預設半徑 500m | devices 有 geom | GET /v1/gis/devices/nearby?lng=121.52&lat=25.034 | 200, GeoJSON | ST_DWithin 500m | ✅ GisControllerTest.devicesNearby_authenticated_returns200 |
| TC-03-013-10 | Happy | 自訂半徑 1000m | devices 有 geom | GET /v1/gis/devices/nearby?lng=...&lat=...&radius=1000 | 200, GeoJSON | 距離排序 | ✅ GisControllerTest.devicesNearby_customRadius_returns200 |
| TC-03-013-11 | Error | 缺少必要參數 | authenticated | GET /v1/gis/devices/nearby | 400 | HTTP 400 | ✅ GisControllerTest.devicesNearby_missingParams_returns400 |
| TC-03-013-12 | Auth | 未登入 | no token | GET /v1/gis/devices/nearby | 401 | HTTP 401 | ✅ GisControllerTest.devicesNearby_noToken_returns401 |
| TC-03-013-13 | Auth | 無權限 | token 無 GIS_VIEW | GET /v1/gis/devices/nearby | 403 | HTTP 403 | ✅ GisControllerTest.devicesNearby_noPermission_returns403 |

---

### FN-03-014 ~ FN-03-023 地圖/GML/匯出

> **實作狀態**：Phase 5C 部分完成；FN-03-016（底圖切換）前端已實作

| FN ID | 功能名稱 | TC 設計 | 自動化 |
|-------|---------|---------|--------|
| FN-03-014 | 坐標轉換 | TC-03-014-01: TWD97↔WGS84 轉換 | ⬜ 待實作 |
| FN-03-015 | 街景連結 | TC-03-015-01: 前端計算 Google Street View URL | ⬜ 前端 |
| FN-03-016 | 底圖切換 | TC-03-016-01: 前端 NLSC tile layer + OSM fallback | ✅ 前端已實作 |
| FN-03-017 | 分區範圍圖層 | TC-03-017-01: 回傳 zone GeoJSON | ⬜ 待實作 |
| FN-03-018 | 管線圖層 | TC-03-018-01: 回傳 pipeline GeoJSON | ⬜ 待實作 |
| FN-03-019 | GML 匯入 | TC-03-019-01: 解析 GML → devices | ⬜ 待實作 |
| FN-03-020 | GML 匯出 | TC-03-020-01: devices → GML XML | ⬜ 待實作 |
| FN-03-021 | 工務局管線匯入 | TC-03-021-01: 批次匯入 pipeline | ⬜ 待實作 |
| FN-03-022 | 資料大平台匯出 | TC-03-022-01: Open Data JSON format | ⬜ 待實作 |
| FN-03-023 | 圖資匯出 CAD | TC-03-023-01: DXF format export | ⬜ 待實作 |

---

## 3. 回路管理 (FN-03-024 ~ FN-03-028)

### FN-03-024 回路列表查詢

**SA**: SA-03 §回路管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/circuits`  
**Service**: `CircuitService` | **SRS**: SRS-04-010 | **Spec**: §4-3 D-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-024-01 | API | 列表查詢 | authenticated | GET /circuits | 200 | HTTP 200 | ✅ CircuitControllerTest.list |
| TC-03-024-02 | Error | 未登入 | no token | GET /circuits | 401 | HTTP 401 | ✅ CircuitControllerTest.list_noToken |

---

### FN-03-025 新增回路

**SA**: SA-03 §回路管理 | **SD**: SD-03 §4 | **API**: `POST /v1/auth/circuits`  
**Service**: `CircuitService.create()` | **SRS**: SRS-04-010 | **Spec**: §4-3 D-3

**商業規則**：
- circuitCode 不可重複

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-025-01 | API | 新增回路 | perm=CIRCUIT_MANAGE | POST /circuits | 200 | HTTP 200 | ✅ CircuitControllerTest.create |
| TC-03-025-02 | Error | 無權限 | no perm | POST /circuits | 403 | HTTP 403 | ✅ CircuitControllerTest.create_noPermission |
| TC-03-025-03 | Happy | 正常新增 | valid data | create() | circuit created | id 非 null | ✅ CircuitServiceTest.create_success |
| TC-03-025-04 | Error | code 重複 | code exists | create() | error | errorCode | ✅ CircuitServiceTest.create_duplicate |

---

### FN-03-026 編輯回路

**SA**: SA-03 §回路管理 | **SD**: SD-03 §4 | **API**: `PUT /v1/auth/circuits/{id}`  
**Service**: `CircuitService.update()` | **SRS**: SRS-04-010 | **Spec**: §4-3 D-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-026-01 | Happy | 更新回路 | circuit exists | update() | 200 | 欄位更新 | ✅ CircuitServiceTest.update_success |

---

### FN-03-027 刪除回路

**SA**: SA-03 §回路管理 | **SD**: SD-03 §4 | **API**: `DELETE /v1/auth/circuits/{id}`  
**Service**: `CircuitService.delete()` | **SRS**: SRS-04-010 | **Spec**: §4-3 D-3

**商業規則**：
- 有關聯設備不可刪

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-027-01 | API | 刪除回路 | perm=CIRCUIT_MANAGE | DELETE /circuits/{id} | 200 | HTTP 200 | ✅ CircuitControllerTest.delete |
| TC-03-027-02 | Error | 有關聯設備 | devices exist | DELETE /circuits/{id} | error | 不可刪 | ✅ CircuitControllerTest.delete_hasDevices |
| TC-03-027-03 | Error | Service 層—有設備 | devices linked | delete() | 拋錯 | errorCode | ✅ CircuitServiceTest.delete_withDevices |
| TC-03-027-04 | Happy | Service 層—無設備 | no devices | delete() | 200 | 已刪除 | ✅ CircuitServiceTest.delete_noDevices |

---

### FN-03-028 查看回路設備

**SA**: SA-03 §回路管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/circuits/{id}/devices`  
**SRS**: SRS-04-010 | **Spec**: §4-3 D-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-028-01 | Happy | 查看回路設備 | circuit 有 devices | GET /circuits/{id}/devices | 200, device list | list 非空 | ⬜ 待補 |

---

## 4. 契約管理 (FN-03-029 ~ FN-03-037)

### FN-03-029 契約列表查詢

**SA**: SA-03 §契約管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/contracts`  
**Service**: `ContractService` | **SRS**: SRS-04-006 | **Spec**: §4-2A

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-029-01 | API | 列表查詢 | authenticated | GET /contracts | 200 | HTTP 200 | ✅ ContractControllerTest.list |
| TC-03-029-02 | Error | 未登入 | no token | GET /contracts | 401 | HTTP 401 | ✅ ContractControllerTest.list_noToken |

---

### FN-03-030 新增契約

**SA**: SA-03 §契約管理 | **SD**: SD-03 §4 | **API**: `POST /v1/auth/contracts`  
**Service**: `ContractService.create()` | **SRS**: SRS-04-006 | **Spec**: §4-2A

**商業規則**：
- 預設 status=ACTIVE
- 可指定 status

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-030-01 | API | 新增契約 | perm=CONTRACT_MANAGE | POST /contracts | 200 | HTTP 200 | ✅ ContractControllerTest.create |
| TC-03-030-02 | Error | 無權限 | no perm | POST /contracts | 403 | HTTP 403 | ✅ ContractControllerTest.create_noPermission |
| TC-03-030-03 | Happy | 預設 ACTIVE | 不指定 status | create() | status=ACTIVE | 預設值 | ✅ ContractServiceTest.create_defaultActive |
| TC-03-030-04 | Happy | 指定狀態 | status=DRAFT | create() | status=DRAFT | 指定值 | ✅ ContractServiceTest.create_explicitStatus |

---

### FN-03-031 編輯契約

**SA**: SA-03 §契約管理 | **SD**: SD-03 §4 | **API**: `PUT /v1/auth/contracts/{id}`  
**Service**: `ContractService.update()` | **SRS**: SRS-04-006 | **Spec**: §4-2A

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-031-01 | Happy | 更新契約 | contract exists | update() | 200 | 欄位更新 | ✅ ContractServiceTest.update |

---

### FN-03-032 刪除契約

**SA**: SA-03 §契約管理 | **SD**: SD-03 §4 | **API**: `DELETE /v1/auth/contracts/{id}`  
**Service**: `ContractService.delete()` | **SRS**: SRS-04-006 | **Spec**: §4-2A

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-032-01 | API | 刪除契約 | perm=CONTRACT_MANAGE | DELETE /contracts/{id} | 200 | HTTP 200 | ✅ ContractControllerTest.delete |
| TC-03-032-02 | Happy | Service 層刪除 | contract exists | delete() | 200 | 已刪除 | ✅ ContractServiceTest.delete |

---

### FN-03-033 契約資產明細

**SA**: SA-03 §契約管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/contracts/{id}/devices`  
**SRS**: SRS-04-006 | **Spec**: §4-2B

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-033-01 | Happy | 查詢資產明細 | contract 有 devices | GET /contracts/{id}/devices | 200 | device list | ⬜ 待實作 |

---

### FN-03-034 ~ FN-03-037 契約進階

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC 設計 | 自動化 |
|-------|---------|---------|--------|
| FN-03-034 | 契約資產批次轉移 | TC-03-034-01: 批次轉移設備至新契約 | ⬜ 待實作 |
| FN-03-035 | 保固到期提醒 | TC-03-035-01: 排程掃描 → 通知 | ⬜ 待實作 |
| FN-03-036 | 預防性維護排程 | TC-03-036-01: CRUD 排程 | ⬜ 待實作 |
| FN-03-037 | 維護排程提醒 | TC-03-037-01: 排程觸發提醒 | ⬜ 待實作 |

---

## 5. 障礙管理 (FN-03-038 ~ FN-03-047)

### FN-03-038 障礙工單列表

**SA**: SA-03 §障礙管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/faults`  
**Service**: `FaultTicketService.list()` | **SRS**: SRS-04-011 | **Spec**: §4-3 D-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-038-01 | API | 列表查詢 | authenticated | GET /faults | 200 | HTTP 200 | ✅ FaultTicketControllerTest.list |
| TC-03-038-02 | Error | 未登入 | no token | GET /faults | 401 | HTTP 401 | ✅ FaultTicketControllerTest.list_noToken |
| TC-03-038-03 | Happy | Service 列表 | faults exist | list() | fault list | 分頁 | ✅ FaultTicketServiceTest.list |

---

### FN-03-039 新增障礙工單

**SA**: SA-03 §障礙管理 | **SD**: SD-03 §4 | **API**: `POST /v1/auth/faults`  
**Service**: `FaultTicketService.create()` | **SRS**: SRS-04-011 | **Spec**: §4-3 D-5

**商業規則**：
- 新增狀態 = OPEN
- 觸發 FaultCorrelation 關聯偵測

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-039-01 | API | 新增障礙 | perm=FAULT_MANAGE | POST /faults | 200 | HTTP 200 | ✅ FaultTicketControllerTest.create |
| TC-03-039-02 | Error | 無權限 | no perm | POST /faults | 403 | HTTP 403 | ✅ FaultTicketControllerTest.create_noPermission |
| TC-03-039-03 | Happy | 狀態 OPEN | valid data | create() | status=OPEN | status | ✅ FaultTicketServiceTest.create_statusOpen |
| TC-03-039-04 | Happy | 觸發關聯偵測 | device 有 circuit | create() | correlation check | event published | ✅ FaultTicketServiceTest.create_triggersCorrelation |

---

### FN-03-040 查看障礙詳情

**SA**: SA-03 §障礙管理 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/faults/{id}`  
**Service**: `FaultTicketService.getById()` | **SRS**: SRS-04-011 | **Spec**: §4-3 D-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-040-01 | API | 查看詳情 | fault exists | GET /faults/{id} | 200 | HTTP 200 | ✅ FaultTicketControllerTest.getById |
| TC-03-040-02 | Happy | Service 查詢 | fault exists | getById() | fault detail | 含 device info | ✅ FaultTicketServiceTest.getById_found |
| TC-03-040-03 | Error | 不存在 | invalid id | getById() | not found | errorCode | ✅ FaultTicketServiceTest.getById_notFound |

---

### FN-03-041 解決障礙

**SA**: SA-03 §障礙管理 | **SD**: SD-03 §4 | **API**: `PUT /v1/auth/faults/{id}/resolve`  
**Service**: `FaultTicketService.resolve()` | **SRS**: SRS-04-011 | **Spec**: §4-3 D-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-041-01 | API | 解決障礙 | perm=FAULT_MANAGE | PUT /faults/{id}/resolve | 200 | HTTP 200 | ✅ FaultTicketControllerTest.resolve |
| TC-03-041-02 | Error | 無權限 | no perm | PUT /faults/{id}/resolve | 403 | HTTP 403 | ✅ FaultTicketControllerTest.resolve_noPermission |
| TC-03-041-03 | Happy | Service 解決 | status=OPEN | resolve() | status=RESOLVED | status | ✅ FaultTicketServiceTest.resolve |
| TC-03-041-04 | Error | 不存在 | invalid id | resolve() | not found | errorCode | ✅ FaultTicketServiceTest.resolve_notFound |

---

### FN-03-042 ~ FN-03-044 關聯障礙

**SA**: SA-03 §關聯障礙 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/fault-correlations`  
**SRS**: SRS-04-011 | **Spec**: §4-3 D-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-042-01 | Happy | 查詢關聯列表 | correlations exist | GET /fault-correlations | 200 | list | ⬜ 待補 |
| TC-03-043-01 | Happy | 確認關聯 | correlation exists | PUT confirm | 200 | confirmed | ⬜ 待補 |
| TC-03-044-01 | Happy | 解決關聯 | correlation confirmed | PUT resolve | 200 | resolved | ⬜ 待補 |

---

### FN-03-045 回路關聯偵測（被動）

**SA**: SA-03 §關聯偵測 | **SD**: SD-03 §4 | **API**: (內部)  
**Service**: `FaultCorrelationService` | **SRS**: SRS-04-011 | **Spec**: §4-3 D-5

**商業規則**：
- 同回路障礙數 ≥ 閾值 → 產生關聯事件
- 無 circuitId / deviceId → 跳過
- Gateway 連線異常偵測

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-045-01 | Happy | 達閾值 → 關聯 | circuit 故障數 ≥ threshold | correlate() | 產生 correlation | record created | ✅ FaultCorrelationServiceTest.circuitThresholdMet |
| TC-03-045-02 | Happy | 未達閾值 | circuit 故障數 < threshold | correlate() | 不產生 | no record | ✅ FaultCorrelationServiceTest.circuitBelowThreshold |
| TC-03-045-03 | Edge | null circuitId | circuitId=null | correlate() | 跳過 | no exception | ✅ FaultCorrelationServiceTest.nullCircuitId |
| TC-03-045-04 | Edge | null deviceId | deviceId=null | correlate() | 跳過 | no exception | ✅ FaultCorrelationServiceTest.nullDeviceId |
| TC-03-045-05 | Happy | Gateway 連線偵測 | gateway disconnected | correlate() | 產生 connectivity alert | alert type | ✅ FaultCorrelationServiceTest.gatewayConnectivity |

---

### FN-03-046 Gateway 心跳偵測

**SA**: SA-03 §偵測 | **SD**: SD-03 §4 | **API**: (排程)  
**SRS**: SRS-04-011

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-046-01 | Happy | 心跳逾時 | lastHeartbeat > threshold | 排程觸發 | 產生障礙工單 | fault created | ⬜ 待實作 |

---

### FN-03-047 SIM 到期預警

**SA**: SA-03 §偵測 | **SD**: SD-03 §4 | **API**: (排程)  
**SRS**: SRS-04-011

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-047-01 | Happy | SIM 即將到期 | simExpiry < now+30d | 排程觸發 | 通知 | notification | ⬜ 待實作 |

---

## 6. 資產異動 (FN-03-048 ~ FN-03-052)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-03-048 | 資產加帳申請 | TC-03-048-01 | 新增加帳 → workflow | ⬜ 待實作 |
| FN-03-049 | 資產除帳申請 | TC-03-049-01 | 新增除帳 → workflow | ⬜ 待實作 |
| FN-03-050 | 資產變更申請 | TC-03-050-01 | 新增變更 → workflow | ⬜ 待實作 |
| FN-03-051 | 批次異動申請 | TC-03-051-01 | 批次 CSV → workflow | ⬜ 待實作 |
| FN-03-052 | 異動申請列表 | TC-03-052-01 | 查詢列表 | ⬜ 待實作 |

---

## 7. 資產清冊匯出 (FN-03-053 ~ FN-03-055)

### FN-03-053 ~ FN-03-055 匯出

**SA**: SA-03 §匯出 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/devices/export`  
**SRS**: SRS-04-013 | **Spec**: §4-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-053-01 | API | 匯出 | perm=DEVICE_EXPORT | GET /devices/export | 200 | HTTP 200 | ✅ DeviceControllerTest.export |
| TC-03-053-02 | Error | 無權限 | no perm | GET /devices/export | 403 | HTTP 403 | ✅ DeviceControllerTest.export_noPermission |
| TC-03-054-01 | Happy | XLSX 格式 | — | format=xlsx | Excel file | Content-Type | ⬜ 待補 |
| TC-03-055-01 | Happy | ODS 格式 | — | format=ods | ODS file | Content-Type | ⬜ 待實作 |

---

## 8. 驗收文件 (FN-03-056 ~ FN-03-059)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-03-056 | 上傳驗收文件 | TC-03-056-01 | 上傳 + 關聯契約 | ⬜ 待實作 |
| FN-03-057 | 驗收文件列表 | TC-03-057-01 | 查詢列表 | ⬜ 待實作 |
| FN-03-058 | 下載驗收文件 | TC-03-058-01 | 檔案下載 | ⬜ 待實作 |
| FN-03-059 | 刪除驗收文件 | TC-03-059-01 | 刪除 + 權限檢查 | ⬜ 待實作 |

---

### FN-03-060 設備分類統計

**SA**: SA-03 §統計 | **SD**: SD-03 §4 | **API**: `GET /v1/auth/devices/statistics`  
**SRS**: SRS-04-013 | **Spec**: §4-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-060-01 | Happy | 分類統計 | devices exist | GET /devices/statistics | 200, by category | count per type | ⬜ 待實作 |

---

## 9. 回路查詢補充

### CircuitController 其他 API

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-C-01 | API | 查詢回路 | circuit exists | GET /circuits/{id} | 200 | HTTP 200 | ✅ CircuitControllerTest.getById |
| TC-03-C-02 | Error | 回路不存在 | invalid id | GET /circuits/{id} | 404 | HTTP 404 | ✅ CircuitControllerTest.getById_notFound |
| TC-03-C-03 | Happy | Service getById | circuit exists | getById() | circuit detail | 含 deviceCount | ✅ CircuitServiceTest.getById_found |
| TC-03-C-04 | Error | Service not found | invalid id | getById() | error | errorCode | ✅ CircuitServiceTest.getById_notFound |

### ContractController 其他 API

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-CT-01 | API | 查詢契約 | contract exists | GET /contracts/{id} | 200 | HTTP 200 | ✅ ContractControllerTest.getById |
| TC-03-CT-02 | Error | 契約不存在 | invalid id | GET /contracts/{id} | 404 | HTTP 404 | ✅ ContractControllerTest.getById_notFound |
| TC-03-CT-03 | Happy | Service getById | contract exists | getById() | contract detail | 含 status | ✅ ContractServiceTest.getById_found |
| TC-03-CT-04 | Error | Service not found | invalid id | getById() | error | errorCode | ✅ ContractServiceTest.getById_notFound |

### Device 其他 API

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-03-D-01 | API | 除帳設備 | device exists | PUT /devices/{id}/decommission | 200 | HTTP 200 | ✅ DeviceControllerTest.decommission |
| TC-03-D-02 | Happy | Service 除帳 | device active | decommission() | status=DECOMMISSIONED | status 更新 | ✅ DeviceServiceTest.decommission |
| TC-03-D-03 | API | 查詢組件 | device exists | GET /devices/{id}/components | 200 | HTTP 200 | ✅ DeviceControllerTest.getComponents |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ✅ 已有自動化 | 73 |
| ⬜ 待補（已實作 FN） | 4 |
| ⬜ 待實作（未實作 FN） | 30 |
| **總 TC 數** | **107** |

### 待補測試優先序

| 優先 | FN | 待建 TC | 原因 |
|------|-----|--------|------|
| 1 | FN-03-010 | 1 TC | 組件替換已實作，缺 API 層測試 |
| 2 | FN-03-028 | 1 TC | 回路設備查詢已實作 |
| 3 | FN-03-042~044 | 3 TC | 關聯障礙 CRUD 已部分實作 |
| 4 | FN-03-048~052 | 5 TC | 資產異動流程（Phase 5+） |
