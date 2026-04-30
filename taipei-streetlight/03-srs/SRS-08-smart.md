# SRS-08 智能路燈管理

> **對應需求**：§8-(1) ~ §8-(10)  
> **設計參照**：`/02-spec/08-smart-streetlight.md`  
> **狀態**：❌ 全模組未開始

---

## SRS-08-001 IoT 統一 API 整合平台

**來源**：§8-(1)

### User Story

> 身為**系統**，需依經濟部「5G 智慧杆系統技術規範」，整合全市既有及未來新增之智能路燈數據，提供統一 API 介面。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-001-1 | 制訂符合 5G 智慧杆技術規範的數據串接方案 |
| AC-08-001-2 | 定義統一 API，支援全市智能路燈上下行通訊 |
| AC-08-001-3 | 可介接多區不同廠商的智能路燈系統 |
| AC-08-001-4 | 具備數據錯漏偵測機制（Checksum / 序號比對） |
| AC-08-001-5 | 發生數據錯漏時可快速辨析權責方，提供回補機制 |

### 技術設計

- **協議**：MQTT Broker（主要）+ CoAP（低功耗）+ HTTP REST（控制指令）
- **資料流**：IoT Device → MQTT Broker → Message Consumer → 時序資料庫 + PostgreSQL
- **時序資料庫**：TimescaleDB（擴充 PostgreSQL）或 InfluxDB
- **即時推送**：WebSocket → 前端地圖/告警

### 資料模型

```
-- 擴充 devices 表
devices: connectivity_type(NONE/DIRECT/GATEWAY), network_config(JSONB),
         parent_device_id(通訊拓撲), last_heartbeat_at

-- 新增表
device_telemetry: device_id, metric_type, value, unit, reported_at  (時序表)
device_status: device_id, online, current_state(OFF/ON_FULL/DIMMED/FAULT), 
               brightness_pct, power_watts, updated_at
connection_logs: device_id, event_type(CONNECT/DISCONNECT), timestamp, reason
```

### 狀態：❌ 未開始

---

## SRS-08-002 路燈地圖即時狀態

**來源**：§8-(2)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-002-1 | 路燈地圖以不同顏色圖示呈現設備即時狀態 |
| AC-08-002-2 | 狀態含：離線（灰）、熄燈（黑）、亮燈全亮（綠）、調光（藍）、故障（紅）、維修中（橘） |
| AC-08-002-3 | 狀態即時更新（≤ 30 秒延遲） |
| AC-08-002-4 | 點擊圖示可查看設備詳情 |

### 技術設計

- 前端：WebSocket 接收狀態變更 → 更新地圖圖層 icon
- 後端：MQTT Consumer 收到 telemetry → 更新 device_status → 推送 WebSocket

### 狀態：❌ 未開始

---

## SRS-08-003 故障自動判斷與告警

**來源**：§8-(3)

### 主要流程

1. 設備回傳異常訊號（電壓/電流/功率異常、離線超時）
2. 系統依 alert_rules 自動判斷故障類別
3. 產生告警（device_alerts），推送至相關人員
4. 可在平台設定條件，觸發自動報修或人工報修

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-003-1 | 依設備回傳訊號自動判斷故障類別 |
| AC-08-003-2 | 告警以報表/圖表呈現並可匯出 |
| AC-08-003-3 | 可設定條件自動建立報修案件 |
| AC-08-003-4 | 可設定條件人工確認後報修 |

### 資料模型

```
alert_rules: id, rule_name, device_type, condition_expr, severity(INFO/WARNING/CRITICAL),
             auto_create_ticket(boolean), enabled, tenant_id
device_alerts: id, device_id, rule_id, alert_type, severity, message, 
               acknowledged, acknowledged_by, ticket_id, created_at
```

### 故障關聯引擎（已設計，對應 fault_correlations）

| 觸發條件 | 故障類型 |
|---------|---------|
| 同一回路 ≥3 報修/30 分鐘 | CIRCUIT 回路故障 |
| 同一 Gateway ≥50% 子設備離線 | GATEWAY 閘道器故障 |
| 地理群聚（半徑 500m 內 ≥5 故障） | POWER_OUTAGE 區域停電 |

### 狀態：❌ 未開始（fault_correlations 資料模型已設計）

---

## SRS-08-004 智慧電表整合

**來源**：§8-(4)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-004-1 | 整合智慧電表回傳數據 |
| AC-08-004-2 | 偵測區域性停電並自動告警 |
| AC-08-004-3 | 偵測電源回路跳脫並自動告警 |
| AC-08-004-4 | 自動區分故障原因：停電 vs 回路跳脫 vs 單燈故障 |

### 外部介接

| 系統 | 協議 | 狀態 |
|------|------|------|
| 智慧電表 | MQTT / Modbus | EXT-11，待確認 |

### 狀態：❌ 未開始

---

## SRS-08-005 調光控制

**來源**：§8-(5)

### User Story

> 身為 **GOV_STAFF**，我可透過平台對單燈或群組執行即時調光（含點滅），也可建立排程自動調光，系統需有 Fail-Safe 機制。

### 主要流程

1. **即時調光**：選擇單燈/群組 → 設定亮度百分比 → 下發指令 → 確認回報
2. **排程調光**：建立排程（時間、週期、目標設備、亮度）→ 到時間自動下發
3. **Fail-Safe**：
   - 控制器本地保存排程，網路斷線時按本地排程執行
   - 包燈用電非 24 小時，需考慮供電時段
   - 指令下發失敗自動重試（佇列 + 指數退避）

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-005-1 | 可對單燈即時調光（含開/關/指定亮度%） |
| AC-08-005-2 | 可對群組（回路/區域/自訂群組）即時調光 |
| AC-08-005-3 | 可建立排程調光（單次/重複週期） |
| AC-08-005-4 | Fail-Safe：控制器本地排程備份 |
| AC-08-005-5 | 指令下發失敗自動重試（≤ 3 次） |
| AC-08-005-6 | 考慮包燈非 24h 供電與網路延遲 |

### 資料模型

```
dimming_schedules: id, name, target_type(DEVICE/CIRCUIT/GROUP), target_id,
                   brightness_pct, schedule_type(ONCE/DAILY/WEEKLY), 
                   cron_expr, enabled, tenant_id
dimming_commands: id, schedule_id, device_id, brightness_pct, 
                  status(PENDING/SENT/ACK/FAILED/RETRY), retry_count,
                  sent_at, ack_at
```

### 狀態：❌ 未開始

---

## SRS-08-006 連線 Log 與最後連線時間

**來源**：§8-(6)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-006-1 | 可查看每台設備最後一次連線時間 |
| AC-08-006-2 | 可查看最近 7 天連線 Log 紀錄 |
| AC-08-006-3 | 連線紀錄可以報表/圖表呈現並匯出 |

### 狀態：❌ 未開始

---

## SRS-08-007 告警閾值分區設定

**來源**：§8-(7)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-007-1 | 提供操作介面設定示警條件（日間亮燈時段、夜間異常時段、中斷時間、電力變化） |
| AC-08-007-2 | 可依不同區域/路段個別設定 |
| AC-08-007-3 | 設定變更立即生效 |

### 狀態：❌ 未開始

---

## SRS-08-008 績效自動計算銜接

**來源**：§8-(8)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-008-1 | 自動計算智能路燈運作維護績效 |
| AC-08-008-2 | 計算結果提供給 SRS-09（績效管理模組）做為計分來源 |

### 狀態：❌ 未開始

---

## SRS-08-009 用電/告警/故障統計分析

**來源**：§8-(9)  
**驗收**：用電統計、自動告警統計、故障統計分析功能。  
**狀態**：❌ 未開始

---

## SRS-08-010 隧道/地下道監視

**來源**：§8-(10)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-08-010-1 | 隧道/地下道照明系統納入監視與異常告警 |
| AC-08-010-2 | 遠端控制需有資安防護（認證 + 加密） |
| AC-08-010-3 | Fail-Safe 設計：通訊中斷時維持最近一次正常狀態 |

### 狀態：❌ P2
