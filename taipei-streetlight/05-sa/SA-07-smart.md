# SA-07 智慧路燈 Function List

> **對應需求**：§8-(1) ~ §8-(10)  
> **SRS 對應**：SRS-08-001 ~ SRS-08-010  
> **Spec 來源**：`/02-spec/08-smart-streetlight.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-07-01 | IoT 數據串接 | 系統 | 統一 API 收發 IoT 數據 |
| UC-07-02 | 即時監控 | GOV_ADMIN, OPERATOR | 地圖查看設備即時狀態 |
| UC-07-03 | 自動故障偵測 | 系統 | Config-Driven 規則引擎→告警→報修 |
| UC-07-04 | 電表整合 | 系統 | 智慧電表數據介接 |
| UC-07-05 | 調光控制 | GOV_ADMIN, OPERATOR | 單燈/群組/排程調光 |
| UC-07-06 | 連線紀錄查看 | GOV_ADMIN | 設備連線 Log |
| UC-07-07 | 示警設定 | GOV_ADMIN | 依區域設定告警閾值 |
| UC-07-08 | 隧道/地下道監控 | GOV_ADMIN | 照明監控+異常告警 |
| UC-07-09 | Telemetry Format 管理 | GOV_ADMIN | 廠商上傳 JSON Format，零程式碼接入 |
| UC-07-10 | Config-Driven 事件規則 | GOV_ADMIN | UI 設定欄位+運算子+閾值，含複合條件 |
| UC-07-11 | 告警通知配置 | GOV_ADMIN | 設定通知對象、抑制策略、發送管道 |
| UC-07-12 | 無訊號偵測 | 系統 | 設備超時未上報 telemetry→自動觸發離線事件 |

---

## Function List

### IoT 數據串接平台 (§8-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-001 | IoT 設備註冊 | C | GOV_ADMIN | 設備 ID、協定(MQTT/CoAP/REST)、認證資訊 | 註冊結果 | 依 5G 智慧杆技術規範；產生唯一 device token | SRS-08-001 | §8-1 | POST /v1/auth/iot/devices |
| FN-07-002 | IoT 設備列表 | R | GOV_ADMIN | 協定、狀態、分頁 | 設備清單 | — | SRS-08-001 | §8-1 | GET /v1/auth/iot/devices |
| FN-07-003 | 設備數據上行接收 (MQTT) | I | IoT Gateway | topic: device/{id}/telemetry, payload: JSON | 以 JSONB 寫入 telemetry | Token 驗證；依 format 定義解析；時序寫入 | SRS-08-001 | §8-1 | MQTT device/{id}/telemetry |
| FN-07-004 | 設備數據上行接收 (REST) | I | IoT Gateway | device_id、時間戳、數據 JSON | 以 JSONB 寫入 telemetry | 同上 | SRS-08-001 | §8-1 | POST /v1/iot/telemetry |
| FN-07-005 | 設備心跳上報 | I | IoT Gateway | device_id、心跳時間 | 更新 last_heartbeat_at | 更新在線狀態；同時更新 last_telemetry_at | SRS-08-001 | §8-1 | POST /v1/iot/heartbeat |
| FN-07-006 | 指令下行 (MQTT) | E | 系統 | topic: device/{id}/command, payload: JSON | 指令送達確認 | QoS 1；超時重試 | SRS-08-001 | §8-1 | MQTT device/{id}/command |
| FN-07-007 | 數據錯漏偵測 | P | 系統 | telemetry 流 | 異常標記 | 值範圍檢查+時序缺失偵測+權責釐清標記 | SRS-08-001 | §8-1 | (內部) |
| FN-07-008 | 數據補救 (backfill) | I | IoT Gateway | 批次歷史數據 | 寫入 telemetry | Gateway 恢復後批次回補 | SRS-08-001 | §8-1 | POST /v1/iot/telemetry/batch |

### 動態 Telemetry Format 管理 (擴充)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-044 | Telemetry Format 定義建立 | C | GOV_ADMIN | 廠商名稱、設備型號、JSON 範例或欄位清單 | format 紀錄 | 系統解析 JSON 取得欄位名稱+資料型態；一個廠商+型號可有多個版本 | SRS-08-001 | §8-1 | POST /v1/auth/iot/telemetry-formats |
| FN-07-045 | Telemetry Format 定義查詢 | R | GOV_ADMIN | 廠商、型號、分頁 | format 清單 | — | SRS-08-001 | §8-1 | GET /v1/auth/iot/telemetry-formats |
| FN-07-046 | Telemetry Format 定義更新 | U | GOV_ADMIN | format ID、欄位變更 | 更新結果 | 新增欄位允許；刪除欄位需確認無 event_rule 引用 | SRS-08-001 | §8-1 | PUT /v1/auth/iot/telemetry-formats/{id} |
| FN-07-047 | Telemetry Format 欄位清單 | R | GOV_ADMIN | format ID | 欄位名稱+型態清單 | 供事件規則設定頁面下拉選單使用；含系統虛擬欄位（`$idle_minutes`） | SRS-08-001 | §8-1 | GET /v1/auth/iot/telemetry-formats/{id}/fields |

### 即時設備狀態地圖 (§8-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-009 | 設備狀態地圖 | R | GOV_ADMIN, OPERATOR | 範圍、類型 | GeoJSON + 狀態色標 | 色標：離線(灰)/熄燈(紅)/亮燈(綠)/調光(黃)/故障(橘)/維修(藍) | SRS-08-002 | §8-2 | GET /v1/auth/iot/map/status |
| FN-07-010 | 設備狀態即時推送 | N | 系統 | 狀態變更事件 | WebSocket 訊息 | 即時更新地圖圖示 | SRS-08-002 | §8-2 | WebSocket /ws/device-status |
| FN-07-011 | 設備即時 telemetry | R | GOV_ADMIN | 設備 ID | 最新 telemetry | 電壓/電流/功率/亮度/溫度 | SRS-08-002 | §8-2 | GET /v1/auth/iot/devices/{id}/telemetry/latest |
| FN-07-012 | 設備歷史 telemetry | R | GOV_ADMIN | 設備 ID、時間範圍 | telemetry 時序 | 圖表繪製用 | SRS-08-002 | §8-2 | GET /v1/auth/iot/devices/{id}/telemetry/history |

### 自動故障偵測與告警 (§8-3) — Config-Driven 告警引擎

> **設計原則**：所有事件偵測規則均透過 `event_rule` table 設定，不寫死在程式中。
> 欄位來源由 Telemetry Format 定義動態取得，系統虛擬欄位以 `$` 前綴標示。

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-013 | 事件規則管理 (CRUD) | C/U/D | GOV_ADMIN | 規則名稱、條件群組(欄位+運算子+閾值)、AND/OR 邏輯、目標設備/區域、嚴重度 | 規則資料 | 支援單一條件與複合條件 (Compound Rules)；欄位下拉選單從 Format 定義取得 | SRS-08-003 | §8-3 | CRUD /v1/auth/iot/event-rules |
| FN-07-048 | 事件規則條件群組管理 | C/U/D | GOV_ADMIN | rule_id、多筆條件(field, operator, value)、group_logic(AND/OR) | 條件群組資料 | 一個規則可有多個條件群組；群組間預設為 OR；群組內條件預設為 AND | SRS-08-003 | §8-3 | (子資源 /v1/auth/iot/event-rules/{id}/conditions) |
| FN-07-014 | Config-Driven 事件偵測 | P | 系統 | telemetry JSONB + 所有 enabled event_rules | 告警事件 | 收到 telemetry 時遍歷 active rules；從 JSONB 取欄位值；套用運算子比對；支援 `$idle_minutes` 虛擬欄位 | SRS-08-003 | §8-3 | (內部) |
| FN-07-049 | 無訊號偵測 (Scheduler) | P | 系統 | 排程觸發 | 設備離線事件 | 每 N 分鐘掃描 `last_telemetry_at`；超過 event_rule 中 `$idle_minutes` 閾值的設備 → 觸發離線事件 | SRS-08-003 | §8-3 | (排程) |
| FN-07-015 | 告警生成 (含抑制判斷) | C | 系統 | 告警事件 | alert_history 紀錄 | 檢查 suppress_duration_min：同設備+同規則在 cooldown 期間內不重複產生；通過後寫入 alert_history；觸發 E5 自動建 fault_ticket (source=AUTO_ALERT) | SRS-08-003 | §8-3 | (內部) |
| FN-07-050 | 告警通知對象設定 | C/U/D | GOV_ADMIN | rule_id、通知對象(角色/個人/群組) | 設定結果 | 每個規則可綁定多個通知對象 | SRS-08-003 | §8-3 | CRUD /v1/auth/iot/event-rules/{id}/recipients |
| FN-07-051 | 告警通知管道設定 | C/U/D | GOV_ADMIN | rule_id、發送管道(EMAIL/SMS/WS/LINE) | 設定結果 | 每個規則可選擇多個發送管道 | SRS-08-003 | §8-3 | CRUD /v1/auth/iot/event-rules/{id}/channels |
| FN-07-052 | 告警通知發送 (Dispatcher) | P | 系統 | alert_history + 規則的通知配置 | 發送結果 | 依規則設定的對象×管道發送；記錄發送 log；失敗重試 | SRS-08-003 | §8-3 | (內部) |
| FN-07-053 | 告警確認 (Acknowledge) | U | GOV_ADMIN, OPERATOR | alert_id | 狀態更新 | OPEN→ACKNOWLEDGED；記錄確認者與時間 | SRS-08-003 | §8-3 | PUT /v1/auth/iot/alerts/{id}/ack |
| FN-07-054 | 告警解除 (Resolve) | U | GOV_ADMIN, OPERATOR | alert_id | 狀態更新 | ACKNOWLEDGED→RESOLVED；自動計算 MTTR | SRS-08-003 | §8-3 | PUT /v1/auth/iot/alerts/{id}/resolve |
| FN-07-016 | 告警歷史查詢 | R | GOV_ADMIN | 設備、嚴重度、狀態(OPEN/ACK/RESOLVED)、時間、分頁 | 告警清單 | 支援依狀態篩選 | SRS-08-003 | §8-3 | GET /v1/auth/iot/alerts |
| FN-07-017 | 告警統計匯出 | E | GOV_ADMIN | 篩選條件 | ODS/XLS/CSV | — | SRS-08-003 | §8-3 | GET /v1/auth/iot/alerts/export |
| FN-07-018 | 告警即時推送 | N | 系統 | 告警生成 | WebSocket + Email + SMS | 依規則通知配置多管道發送 | SRS-08-003 | §8-3 | WebSocket /ws/alerts |

### 智慧電表整合 (§8-4)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-019 | 電表數據接收 | I | 系統(EXT-09) | 電表 ID、電壓/電流/功率/用電量 | telemetry | 與智能路燈訊號交叉比對 | SRS-08-004 | §8-4 | POST /v1/iot/meter/telemetry |
| FN-07-020 | 區域停電偵測 | P | 系統 | 電表+路燈訊號 | 告警 | 同回路電表異常+路燈離線→區域停電判斷 | SRS-08-004 | §8-4 | (內部) |
| FN-07-021 | 回路跳脫偵測 | P | 系統 | 電表訊號 | 告警 | 電表正常但回路電流=0→跳脫 | SRS-08-004 | §8-4 | (內部) |
| FN-07-022 | 電表狀態總覽 | R | GOV_ADMIN | — | 在線/離線/異常統計 | — | SRS-08-004 | §8-4 | GET /v1/auth/iot/meters/status |

### 調光控制 (§8-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-023 | 單燈即時調光 | P | GOV_ADMIN, OPERATOR | 設備 ID、亮度(0-100%) | 指令結果 | 非同步指令+確認回報；超時→重試→告警 | SRS-08-005 | §8-5 | POST /v1/auth/iot/dimming/instant |
| FN-07-024 | 群組即時調光 | P | GOV_ADMIN | 群組 ID / 區域、亮度 | 指令結果 | 批次下發；逐燈回報 | SRS-08-005 | §8-5 | POST /v1/auth/iot/dimming/group |
| FN-07-025 | 調光排程管理 | C/U/D | GOV_ADMIN | 排程名稱、目標、亮度、cron/日期 | 排程資料 | 支援單次+重複週期 | SRS-08-005 | §8-5 | CRUD /v1/auth/iot/dimming/schedules |
| FN-07-026 | 調光排程執行 | P | 系統 | 排程觸發 | 批次指令 | cron-based 觸發；記錄執行結果 | SRS-08-005 | §8-5 | (排程) |
| FN-07-027 | Fail-Safe 處理 | P | 系統 | 通訊中斷偵測 | 恢復預設 | 網路中斷→控制器本地排程接管→恢復後同步 | SRS-08-005 | §8-5 | (內部) |
| FN-07-028 | 調光群組管理 | C/U/D | GOV_ADMIN | 群組名稱、設備清單 | 群組資料 | — | SRS-08-005 | §8-5 | CRUD /v1/auth/iot/dimming/groups |
| FN-07-029 | 調光指令紀錄 | R | GOV_ADMIN | 設備/群組、時間、分頁 | 指令歷史 | 含成功/失敗/超時 | SRS-08-005 | §8-5 | GET /v1/auth/iot/dimming/logs |

### 連線紀錄 (§8-6)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-030 | 設備最後連線時間 | R | GOV_ADMIN | 設備 ID | last_heartbeat_at + 狀態 | — | SRS-08-006 | §8-6 | GET /v1/auth/iot/devices/{id}/connection |
| FN-07-031 | 7 天連線 Log 查詢 | R | GOV_ADMIN | 設備 ID | 連線紀錄清單 | 保留 7 天；超過自動清理 | SRS-08-006 | §8-6 | GET /v1/auth/iot/devices/{id}/connection/logs |
| FN-07-032 | 連線統計匯出 | E | GOV_ADMIN | 設備/時間 | 報表 | — | SRS-08-006 | §8-6 | GET /v1/auth/iot/connection/export |

### 示警設定 (§8-7)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-033 | 日間亮燈偵測設定 | U | GOV_ADMIN | 區域、日間時段定義 | 設定結果 | 日間亮燈→異常告警 | SRS-08-007 | §8-7 | PUT /v1/auth/iot/alert-config/daytime |
| FN-07-034 | 夜間異常時段設定 | U | GOV_ADMIN | 區域、夜間時段、異常定義 | 設定結果 | 夜間熄燈/電壓異常→告警 | SRS-08-007 | §8-7 | PUT /v1/auth/iot/alert-config/nighttime |
| FN-07-035 | 訊號中斷閾值設定 | U | GOV_ADMIN | 區域、超時分鐘數 | 設定結果 | 可依區域路段個別設定 | SRS-08-007 | §8-7 | PUT /v1/auth/iot/alert-config/timeout |
| FN-07-036 | 電力變化閾值設定 | U | GOV_ADMIN | 區域、電壓/電流閾值 | 設定結果 | — | SRS-08-007 | §8-7 | PUT /v1/auth/iot/alert-config/power |

### 績效自動計算供給 (§8-8)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-037 | 提供 KPI 數據 | R | 系統(§9) | KPI 指標 ID、時間範圍 | 計算數據 | 自動告警統計/妥善率/回應時間 → 供績效模組 | SRS-08-008 | §8-8 | GET /v1/auth/iot/kpi-data |

### 統計分析 (§8-9)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-038 | 智能路燈用電統計 | R | GOV_ADMIN | 時間範圍、區域 | 用電統計 | — | SRS-08-009 | §8-9 | GET /v1/auth/iot/statistics/power |
| FN-07-039 | 自動告警統計 | R | GOV_ADMIN | 時間範圍 | 告警類型分布 | — | SRS-08-009 | §8-9 | GET /v1/auth/iot/statistics/alerts |
| FN-07-040 | 故障統計 | R | GOV_ADMIN | 時間範圍 | 故障分類分布 | — | SRS-08-009 | §8-9 | GET /v1/auth/iot/statistics/faults |

### 隧道/地下道監控 (§8-10)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-07-041 | 隧道照明狀態監控 | R | GOV_ADMIN | 隧道/地下道 ID | 即時燈況 | 納入統一監控；差異化圖示 | SRS-08-010 | §8-10 | GET /v1/auth/iot/tunnel/{id}/status |
| FN-07-042 | 隧道異常告警 | N | 系統 | 異常偵測 | 告警 | Fail-Safe 設計；資安防護(VPN/加密) | SRS-08-010 | §8-10 | (內部) |
| FN-07-043 | 隧道遠端控制 | P | GOV_ADMIN | 隧道 ID、控制指令 | 執行結果 | 需二次確認；操作日誌 | SRS-08-010 | §8-10 | POST /v1/auth/iot/tunnel/{id}/control |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 即時監控地圖 | /admin/iot/map | 設備狀態+色標+WebSocket | FN-07-009~012 |
| IoT 設備管理 | /admin/iot/devices | 註冊+連線+telemetry | FN-07-001~002, 030~031 |
| 調光控制 | /admin/iot/dimming | 單燈/群組/排程 | FN-07-023~029 |
| Telemetry Format 管理 | /admin/iot/telemetry-formats | 廠商 Format 定義+欄位清單 | FN-07-044~047 |
| 事件規則管理 | /admin/iot/event-rules | Config-Driven 規則+複合條件+通知配置 | FN-07-013, 048~052 |
| 告警管理 | /admin/iot/alerts | 告警歷史+確認+解除+匯出+即時 | FN-07-015~018, 053~054 |
| 示警設定 | /admin/iot/alert-config | 依區域設定閾值 | FN-07-033~036 |
| 電表狀態 | /admin/iot/meters | 電表總覽 | FN-07-019~022 |
| 隧道監控 | /admin/iot/tunnel | 照明狀態+控制 | FN-07-041~043 |
| 統計分析 | /admin/iot/statistics | 用電/告警/故障 | FN-07-038~040 |
