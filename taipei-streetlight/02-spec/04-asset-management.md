# 04 資產管理

> 來源：需求 4-(1) ~ 4-(6) ｜預估工時：20+（GIS 另計）

## 子模組概覽

| 子模組 | 需求項 | 說明 |
|--------|--------|------|
| 地理資訊（GIS） | 4-(1) | 路燈地圖、坐標、圖資匯入匯出 |
| 契約管理 | 4-(2) | 契約資訊、保固、維護權責 |
| 資產資訊 | 4-(3) | 燈桿、燈具、分電箱、附掛物 |
| 資產異動 | 4-(4) | 加帳、除帳、變更、審核流程 |
| 統計報表 | 4-(5) | 資產清冊匯出 |
| 驗收接管 | 4-(6) | 竣工圖、設備規格文件管理 |

---

## 4-1 地理資訊（GIS）

### A. 路燈地圖
- 圖形化介面呈現路燈、分電箱點位及管線位置
- 精確圖資：**ArcGIS** 圖資平台建置及更新
- 行動瀏覽：轉置於開放式圖台（**Google Map / Bing Map / OpenLayers**）
- 效能要求：單一畫面需顯示千分之一地形圖上全數點位（≈ Google Map 縮放等級 15）

### B. 坐標資料整合
- TWD97 二度分帶坐標
- 經緯度（GPS）坐標
- 台電公司「電力坐標」（TWD67 TM2）
- 具備坐標轉換功能

### C. 街景與圖資介接
- 連結 Google Map 街景查閱設施周邊
- 介接：內政部國土署、GIS 管線圖、道路及人行道視覺化平台

### D. 基本圖資
- 圖例、符號說明及管理（向量圖元）
- 底圖：TWD97 臺北市一仟分之一數值地形圖
- 分區範圍：行政區里、分隊分區圖、台電區處、包燈電號分區圖、廠商分區圖
- 機關路燈標準圖（年度更新）

### E. 坐標規範
- 平面基準：TWD97 二度分帶
- 高程基準：TWVD2001
- 規範：內政部「公共設施管線資料標準」、工務局格式
- 路燈及分電箱點位、管線**三維坐標**（含架空管線及回路綁定）
- 差異化呈現平面/高架道路、隧道/車行地下道

### F. 圖資匯入匯出
- 匯入時比對確認正確性，差異需檢視確認
- 匯出格式：GML（地理標誌語言）
- 匯出「臺北市資料大平臺」規定格式
- 可從工務局管線資料庫下載並匯入
- 可匯出指定區域圖資至電腦繪圖軟體（含比例尺、圖面尺寸設定）
- 整合列管建案基地施工範圍

---

## 4-2 契約管理 `工時 20`

### A. 契約資訊 `5`
- 預算年度、採購案號、契約名稱、契約廠商
- 對應資產類別、數量、契約期限、驗收日期與保固年限
- 記錄歷史資料
- 可新增、編輯、刪除、停用契約資訊
- 彈性增加資料欄位及內容

### B. 資產明細連結 `3`
- 資產明細與契約名稱、財物分類連結
- 供維修時查詢保固年限等資訊

### C. 預防性維護排程 `5`
- 分區、分期、周期或日期之編輯管理
- 時間提醒功能

### D. 契約資產轉移 `5`
- 依契約名稱、採購案號篩選資產清單
- 批次方式轉移資產至所屬契約
- 適用保固維護到期後之權責轉移

### E. 資料欄位定義 `2`
- 定義契約管理所需資料欄位格式
- 機關彙整清冊匯入
- 主鍵或非空欄位之預設值與補正方案

### F. 匯出功能 `0`
- 格式：ODS / XLS / CSV

---

## 4-3 資產資訊

### A. 燈桿設備 `工時 10`
- 路燈號碼牌編號、系統編號（流水號）
- 坐標位置（TWD97 + 經緯度 + 台電坐標）
- 設置（更新）年月、財產歸屬
- 財物分類（含「臺北市政府財產管理系統」要求項目）
- 設置位置（路面/人行道、淨寬規定、反光紙）
- 基礎型式（基座式、螺栓式、溝上式）
- 歷來整建及維護契約、權責維護期限、廠商資訊
- 換裝歷程（含照片）、報修歷程（含照片）
- 認養、認捐歷程

### B. 燈桿屬性
- 材質（不鏽鋼、鍍鋅鋼板、鋁桿、木桿、水泥桿）
- 型態（自備桿、附掛台電桿、附掛外牆）
- 樣式（單臂、雙臂、多臂、造型、吸頂、壁掛）
- 高度
- 照明分類（道路、人行道、隧道、公園）

### C. 燈具資料
- 編號、規格（廠牌、型號、光通量、功率、色溫、壽命、出廠年月、燈具 ID、安裝日期、保固期限）
- 光源類別（LED、高壓鈉燈、複金屬燈）
- 控制型態（智能、非智能、定時、光控感應）
- 電源分電箱、回路編號、台電電號與用電類別
- 智能控制器規格（廠牌、型號、控制器 ID、電信商/IP、安裝日期、保固期限）

### D. 分電箱設備
- 編號、坐標、設置年月、類型
- 裝置高度（是否達 1.5 公尺）
- 設置位置、安全檢查紀錄
- 財產歸屬、台電電號、廠商資訊
- 歷來契約、權責維護期限

### E. 電力設備
- 高低壓、UPS、發電機
- 參考分電箱設備內容

### F. 其他智能相關設備/附掛物
- 風光互補、智慧園燈、車流偵測系統等
- 他單位附掛物：申設單位、核准日期、文號、設置期限

### G. 快速搜尋
- 圖形化介面辨識圖示
- 清冊搜尋篩選

---

## 4-4 資產異動
- 單筆或批次加帳、除帳、變更
- 機關審核流程、應備文件
- 依賴：簽核管理模組（03）

## 4-5 統計報表
- 所有資產清冊全欄位匯出（ODS / XLS / CSV）

## 4-6 驗收接管文件管理
- 竣工圖、設備規格、操作維護手冊
- 以連結方式節省主機空間

---

## 設計決策：設備資料模型

> 以下為 4-3 資產資訊的資料模型設計方向，經討論確認。

### D-1 共用欄位 + JSONB 專有欄位

所有 IoT 設備（燈桿、燈具、分電箱、控制器、附掛物等）共用同一張 `devices` 表，依 `device_type` 區分類型，專有欄位以 JSONB 儲存。

**選擇理由**：
- 需求 4-2 E 明確要求「彈性增加資料欄位」— JSONB 天然支持
- PostgreSQL JSONB 可建 GIN index，查詢效能佳
- 共用欄位走強型別 column，保證資料完整性；專有欄位走 JSONB，保持彈性
- 比 EAV（key-value）好查詢，比 Single Table Inheritance（一張大寬表幾十個 nullable column）乾淨
- JPA 端用 `@JdbcTypeCode(SqlTypes.JSON)` + `Map<String, Object>` 或強型別 DTO 皆可

**共用欄位**（所有設備類型皆有）：

| 欄位 | 說明 |
|------|------|
| id | BIGSERIAL PK |
| device_type | 設備類型（POLE / LUMINAIRE / PANEL_BOX / CONTROLLER / ATTACHMENT 等） |
| device_code | 設備編號（路燈號碼牌、分電箱編號等） |
| twd97_x, twd97_y | TWD97 二度分帶坐標 |
| lng, lat | 經緯度（GPS） |
| taipower_coord | 台電坐標（TWD67 TM2） |
| installed_at | 設置（更新）年月 |
| property_owner | 財產歸屬 |
| contract_id | FK → contracts，目前所屬契約 |
| dept_id | FK → depts，管轄部門 |
| tenant_id | 租戶 ID |
| status | 設備狀態（ACTIVE / INACTIVE / DECOMMISSIONED） |
| attributes | **JSONB** — 依 device_type 存放專有欄位 |
| created_at, updated_at | 時間戳 |

**專有欄位（attributes JSONB）範例**：

| device_type | attributes 內容 |
|-------------|----------------|
| POLE（燈桿） | material, pole_type, style, height, lighting_category, foundation_type |
| LUMINAIRE（燈具） | light_source, wattage, color_temp, lumen, lifespan, control_type, circuit_id |
| PANEL_BOX（分電箱） | box_type, mount_height, taipower_account, safety_check_records |
| CONTROLLER（智能控制器） | brand, model, controller_id, carrier, ip, protocol |
| ATTACHMENT（附掛物） | applicant_org, approval_date, doc_number, permit_expiry |

> 高頻查詢的專有欄位（如燈具功率、燈桿高度）可用 PostgreSQL **generated column** 或 **partial index** 加速，不需額外建 extension table。

---

### D-2 設備連線拓撲（直連 vs 閘道）

IoT 設備連線有兩種模式，資料模型需同時支援：

| 模式 | 說明 | 路燈場景範例 |
|------|------|-------------|
| **直連（Direct）** | 設備內建 SIM（4G/5G/NB-IoT），直接上報雲端 | 智慧燈具自帶控制器 + SIM |
| **閘道（Gateway）** | 設備用短距協定（LoRa / Zigbee / BLE / RS-485）連到 Gateway，Gateway 彙整上傳 | 分電箱作為 Gateway，管理回路下所有燈具 |

**拓撲建模**：透過 `parent_device_id` 自關聯表達 Gateway → Device 的樹狀結構：

```
devices
├── parent_device_id  FK → devices(id)   -- 指向上層 Gateway（NULL = 直連或頂層）
├── connectivity_type  ('DIRECT' | 'GATEWAY')
└── network_config JSONB
    ├── 直連: { sim_iccid, imei, ip, carrier, apn }
    └── 閘道設備端: { protocol: 'LoRa', dev_eui, channel }
    └── 閘道本體: { sim_iccid, imei, ip, carrier, apn, max_children }
```

**拓撲範例**：

```
分電箱 A (device_type=PANEL_BOX, connectivity_type=DIRECT, SIM 直連)
├── 燈桿 001 (POLE, connectivity_type=GATEWAY, parent=分電箱A)
│   └── 燈具 001-L (LUMINAIRE, protocol=LoRa)
├── 燈桿 002 (POLE, connectivity_type=GATEWAY, parent=分電箱A)
│   └── 燈具 002-L (LUMINAIRE, protocol=LoRa)
└── 燈桿 003 (POLE, connectivity_type=GATEWAY, parent=分電箱A)
    └── 燈具 003-L (LUMINAIRE, protocol=RS-485)
```

### D-3 電力回路 vs 通訊拓撲（雙線關係）

**重要**：同一回路的燈不一定走同一 Gateway，兩者必須分開建模。

| 關係 | 欄位 | 用途 |
|------|------|------|
| **通訊拓撲** | `parent_device_id` → devices(id) | Gateway 歸屬，影響連線監控、韌體更新 |
| **電力回路** | `circuit_id` → circuits(id) | 分電箱 + 回路編號，影響開關控制、電費計算 |

```
circuits
├── id BIGSERIAL PK
├── panel_box_device_id  FK → devices(id)  -- 所屬分電箱
├── circuit_number       VARCHAR            -- 回路編號
├── taipower_account     VARCHAR            -- 台電電號
├── usage_type           VARCHAR            -- 用電類別
└── tenant_id
```

### D-4 額外考量

| 議題 | 說明 |
|------|------|
| **Gateway 健康監控** | Gateway 本身也是 device，需心跳/在線狀態欄位（`last_heartbeat_at`） |
| **離線容錯** | Gateway 斷線時設備本地快取數據，恢復後批次上傳；需定義重傳策略 |
| **設備層級查詢** | PostgreSQL recursive CTE 查詢某 Gateway 下所有子設備 |
| **歷程追蹤** | 換裝/報修歷程另開 `device_events` 表，附照片 URL |

---

### D-5 關聯障礙偵測（Fault Correlation）

> 實務情境：某條電力回路跳電 → 回路下 20 盞燈同時熄滅 → 民眾/巡檢只回報其中 3~5 盞 → 維修人員逐盞排查浪費時間。
> 根因只有一個：**回路級故障**。系統應自動關聯，避免產生大量重複工單。

#### 資料模型

```
fault_tickets（障礙工單 — 單筆報修或自動告警）
├── id              BIGSERIAL PK
├── device_id       FK → devices(id)         -- 報修的具體設備
├── circuit_id      FK → circuits(id)        -- 系統自動回填（從 device 關聯取得）
├── correlation_id  FK → fault_correlations  -- 歸因到哪個關聯事件（NULL = 獨立工單）
├── source          ('CITIZEN_REPORT' | 'PATROL' | 'AUTO_ALERT')
├── status          ('OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'MERGED')
├── description     TEXT
├── reported_at     TIMESTAMP
├── tenant_id
└── ...

fault_correlations（關聯障礙 — 將多筆工單歸因到同一根因）
├── id                BIGSERIAL PK
├── root_cause_type   ('CIRCUIT' | 'PANEL_BOX' | 'GATEWAY' | 'POWER_OUTAGE')
├── root_cause_id     BIGINT    -- 指向 circuit_id 或 device_id（分電箱/Gateway）
├── affected_count    INT       -- 受影響設備數
├── status            ('DETECTED' | 'CONFIRMED' | 'RESOLVED')
├── detected_at       TIMESTAMP
├── resolved_at       TIMESTAMP
├── tenant_id
└── ...
```

#### 雙路徑故障模型：電力 vs 通訊

一個 IoT 設備同時依賴**兩條獨立路徑**，任一中斷都會產生障礙，但表現不同：

| | 電力路徑 | 通訊路徑 |
|---|---|---|
| 上游 | 分電箱 → 回路（circuit） | Gateway → SIM → 雲端 |
| 故障表現 | 燈不亮（物理滅燈） | 設備離線（telemetry 中斷，**燈可能還亮**） |
| 使用者感知 | 民眾 / 巡檢可目視發現 | **不可感知** — 只有系統監控能偵測 |
| 偵測方式 | 民眾報修 + 巡檢通報 | `last_heartbeat_at` 超過閾值（如 15 分鐘） |
| 根因 | 回路跳電、線路斷、分電箱故障 | Gateway 當機、SIM 到期/欠費、APN 異常、ISP 斷線 |
| 復原後行為 | 來電即恢復 | Gateway 恢復後**批次回補**（backfill）歷史 telemetry |

> **關鍵差異**：通訊故障造成的「設備離線」不會有民眾報修，只能靠系統主動偵測。且 SIM 到期/流量耗盡屬於**可預測故障**，應提前告警。

#### 自動關聯觸發條件

| 維度 | 觸發規則 | 根因類型 | 偵測方式 |
|------|----------|----------|----------|
| **電力回路** | 同 `circuit_id` 下，30 分鐘內 ≥ 3 筆障礙報修 | `CIRCUIT` | 被動（報修驅動） |
| **通訊 Gateway** | 同 `parent_device_id` 下，≥ 50% 子設備心跳超時 | `GATEWAY` | 主動（心跳掃描） |
| **地理區域** | 同 GPS 範圍內（如 500m 半徑）多設備異常 | `POWER_OUTAGE`（區域停電/施工挖斷） | 混合 |

#### 觸發後動作

1. 建立 `fault_correlation` 記錄
2. 該回路/Gateway 下所有設備標記 `suspected_fault = true`
3. 已存在的個別工單狀態設為 `MERGED`，指向 `correlation_id`
4. 產生一張「回路/Gateway 障礙」工單，取代後續個別工單
5. 推播通知維修人員：「回路 A-03 疑似故障，影響 20 盞燈」
6. 維修人員優先檢查分電箱/回路，而非逐盞修

#### 三維度關聯分析總覽

```
─────────── 被動路徑（報修驅動）───────────
障礙報修進入
    │
    ├─ 比對 circuit_id → 同回路近期是否有其他報修？ ──→ 回路級障礙（CIRCUIT）
    │
    ├─ 比對 parent_device_id → 同 Gateway 下是否多設備離線？ ──→ Gateway 故障（GATEWAY）
    │
    └─ 比對 GPS 座標 → 同區域是否密集報修？ ──→ 區域停電/施工（POWER_OUTAGE）

─────────── 主動路徑（心跳掃描）───────────
定時排程（每 5 分鐘）
    │
    ├─ Gateway 本體心跳超時？ ──→ Gateway 離線告警（AUTO_ALERT + GATEWAY）
    │
    ├─ Gateway 在線但子設備 ≥ 50% 離線？ ──→ 通訊異常告警
    │
    └─ SIM 到期日 ≤ 30 天？ ──→ 預警通知（不建工單，推播提醒）
```

#### Gateway 通訊故障處理流程

```
Gateway 心跳超時（15 分鐘無回報）
    │
    ├─ 1. 標記 Gateway status = OFFLINE
    ├─ 2. 查詢所有 parent_device_id = Gateway 的子設備
    ├─ 3. 子設備若也心跳超時 → 標記為 COMM_LOST（通訊中斷，非設備故障）
    ├─ 4. 建立 fault_correlation: root_cause_type='GATEWAY', affected_count=子設備數
    ├─ 5. 自動建立 AUTO_ALERT 工單，source='AUTO_ALERT'
    └─ 6. 推播通知：「Gateway PB-A01 離線，影響 30 台設備，請檢查 SIM / 網路」

Gateway 恢復上線
    │
    ├─ 1. 接收到心跳 → status = ONLINE
    ├─ 2. 子設備陸續回報 → 逐一恢復狀態
    ├─ 3. 處理 backfill 資料（Gateway 離線期間子設備快取的 telemetry）
    └─ 4. 若所有子設備恢復 → correlation status='RESOLVED', 自動結案
```

> **注意**：此功能與 05 維修管理模組高度關聯，障礙工單的完整生命週期（派工、施工、結案）在 05 中定義；此處僅定義資料模型與自動關聯邏輯。
