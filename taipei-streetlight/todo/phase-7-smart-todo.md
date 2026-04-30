# Phase 7 — 智慧路燈 (IoT) TODO

> **建立日期**: 2026-04-26  
> **最後更新**: 2026-04-26 (7e1 done)  
> **甘特圖**: 06/16 – 08/15 (9 週)  
> **前置**: Phase 5C GIS 基礎 (PostGIS + OpenLayers)、Phase 4 報修 (fault_tickets)  
> **執行計畫**: 99-plan/2026-04-24-gantt.md §Phase 7  
> **關鍵路徑**: 7a → 7c → 7e1 → 7e2 → 7e3 → 7h（告警引擎主線）；7c → 7d → 7f → 7g（調光主線）  
> **里程碑**: m3 — 2026-08-15

### 架構決策紀錄

| # | 議題 | 決策 | 說明 |
|---|------|------|------|
| D1 | MQTT Broker 選型 | **EMQX** (全程) | 支援百萬級連線、內建 Dashboard、ACL Rule Engine |
| D2 | MQTT Client Library | **Spring Integration MQTT** | 與 Spring Boot 生態一致，Channel-based 抽象 |
| D3 | 基礎設施管理 | **docker-compose.yml** | 統一管理 EMQX + 既有服務 |
| D4 | Flyway 起始版號 | **V70** | 留 gap 給 Phase 6 修補 |
| D5 | IoT 認證架構 | **雙 SecurityFilterChain** | `/v1/iot/**` 走 DeviceTokenAuthFilter；`/v1/auth/**` 走 JWT |
| D6 | Telemetry 分區策略 | **TimescaleDB hypertable** | 自動分區+壓縮+時序查詢優化 |
| D7 | telemetry hypertable 時機 | **7b 普通表，7c 再決定** | 7b 只建 CREATE TABLE + 索引，不呼叫 create_hypertable() |
| D8 | EventRuleEngine 觸發時機 | **7c 發 ApplicationEvent，7e1 接 @EventListener** | ingest() 結尾發 TelemetryIngestedEvent，低耦合不 block 7c |
| D9 | DataQualityEngine min/max 來源 | **field_definitions 可選 min/max，容錯跳過** | 有定義就檢查，沒有就跳過；向後相容既有 format |
| D10 | target_scope areaIds 匹配 | **areaIds = deptId 清單** | 路燈管理以區處 (dept) 為管轄單位，Engine 查 device.deptId ∈ targetScope.areaIds |
| D11 | AlertDispatcher 整合方式 | **橋接既有 NotificationService** | targets→userIds + NotificationPayload(type=ALERT)，委派 NotificationService；SMS/LINE 走 NoOp 記 log |
| D12 | autoCreateTicket 時機 | **7e2 實作，呼叫 FaultTicketService** | deviceId + message 建立最小工單 (source=AUTO_ALERT) |
| D13 | NoSignalDetectionJob 掃描策略 | **規則驅動 (查 $idle_minutes 條件)** | 與 EventRule 體系一致，支援不同規則不同閾值 |
| D14 | 逾時設備處理方式 | **走 AlertTriggeredEvent → AlertSuppressionEngine** | 複用 7e2 抑制+通知完整流程，避免重複告警 |
| D15 | 多租戶掃描方式 | **反查 rule.tenantId，逐租戶設 TenantContext** | 確保 tenant filter 正確 |
| D16 | NoSignalProperties 綁定 | **獨立 @ConfigurationProperties** | prefix=iot.no-signal-detection |
| D17 | IoT 地圖端點策略 | **新增 IoT 專用端點** | `/v1/auth/iot/map/status`，複用 GeoJsonResponse，只查 IoT 設備 |
| D18 | 設備顯示狀態計算 | **後端計算 displayStatus** | ONLINE/OFFLINE/FAULT 寫入 GeoJSON properties |
| D19 | WebSocket 推送觸發 | **監聽 TelemetryIngestedEvent** | 複用 DashboardPushService 模式 |
| D20 | 前端範圍 | **7d 只做後端** | 前端 IoTMapView.vue 留後續 |
| D21 | DimmingResult 新增 PENDING | **新增 PENDING** | SD sequence 需要 PENDING 狀態，Flyway V79 |
| D22 | ACK 訂閱模式 | **新增 MQTT inbound channel** | 訂閱 `device/+/ack`，獨立 @ServiceActivator 處理 |
| D23 | groupDim 執行策略 | **逐燈循序下發** | 群組通常 < 50 燈，簡單可靠 |
| D24 | DimmingScheduleJob 觸發策略 | **@Scheduled(fixedRate=60s)** | 與 NoSignalDetectionJob 同模式 |
| D25 | 基本 Timeout 標記 | **7f 包含簡易 timeout job** | PENDING 超過 30s → TIMEOUT（不含重試） |
| D26 | AuditEventType 新增 | **6 個獨立類型** | CREATE/UPDATE/DELETE_DIMMING_GROUP + SCHEDULE |
| D27 | cron 與 one_time_at 互斥 | **Service 層驗證** | 兩者必填一、不可同時有值 |
| D28 | 7g 實作範圍 | **後端可控部分** | Sync API + FAILSAFE log + 設備端寫設計文件 |
| D29 | 恢復同步觸發 | **Event-driven** | telemetry 恢復 → 偵測 TIMEOUT → 自動下發同步 |
| D30 | 電表數據儲存 | **複用 telemetry 表** | POWER_EQUIPMENT 設備走相同 ingest 流程 |
| D31 | PowerOutageDetector 觸發 | **@Scheduled(fixedRate=120s)** | 定期掃描同 NoSignalDetectionJob 模式 |
| D32 | 電表回路關聯 | **devices.circuit_id** | 不新建表，V80 加索引 + ErrorCode |
| D33 | 前端實作範圍 | **骨架** | routes + 空白頁面 + API 函式 + TS 型別 |
| D34 | API 函式組織 | **單一 api/iot.ts** | ~30 endpoints 單檔可控 |
| D35 | TC 覆蓋盤點 | **補缺口** | 已覆蓋打勾；連線/統計/隧道 P2 標 N/A |
| D36 | 7j 前端測試 | **不做** | 骨架階段不測；待完整開發後補 |

---

### 進度總覽

| 區塊 | Gantt ID | 工期 | 進度 | 說明 |
|------|----------|------|------|------|
| 7a IoT Gateway + MQTT | 7a | 8d | ✅ | MQTT broker + Spring Integration |
| 7b DB Schema (10 tables + ALTER) | 7b | 5d | ✅ | V71-V78 + 12 Entity + 12 Repo + 11 Enum |
| 7b2 Telemetry Format CRUD | 7b2 | 3d | ✅ | 4 endpoints + Service + 10 TC |
| 7c 遙測資料接收 (JSONB) + 心跳 | 7c | 8d | ✅ | MQTT/REST 上行 + JSONB 儲存 + DataQualityEngine + 8 TC |
| 7e1 Config-Driven 事件規則引擎 | 7e1 | 8d | ✅ | EventRuleEngine + CRUD 6 endpoints + 20 TC |
| 7e2 告警抑制 + 多管道通知 | 7e2 | 5d | ✅ | Suppression + Dispatcher |
| 7e3 無訊號偵測 (Scheduler) | 7e3 | 3d | ✅ | $idle_minutes 掃描 + NoSignalDetectionJob + 8 TC |
| 7d 即時狀態地圖 (OpenLayers+WS) | 7d | 8d | ✅(BE) | GeoJSON + WebSocket |
| 7f 調光控制 | 7f | 10d | ✅ | 單燈/群組/排程 + ACK + Timeout |
| 7g Fail-Safe 設計 | 7g | 5d | ✅ | DimmingSyncService + sync API + FAILSAFE log |
| 7h 智慧電表整合 | 7h | 5d | ✅ | V80 + MeterController + PowerOutageDetector + 7 TC |
| 7i 前端 10 頁面 | 7i | 35d | ✅(骨架) | 10 Vue pages + api/iot.ts + types/iot.ts + routes |
| 7j 測試 | 7j | 5d | ✅ | 62/82 TC covered; 20 deferred (WS/P2) |

---

## 7a — IoT Gateway 架構 + MQTT Broker (8 天, 06/16–06/25)

> **SD**: SD-07-smart.md §3 mqtt/  
> **SA**: FN-07-001~006  
> **FN 對應**: FN-07-001 (設備註冊), FN-07-003 (MQTT 上行), FN-07-006 (MQTT 下行)

### MQTT Broker 部署

- [x] **Docker Compose 加入 EMQX / Mosquitto** — 選擇一款 MQTT broker
  - 端口: 1883 (MQTT), 8083 (WebSocket), 18083 (管理介面)
  - 認證: device_token 驗證
  - ACL: 每個設備只能 publish 到 `device/{自己id}/telemetry`
- [x] **Spring Integration MQTT 配置** — `MqttConfig.java`
  - `spring-integration-mqtt` dependency
  - Inbound adapter: 訂閱 `device/+/telemetry`
  - Outbound adapter: publish 到 `device/{id}/command`
  - Connection factory: TCP + TLS 可選

### IoT 設備管理 API

- [x] **IoTDeviceController** — FN-07-001~002
  - `POST /v1/auth/iot/devices` — 設備 IoT 註冊（寫入 devices 的 IoT 欄位）
    - 產生唯一 device_token (UUID + HMAC)
    - 設定 connectivity_type, auth_type, network_config
  - `GET /v1/auth/iot/devices` — IoT 設備列表（篩選 device_token IS NOT NULL）
- [x] **IoTDeviceService**
  - `registerIoT(deviceId, request)` — 更新 devices 表的 IoT 欄位
  - `generateDeviceToken()` — 安全 token 產生
  - `listIoTDevices(filter, pageable)` — 篩選 IoT 設備

### MQTT 訊息處理

- [x] **MqttInboundHandler** — FN-07-003
  - `onMessage(topic, payload)` — 解析 topic 取 device_id
  - device_token 驗證
  - 委派 TelemetryService.ingest()
- [x] **MqttCommandPublisher** — FN-07-006
  - `sendCommand(deviceId, command)` — QoS 1 發佈
  - 超時 30s → 重試 × 2

### 權限 + 選單

- [x] **Flyway migration** — IoT 權限
  - IOT_VIEW, IOT_MANAGE, IOT_DIMMING
  - 選單: 智慧路燈 (DIRECTORY) → 6 子頁面
  - 角色綁定: ADMIN 全部; DEPT_ADMIN IOT_VIEW+IOT_MANAGE; OPERATOR IOT_VIEW+IOT_DIMMING

---

## 7b — DB Schema (5 天, 06/16–06/20, 與 7a 並行)

> **SD**: SD-07-smart.md §1  
> **Migration**: V70+ (規劃)

### Flyway Migration

- [x] **V70 — devices 表 IoT 擴充** — ALTER TABLE
  ```sql
  ALTER TABLE devices ADD COLUMN device_token      VARCHAR(200) UNIQUE;
  ALTER TABLE devices ADD COLUMN auth_type          VARCHAR(20);
  ALTER TABLE devices ADD COLUMN firmware_version   VARCHAR(50);
  ALTER TABLE devices ADD COLUMN last_telemetry_at  TIMESTAMP;
  ALTER TABLE devices ADD COLUMN format_id          BIGINT REFERENCES telemetry_formats(id);
  ```
- [x] **V71 — telemetry_formats** — 動態 format 定義
- [x] **V72 — telemetry** — 時序 JSONB 表 + GIN 索引
  - 考慮 PostgreSQL 原生分區 (RANGE by time) 或 TimescaleDB
- [x] **V73 — event_rules + event_rule_conditions** — Config-Driven 事件規則
- [x] **V74 — event_notification_targets + event_notification_channels** — 告警通知配置
- [x] **V75 — alert_history** — 含狀態機 (OPEN/ACK/RESOLVED) + MTTR
- [x] **V76 — alert_notification_log** — 通知發送紀錄
- [x] **V77 — dimming_groups + dimming_schedules + dimming_logs** — 調光三表
- [x] **V78 — alert_configs** — 示警設定
- [x] **V79 — IoT 權限 + 選單** — IOT_VIEW/IOT_MANAGE/IOT_DIMMING + 選單項

### JPA Entity

- [x] **TelemetryFormat** — @Filter("tenantFilter"), TenantAware
- [x] **Telemetry** — time, tenantId, deviceId, formatId, payload (JSONB), qualityFlag
- [x] **EventRule** — 含 conditionLogic, suppressDurationMin, autoCreateTicket
- [x] **EventRuleCondition** — conditionGroup, field, operator, thresholdValue
- [x] **EventNotificationTarget** — targetType (ROLE/USER/GROUP), targetId
- [x] **EventNotificationChannel** — channel (EMAIL/SMS/WEBSOCKET/LINE), config JSONB
- [x] **AlertHistory** — status 狀態機, triggeredValues JSONB, mttrMinutes
- [x] **AlertNotificationLog** — channel, recipient, status, errorMessage
- [x] **DimmingGroup** — deviceIds (BIGINT[])
- [x] **DimmingSchedule** — scheduleCron, oneTimeAt, targetType
- [x] **DimmingLog** — commandType, result, ackAt
- [x] **AlertConfig** — configType, areaScope JSONB, configValue JSONB

### Repository (12)

- [x] TelemetryFormatRepository
- [x] TelemetryRepository (native query for time-series)
- [x] EventRuleRepository
- [x] EventRuleConditionRepository
- [x] EventNotificationTargetRepository
- [x] EventNotificationChannelRepository
- [x] AlertHistoryRepository
- [x] AlertNotificationLogRepository
- [x] DimmingGroupRepository
- [x] DimmingScheduleRepository
- [x] DimmingLogRepository
- [x] AlertConfigRepository

### Enum

- [x] **AlertSeverity** — CRITICAL / WARNING / INFO
- [x] **AlertStatus** — OPEN / ACKNOWLEDGED / RESOLVED
- [x] **ConditionOperator** — GT, GTE, LT, LTE, EQ, NEQ
- [x] **NotificationChannel** — EMAIL / SMS / WEBSOCKET / LINE
- [x] **NotificationTargetType** — ROLE / USER / GROUP
- [x] **DimmingCommandType** — INSTANT / SCHEDULED / FAILSAFE
- [x] **DimmingResult** — SUCCESS / TIMEOUT / FAILED
- [x] **AlertConfigType** — DAYTIME / NIGHTTIME / TIMEOUT / POWER
- [x] **QualityFlag** — OK / SUSPECT / MISSING

---

## 7b2 — Telemetry Format CRUD (3 天, after 7b)

> **SA**: FN-07-044~047  
> **SD**: SD-07-smart.md §4.2b  

### Controller

- [x] **TelemetryFormatController** — 4 endpoints
  - `POST /v1/auth/iot/telemetry-formats` — 建立 (上傳 JSON 範例自動解析欄位)
  - `GET /v1/auth/iot/telemetry-formats` — 列表 (分頁+廠商篩選)
  - `PUT /v1/auth/iot/telemetry-formats/{id}` — 更新 (新增欄位允許; 刪除欄位需檢查引用)
  - `GET /v1/auth/iot/telemetry-formats/{id}/fields` — 欄位清單 (含 $idle_minutes 虛擬欄位)

### Service

- [x] **TelemetryFormatService**
  - `create(request)` — 解析 JSON sample → 自動產生 field_definitions
  - `update(id, request)` — 檢查 event_rule_conditions 是否引用被刪欄位
  - `getFields(id)` — 回傳廠商欄位 + 系統虛擬欄位 (`$idle_minutes`)
  - `parseJsonSample(json)` — 遞迴解析 JSON key-value → [{name, type, unit}]

---

## 7c — 遙測資料接收 (JSONB) + 心跳 (8 天, after 7a)

> **SA**: FN-07-003~005, 007~008  
> **SD**: SD-07-smart.md §5.1  
> **決策**: D8 (7c 發 TelemetryIngestedEvent, 7e1 接), D9 (field_definitions 可選 min/max)

### DTO

- [x] **TelemetryIngestRequest** — timestamp (可選), payload (Map<String,Object>)
- [x] **TelemetryBatchRequest** — List<TelemetryIngestRequest> records
- [x] **HeartbeatRequest** — (空 body, deviceId 從 token 取)
- [x] **TelemetryResponse** — id, time, deviceId, formatId, payload, qualityFlag

### Controller

- [x] **TelemetryController** — 5 endpoints
  - `POST /v1/iot/telemetry` — REST 上行 (device token auth, 非 JWT) — FN-07-004
  - `POST /v1/iot/telemetry/batch` — 批次回補 — FN-07-008
  - `POST /v1/iot/heartbeat` — 心跳上報 — FN-07-005
  - `GET /v1/auth/iot/devices/{id}/telemetry/latest` — 最新 telemetry (JWT) — FN-07-011
  - `GET /v1/auth/iot/devices/{id}/telemetry/history` — 歷史 telemetry (JWT) — FN-07-012

### Service

- [x] **TelemetryService**
  - `ingest(deviceId, payload, timestamp)` — 核心上行流程
    1. 查詢 device → 取 format_id
    2. DataQualityEngine.check(formatId, payload) → quality_flag
    3. INSERT INTO telemetry (time, device_id, format_id, payload=JSONB)
    4. UPDATE devices SET last_telemetry_at=now(), last_heartbeat_at=now()
    5. 發布 TelemetryIngestedEvent (D8: 7e1 再接 EventRuleEngine)
  - `batchIngest(deviceId, records[])` — 批次回補
  - `heartbeat(deviceId)` — 更新 last_heartbeat_at + last_telemetry_at
  - `getLatest(deviceId)` — 最新一筆 telemetry
  - `getHistory(deviceId, startTime, endTime, pageable)` — 時序查詢

### Spring ApplicationEvent

- [x] **TelemetryIngestedEvent** — deviceId, payload, formatId, qualityFlag (D8 接口)

### 資料品質引擎

- [x] **DataQualityEngine** — FN-07-007 (D9: min/max 可選)
  - `check(formatId, payload)` → QualityFlag
  - 載入 field_definitions → 逐欄位檢查
  - 有 min/max 定義 → 值超出範圍 → SUSPECT
  - 必要欄位缺失 → MISSING
  - 無 min/max 定義 → 跳過 (容錯)

### MQTT 整合

- [x] **MqttInboundHandler** — 解除 TODO 註解，注入 TelemetryService.ingest()

### Device Token 認證

- [x] **DeviceTokenAuthFilter** — 新增 Spring Security filter
  - 從 HTTP Header `X-Device-Token` 取 token
  - 查詢 devices WHERE device_token = ?
  - 設置 SecurityContext (非 JWT, 僅限 /v1/iot/** 路徑)

---

## 7e1 — Config-Driven 事件規則引擎 (8 天, after 7c)

> **SA**: FN-07-013~014, 048  
> **SD**: SD-07-smart.md §3 engine/EventRuleEngine

### Controller

- [x] **EventRuleController** — CRUD + 子資源
  - `POST /v1/auth/iot/event-rules` — 新增規則 (含 conditions 一次建立)
  - `GET /v1/auth/iot/event-rules` — 列表
  - `PUT /v1/auth/iot/event-rules/{id}` — 編輯
  - `DELETE /v1/auth/iot/event-rules/{id}` — 刪除 (CASCADE conditions/targets/channels)
  - `GET /v1/auth/iot/event-rules/{id}/conditions` — 條件群組列表
  - `PUT /v1/auth/iot/event-rules/{id}/conditions` — 批次更新條件群組

### Service

- [x] **EventRuleService**
  - `create(request)` — 建立 rule + conditions (交易)
  - `update(id, request)` — 更新規則
  - `delete(id)` — CASCADE 刪除
  - `updateConditions(ruleId, conditions[])` — 全量替換條件
  - `findActiveRules(deviceType, areaId)` — 查詢 enabled + target_scope 匹配的規則

### 規則匹配引擎

- [x] **EventRuleEngine** — FN-07-014 核心
  - `evaluate(deviceId, payload)` — 主入口
    1. 查詢設備的 deviceType, areaId
    2. `findActiveRules(deviceType, areaId)` → List<EventRule>
    3. 對每個 rule: 載入 conditions → 按 condition_group 分組
    4. 群組內: 從 JSONB payload 取欄位值 → 套用 operator 比對 → AND 合併
    5. 群組間: 依 condition_logic (AND/OR) 合併
    6. 結果為 true → 觸發 AlertTriggeredEvent
  - `resolveFieldValue(field, payload, device)` — 欄位值解析
    - 普通欄位: `payload.get(field)` → 數值轉型
    - 虛擬欄位 `$idle_minutes`: `Duration.between(device.lastTelemetryAt, now()).toMinutes()`
  - `evaluateCondition(value, operator, threshold)` — 單條件比對
    - 支援 >, >=, <, <=, ==, !=

### Compound Rules 測試重點

- [x] 單一條件 (rssi <= -100) → 觸發
- [x] 單一條件未滿足 → 不觸發
- [x] AND 群組全部成立 → 觸發
- [x] AND 群組部分成立 → 不觸發
- [x] OR 多群組任一成立 → 觸發
- [x] disabled 規則 → 跳過
- [x] JSONB 欄位不存在 → 視為不成立 (不拋錯)

---

## 7e2 — 告警抑制 + 多管道通知 (5 天, after 7e1)

> **SA**: FN-07-015, 050~054  
> **SD**: SD-07-smart.md §3 engine/AlertSuppressionEngine, AlertDispatcher

### 告警抑制引擎

- [x] **AlertSuppressionEngine** — FN-07-015
  - `checkSuppression(rule, deviceId)` → boolean (SUPPRESSED / PASS)
    - 查詢 alert_history: 最近一筆同 device_id + 同 rule_id
    - 判斷 `triggered_at + suppress_duration_min > now()` → SUPPRESSED
  - 通過抑制 → AlertService.createAlert()

### 告警 Service

- [x] **AlertService** — alert lifecycle
  - `createAlert(rule, deviceId, severity, triggeredValues)` — INSERT alert_history (status=OPEN)
    - if rule.autoCreateTicket → FaultTicketService.create(source=AUTO_ALERT)
  - `acknowledge(alertId, userId)` — OPEN → ACKNOWLEDGED
  - `resolve(alertId, userId)` — ACKNOWLEDGED → RESOLVED + 計算 mttr_minutes

### 告警通知配置 API

- [x] **EventRuleController (子資源)**
  - `GET /v1/auth/iot/event-rules/{id}/recipients` — 通知對象列表
  - `PUT /v1/auth/iot/event-rules/{id}/recipients` — 更新通知對象
  - `GET /v1/auth/iot/event-rules/{id}/channels` — 通知管道列表
  - `PUT /v1/auth/iot/event-rules/{id}/channels` — 更新通知管道

### 告警通知發送

- [x] **AlertDispatcher** — FN-07-052
  - `dispatch(alert, rule)` — 主入口
    1. 查詢 event_notification_targets → 解析實際收件人 (角色→用戶列表)
    2. 查詢 event_notification_channels → 取得啟用管道
    3. 對每個 收件人 × 管道 發送
    4. 寫入 alert_notification_log
  - Email: 沿用現有 EmailService
  - SMS: 預留介面 (SmsService interface)
  - WebSocket: broadcast `/ws/alerts`
  - LINE: 預留介面

### 告警歷史 + 匯出

- [x] **AlertHistoryController** — FN-07-016~017, 053~054
  - `GET /v1/auth/iot/alerts` — 列表 (status 篩選: OPEN/ACK/RESOLVED)
  - `GET /v1/auth/iot/alerts/export` — 匯出 ODS/XLS/CSV
  - `PUT /v1/auth/iot/alerts/{id}/acknowledge` — 確認
  - `PUT /v1/auth/iot/alerts/{id}/resolve` — 解除

---

## 7e3 — 無訊號偵測 Scheduler (3 天, after 7e2)

> **SA**: FN-07-049  
> **SD**: SD-07-smart.md §3 scheduler/NoSignalDetectionJob

### Scheduler

- [x] **NoSignalDetectionJob** — @Scheduled(fixedDelay = 5min)
  1. 查詢所有 event_rules 中包含 `$idle_minutes` 條件的規則 (D13)
  2. 對每個規則: 提取閾值 (如 120 分鐘)
  3. 查詢 devices WHERE last_telemetry_at < now() - threshold AND device_token IS NOT NULL
  4. 對每個逾時設備 → publish AlertTriggeredEvent (D14)
  5. 走 AlertSuppressionEngine → 完整告警流程

### 配置

- [x] **NoSignalProperties** + application.yml — scheduler 開關 + 間隔 (D16)
  ```yaml
  iot:
    no-signal-detection:
      enabled: true
      interval-ms: 300000  # 5 分鐘
  ```

---

## 7d — 即時狀態地圖 (8 天, after 7c, 與 7e1 並行)

> **SA**: FN-07-009~010  
> **SD**: SD-07-smart.md §3 websocket/

### 後端

- [x] **GET /v1/auth/iot/map/status** — GeoJSON 設備狀態
  - 回傳 FeatureCollection (依 devices 表 lat/lng)
  - properties: id, deviceCode, status, lastHeartbeatAt, lastTelemetryAt
  - 色標: 離線(灰)/熄燈(紅)/亮燈(綠)/調光(黃)/故障(橘)/維修(藍)
- [x] **WebSocket /ws/device-status** — 狀態即時推送
  - DeviceStatusChangedEvent → STOMP broadcast
  - topic: `/topic/tenant/{tenantId}/map/device-status`

### 前端

- [ ] **IoTMapView.vue** — 即時監控地圖頁面
  - 沿用 Phase 5C OpenLayers 架構
  - 設備圖層: VectorLayer + GeoJSON + 狀態色標
  - WebSocket 連線: 收到狀態變更 → 更新對應 feature 圖示
  - 點擊設備 → Popup (即時 telemetry: GET /devices/{id}/telemetry/latest)
  - 圖例 (離線/熄燈/亮燈/調光/故障/維修)

---

## 7f — 調光控制 (10 天, after 7d)

> **SA**: FN-07-023~029  
> **SD**: SD-07-smart.md §5.2

### Controller

- [x] **DimmingController** — 11 endpoints
  - `POST /v1/auth/iot/dimming/instant` — 單燈即時調光
  - `POST /v1/auth/iot/dimming/group` — 群組即時調光
  - `CRUD /v1/auth/iot/dimming/groups` — 調光群組管理 (4 endpoints)
  - `CRUD /v1/auth/iot/dimming/schedules` — 調光排程管理 (4 endpoints)
  - `GET /v1/auth/iot/dimming/logs` — 指令歷史

### Service

- [x] **DimmingService**
  - `instantDim(deviceId, brightness)` — MQTT 下發 + INSERT dimming_logs (PENDING) + 等待 ACK
  - `groupDim(groupId, brightness)` — 批次逐燈下發 + 彙總結果
  - `onAck(logId, success)` — UPDATE dimming_logs SET result + ack_at

### 排程

- [x] **DimmingScheduleJob** — @Scheduled(fixedRate=60s) 掃描
  - 掃描 dimming_schedules WHERE enabled=true AND (cron matches OR one_time_at <= now())
  - 觸發 groupDim / instantDim
  - 記錄結果 → dimming_logs (command_type=SCHEDULED)
- [x] **DimmingTimeoutJob** — @Scheduled(fixedRate=10s) PENDING→TIMEOUT (D25)

---

## 7g — Fail-Safe 設計 (5 天, after 7f)

> **SA**: FN-07-027  
> **SD**: SD-07-smart.md §5.2

- [x] **通訊中斷偵測** — DimmingTimeoutJob (PENDING > 30s → TIMEOUT)
- [x] **控制器本地排程接管** — 設備端 fail-safe 設計文件 (D28: 後端可控部分)
- [x] **恢復同步** — DimmingSyncService + TelemetryIngestedEvent → auto sync (D29)
- [x] **dimming_logs 記錄** — command_type=FAILSAFE_SYNC

---

## 7h — 智慧電表整合 (5 天, after 7e3)

> **SA**: FN-07-019~022  
> **SD**: SD-07-smart.md §5.3

### Controller

- [x] **MeterController**
  - `GET /v1/auth/iot/meters/status` — 電表狀態總覽 (D30: 複用 telemetry 表)

### 停電偵測

- [x] **PowerOutageDetector** — FN-07-020~021 (D31: @Scheduled fixedRate=120s)
  - 掃描同回路電表 + 路燈
  - 電表異常 + 多燈離線 → POWER_OUTAGE (CRITICAL)
  - 電表正常 + 回路電流=0 → CIRCUIT_TRIP (WARNING)
  - 觸發 AlertService.createAlert()

---

## 7i — 前端 10 頁面 (35 天, after 7b2, 與後端並行)

> **SA**: SA-07-smart.md §前端頁面清單

| # | 頁面 | 路由 | FN 對應 | 優先級 |
|---|------|------|---------|--------|
| 1 | 即時監控地圖 | /admin/iot/map | FN-07-009~012 | P0 |
| 2 | IoT 設備管理 | /admin/iot/devices | FN-07-001~002, 030~031 | P0 |
| 3 | Telemetry Format 管理 | /admin/iot/telemetry-formats | FN-07-044~047 | P0 |
| 4 | 事件規則管理 | /admin/iot/event-rules | FN-07-013, 048~052 | P0 |
| 5 | 告警管理 | /admin/iot/alerts | FN-07-015~018, 053~054 | P0 |
| 6 | 調光控制 | /admin/iot/dimming | FN-07-023~029 | P1 |
| 7 | 示警設定 | /admin/iot/alert-config | FN-07-033~036 | P1 |
| 8 | 電表狀態 | /admin/iot/meters | FN-07-019~022 | P1 |
| 9 | 隧道監控 | /admin/iot/tunnel | FN-07-041~043 | P2 |
| 10 | 統計分析 | /admin/iot/statistics | FN-07-038~040 | P2 |

### 前端重點頁面

- [x] **DeviceListView.vue** — IoT 設備管理 (skeleton)
- [x] **TelemetryFormatView.vue** — Format 定義 (skeleton)
- [x] **TelemetryDataView.vue** — 遙測數據 (skeleton)
- [x] **EventRuleView.vue** — 事件規則 (skeleton)
- [x] **AlertHistoryView.vue** — 告警歷史 (skeleton)
- [x] **DimmingControlView.vue** — 調光控制 (skeleton)
- [x] **DimmingGroupView.vue** — 調光群組 (skeleton)
- [x] **DimmingScheduleView.vue** — 調光排程 (skeleton)
- [x] **IoTMapView.vue** — IoT 地圖 (skeleton)
- [x] **MeterStatusView.vue** — 電表總覽 (skeleton)

### 前端共用

- [x] **api/iot.ts** — IoT 模組全部 API 函式 (~30 functions, D34)
- [x] **types/iot.ts** — TypeScript 介面定義
- [ ] **i18n** — zh-TW IoT 相關鍵值 (待完整開發時補)

---

## 7j — 測試 (5 天, after 7g)

> **Test**: TS-07-smart.md (82 TC)

### 後端單元測試

#### Telemetry Format (7 TC) ✅
- [x] TC-07-044-01~03: Format 建立 (自動解析/手動/重複)
- [x] TC-07-045-01: Format 查詢
- [x] TC-07-046-01~02: Format 更新 (新增欄位/刪除被引用欄位)
- [x] TC-07-047-01: 欄位清單含虛擬欄位

#### Config-Driven 事件偵測 (8 TC) ✅
- [x] TC-07-014-01~02: 單一條件觸發/未觸發
- [x] TC-07-014-03~04: AND 群組全部成立/部分成立
- [x] TC-07-014-05: OR 多群組任一成立
- [x] TC-07-014-06: disabled 規則跳過
- [x] TC-07-014-07: target_scope 篩選
- [x] TC-07-014-08: JSONB 欄位不存在

#### 無訊號偵測 (3 TC) ✅
- [x] TC-07-049-01: 設備 2hr 未上報 → 觸發
- [x] TC-07-049-02: 設備 1hr 未上報 → 未觸發
- [x] TC-07-049-03: 恢復上報 → 不再觸發

#### 告警抑制 (5 TC) ✅
- [x] TC-07-015-01: 首次觸發 → alert
- [x] TC-07-015-02: Cooldown 內 → SUPPRESSED
- [x] TC-07-015-03: Cooldown 過後 → 新 alert
- [x] TC-07-015-04: 不同設備同規則 → 各自 alert
- [x] TC-07-015-05: auto_create_ticket → fault_ticket

#### 通知配置 (4 TC) ✅
- [x] TC-07-050-01~02: 通知對象 CRUD
- [x] TC-07-051-01~02: 通知管道 CRUD

#### 通知發送 (3 TC) — 1 ✅ / 2 deferred
- [x] TC-07-052-01: 多管道發送 → log
- [ ] TC-07-052-02: Email 失敗 → FAILED log ⏭️ (integration test)
- [ ] TC-07-052-03: WebSocket 推送成功 ⏭️ (WebSocket)

#### 告警生命週期 (6 TC) ✅
- [x] TC-07-053-01: OPEN → ACK
- [x] TC-07-053-02: 重複 ACK → 400
- [x] TC-07-054-01: ACK → RESOLVED + MTTR
- [x] TC-07-054-02: 未 ACK 直接 Resolve → 400
- [x] TC-07-016-01~02: 告警列表 (狀態篩選/全部)

#### 設備管理 + 數據上行 (9 TC) — 7 ✅ / 2 deferred
- [x] TC-07-001~002: 設備註冊/重複/列表
- [x] TC-07-003~005: MQTT/REST 上行 + 心跳
- [ ] TC-07-006-01: 指令下行 ⏭️ (MQTT integration)
- [ ] TC-07-007-01: 數據錯漏偵測 ⏭️ (internal)
- [x] TC-07-008-01: batch backfill

#### 狀態監控 (4 TC) — 3 ✅ / 1 deferred
- [x] TC-07-009-01: 狀態地圖 GeoJSON
- [ ] TC-07-010-01: WebSocket 狀態變更 ⏭️ (WebSocket)
- [x] TC-07-011-01: 最新 telemetry
- [x] TC-07-012-01: 歷史 telemetry

#### 電表與停電 (4 TC) — 3 ✅ / 1 deferred
- [ ] TC-07-019-01: 電表 telemetry 接收 ⏭️ (D30: 複用 telemetry ingest)
- [x] TC-07-020-01: 區域停電偵測
- [x] TC-07-021-01: 回路跳脫偵測
- [x] TC-07-022-01: 電表總覽

#### 調光控制 (7 TC) — 6 ✅ / 1 deferred
- [x] TC-07-023-01: 單燈調光
- [x] TC-07-024-01: 群組調光
- [x] TC-07-025-01: 排程 CRUD
- [x] TC-07-026-01: 排程觸發
- [ ] TC-07-027-01: Fail-Safe 恢復預設 ⏭️ (設備端)
- [x] TC-07-028-01: 群組管理
- [x] TC-07-029-01: 指令紀錄

#### 連線 & 設定 (7 TC) — N/A (P2)
- [ ] TC-07-030~036 ⏭️ P2

#### 統計 & 隧道 (7 TC) — N/A (P2)
- [ ] TC-07-037~043 ⏭️ P2

---

## 跨模組依賴

| 依賴項 | 模組 | 狀態 | 影響 |
|--------|------|------|------|
| devices 表 | Phase 1 (SD-03) | ✅ 已完成 | ALTER TABLE 新增 IoT 欄位 |
| fault_tickets | Phase 4 (SD-03) | ✅ 已完成 | auto_create_ticket 建單 |
| PostGIS + OpenLayers | Phase 5C (SD-03 §6) | ✅ 已完成 | 即時狀態地圖 |
| EmailService | Phase 1 (通用) | ✅ 已完成 | 告警 Email 發送 |
| WebSocket (STOMP) | Phase 8 (儀表板) | ✅ 已完成 | 共用 /ws endpoint |
| IoT 廠商介面規格 | Track X (tx5) | ⬜ 等待中 | 決定 telemetry format 範例 |

---

## 風險與待決事項

| # | 風險/議題 | 影響 | 緩解方案 |
|---|---------|------|---------|
| R1 | IoT 廠商規格未確認 (tx5, 40d) | telemetry format 不確定 | JSONB 動態 format 已解耦，不影響開發 |
| R2 | MQTT broker 選型 | 效能/授權差異 | EMQX (開源版) 為首選，Mosquitto 為備案 |
| R3 | 16 萬設備 telemetry 寫入量 | DB 效能 (每天 ~4600 萬筆) | PostgreSQL 分區表 (RANGE by month) + GIN 索引 |
| R4 | 告警風暴 (大量設備同時觸發) | 通知淹沒 | suppress_duration_min + Dispatcher 節流 |
| R5 | SMS 第三方 API 選型 | 成本/到達率 | 先預留 SmsService interface，Phase 10 前確認 |
| R6 | 調光指令遺失 | 路燈狀態不一致 | Fail-Safe 設計 + QoS 1 + 超時重試 |

---

## 文件追溯

| 文件 | 路徑 | 用途 |
|------|------|------|
| 需求規格 | 02-spec/08-smart-streetlight.md | 原始需求 §8-(1)~(10) |
| SRS | 03-srs/SRS-08-smart.md | 驗收準則 SRS-08-001~010 |
| SA | 05-sa/SA-07-smart.md | Function List FN-07-001~054 |
| SD | 06-sd/SD-07-smart.md | DB Schema + Class + API + Sequence |
| SD (devices) | 06-sd/SD-03-asset.md | devices 表 IoT 擴充欄位 |
| Test | 09-test-spec/TS-07-smart.md | 82 TC |
| 甘特圖 | 99-plan/2026-04-24-gantt.md | Phase 7 時程 (9 週) |
