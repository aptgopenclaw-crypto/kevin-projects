# TS-07 智慧路燈 — Test Specification (Forward Design)

> **對應 SA**：SA-07-smart (FN-07-001 ~ FN-07-043)  
> **對應 SD**：SD-07-smart  
> **Test Classes**：0 (尚未實作)  
> **Phase**：Phase 5 — 智慧路燈整合

---

## 使用方式

本文件為 **前瞻設計 TC**，用於 Phase 5 實作時的驗收標準。  
所有 TC 均為 ⬜ 狀態，待實作時轉為 ✅。

---

## 1. IoT 設備管理 (FN-07-001 ~ FN-07-002)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-001-01 | FN-07-001 IoT 設備註冊 | 新增 IoT 設備 | POST /iot/devices | 201, deviceToken | 含 token |
| TC-07-001-02 | FN-07-001 | 重複 deviceCode | POST /iot/devices | error | duplicate |
| TC-07-002-01 | FN-07-002 IoT 設備列表 | 查詢 IoT 設備 | GET /iot/devices | 200, list | 分頁 |

---

## 2. 數據上行 (FN-07-003 ~ FN-07-008)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-003-01 | FN-07-003 MQTT 上行 | telemetry JSONB 接收 | MQTT device/{id}/telemetry | 以 JSONB 存入 telemetry | payload 欄位完整 |
| TC-07-003-02 | FN-07-003 | 不同廠商 format 的 JSON | MQTT | 正確關聯 format_id | format_id 正確 |
| TC-07-003-03 | FN-07-003 | 未知 format 的 JSON | MQTT | format_id=NULL，仍存入 raw | quality_flag=SUSPECT |
| TC-07-004-01 | FN-07-004 REST 上行 | REST telemetry (JSONB) | POST /iot/telemetry | 200, stored as JSONB | 持久化 |
| TC-07-004-02 | FN-07-004 | 接收後更新 last_telemetry_at | POST /iot/telemetry | iot_devices.last_telemetry_at 更新 | timestamp 近似 now() |
| TC-07-005-01 | FN-07-005 心跳上報 | heartbeat 接收 | POST /iot/heartbeat | 200, lastSeen 更新 | timestamp |
| TC-07-006-01 | FN-07-006 指令下行 | 下發控制指令 | MQTT command | device ACK | 指令送達 |
| TC-07-007-01 | FN-07-007 數據錯漏偵測 | 偵測到缺失 | (內部) | 產生 backfill request | alert |
| TC-07-008-01 | FN-07-008 數據補救 | batch backfill | POST /iot/telemetry/batch | 200, batch stored | count |

---

## 2b. Telemetry Format 管理 (FN-07-044 ~ FN-07-047)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-044-01 | FN-07-044 Format 建立 | 上傳 JSON 範例自動解析欄位 | POST /iot/telemetry-formats | 201, field_definitions 自動產生 | 欄位名+型態正確 |
| TC-07-044-02 | FN-07-044 | 手動指定欄位清單建立 | POST /iot/telemetry-formats | 201 | field_definitions 匹配輸入 |
| TC-07-044-03 | FN-07-044 | 重複廠商+型號+版本 | POST /iot/telemetry-formats | 409 conflict | unique 約束 |
| TC-07-045-01 | FN-07-045 Format 查詢 | 列表查詢 | GET /iot/telemetry-formats | 200, list | 分頁+篩選 |
| TC-07-046-01 | FN-07-046 Format 更新 | 新增欄位 | PUT /iot/telemetry-formats/{id} | 200 | 新欄位出現在 field_definitions |
| TC-07-046-02 | FN-07-046 | 刪除欄位但被 event_rule 引用 | PUT /iot/telemetry-formats/{id} | 400 | 拒絕刪除被引用欄位 |
| TC-07-047-01 | FN-07-047 欄位清單 | 查詢欄位含系統虛擬欄位 | GET /iot/telemetry-formats/{id}/fields | 200, 含 $idle_minutes | 虛擬欄位存在 |

---

## 3. 狀態監控 (FN-07-009 ~ FN-07-012)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-009-01 | FN-07-009 狀態地圖 | 設備狀態 GeoJSON | GET /iot/map/status | 200, GeoJSON | online/offline/alarm |
| TC-07-010-01 | FN-07-010 即時推送 | WebSocket 狀態變更 | WS /ws/device-status | status change message | event type |
| TC-07-011-01 | FN-07-011 即時 telemetry | 最新讀數 | GET /devices/{id}/telemetry/latest | 200, latest values | timestamp 近 |
| TC-07-012-01 | FN-07-012 歷史 telemetry | 時間區間查詢 | GET /devices/{id}/telemetry/history | 200, time-series | within range |

---

## 4. 告警系統 — Config-Driven 事件引擎 (FN-07-013 ~ FN-07-018, FN-07-048 ~ FN-07-054)

### 4a. 事件規則 CRUD

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-013-01 | FN-07-013 | 新增單一條件規則 (rssi <= -100) | POST /iot/event-rules | 201, rule + 1 condition | condition 欄位正確 |
| TC-07-013-02 | FN-07-013 | 新增複合條件規則 (rssi<=-100 AND voltage<180) | POST /iot/event-rules | 201, rule + 2 conditions in same group | group AND 邏輯 |
| TC-07-013-03 | FN-07-013 | 新增多群組 OR 規則 | POST /iot/event-rules | 201, 2 groups | condition_logic=OR |
| TC-07-013-04 | FN-07-013 | 新增 $idle_minutes 虛擬欄位規則 | POST /iot/event-rules | 201 | field=$idle_minutes |
| TC-07-013-05 | FN-07-013 | 編輯規則閾值 | PUT /iot/event-rules/{id} | 200 | 值已更新 |
| TC-07-013-06 | FN-07-013 | 刪除規則 | DELETE /iot/event-rules/{id} | 200 | cascade 刪除 conditions/targets/channels |
| TC-07-013-07 | FN-07-013 | 禁用/啟用規則 | PUT /iot/event-rules/{id} | 200 | enabled 切換 |

### 4b. Config-Driven 事件偵測

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-014-01 | FN-07-014 | 單一條件觸發 (rssi<=-100) | (內部) | alert 產生 | severity 正確 |
| TC-07-014-02 | FN-07-014 | 單一條件未觸發 (rssi=-80) | (內部) | 無 alert | 無紀錄 |
| TC-07-014-03 | FN-07-014 | 複合條件 AND 全部成立 | (內部) | alert 產生 | 兩個條件都滿足 |
| TC-07-014-04 | FN-07-014 | 複合條件 AND 部分成立 | (內部) | 無 alert | 只一個條件滿足 |
| TC-07-014-05 | FN-07-014 | 多群組 OR 任一成立 | (內部) | alert 產生 | group2 成立即觸發 |
| TC-07-014-06 | FN-07-014 | 規則 enabled=false 不執行 | (內部) | 無 alert | 跳過禁用規則 |
| TC-07-014-07 | FN-07-014 | target_scope 篩選 (只對特定區域) | (內部) | 只對符合區域的設備觸發 | scope 過濾正確 |
| TC-07-014-08 | FN-07-014 | JSONB 欄位不存在於 payload | (內部) | 條件視為不成立 | 不拋錯 |

### 4c. 無訊號偵測

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-049-01 | FN-07-049 | 設備 2hr 未上報 | (排程) | 觸發「設備離線」事件 | alert status=OPEN |
| TC-07-049-02 | FN-07-049 | 設備 1hr 未上報 (未達閾值) | (排程) | 無 alert | 未觸發 |
| TC-07-049-03 | FN-07-049 | 設備離線後恢復上報 | (排程) | 不再觸發新的離線事件 | last_telemetry_at 更新 |

### 4d. 告警抑制 (Suppression)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-015-01 | FN-07-015 | 首次觸發 → 產生 alert | (內部) | alert_history +1 | status=OPEN |
| TC-07-015-02 | FN-07-015 | Cooldown 內再次觸發 (同設備+同規則) | (內部) | 不產生新 alert | SUPPRESSED |
| TC-07-015-03 | FN-07-015 | Cooldown 過後再次觸發 | (內部) | 產生新 alert | alert_history +1 |
| TC-07-015-04 | FN-07-015 | 不同設備同規則同時觸發 | (內部) | 各自產生 alert | 抑制按設備分開 |
| TC-07-015-05 | FN-07-015 | auto_create_ticket=true | (內部) | alert + fault_ticket | fault_ticket_id 不為 NULL |

### 4e. 通知對象 & 管道設定

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-050-01 | FN-07-050 | 設定通知對象 (角色+個人) | PUT /iot/event-rules/{id}/recipients | 200 | target_type 正確 |
| TC-07-050-02 | FN-07-050 | 查詢通知對象 | GET /iot/event-rules/{id}/recipients | 200, list | 包含已設定的對象 |
| TC-07-051-01 | FN-07-051 | 設定 EMAIL + SMS 管道 | PUT /iot/event-rules/{id}/channels | 200 | 2 channels |
| TC-07-051-02 | FN-07-051 | 查詢管道 | GET /iot/event-rules/{id}/channels | 200, list | channel 正確 |

### 4f. 通知發送 (Dispatcher)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-052-01 | FN-07-052 | 告警觸發後多管道發送 | (內部) | alert_notification_log 多筆 | EMAIL+SMS+WS 各一筆 |
| TC-07-052-02 | FN-07-052 | Email 發送失敗記錄 | (內部) | log status=FAILED | error_message 不為空 |
| TC-07-052-03 | FN-07-052 | WebSocket 推送成功 | WS /ws/alerts | 收到 alert 訊息 | severity+device_id |

### 4g. 告警生命週期 (State Machine)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-053-01 | FN-07-053 | 確認告警 (OPEN→ACK) | PUT /iot/alerts/{id}/ack | 200, status=ACKNOWLEDGED | ack_by + ack_at |
| TC-07-053-02 | FN-07-053 | 重複確認已 ACK 的告警 | PUT /iot/alerts/{id}/ack | 400 | 已確認 |
| TC-07-054-01 | FN-07-054 | 解除告警 (ACK→RESOLVED) | PUT /iot/alerts/{id}/resolve | 200, status=RESOLVED | resolved_at + mttr_minutes 自動計算 |
| TC-07-054-02 | FN-07-054 | 尚未 ACK 就試圖 Resolve | PUT /iot/alerts/{id}/resolve | 400 | 必須先確認 |
| TC-07-016-01 | FN-07-016 | 查詢告警列表 (依狀態篩選) | GET /iot/alerts?status=OPEN | 200, list | 只包含 OPEN |
| TC-07-016-02 | FN-07-016 | 查詢告警列表 (全部狀態) | GET /iot/alerts | 200, list | 分頁+排序 |
| TC-07-017-01 | FN-07-017 | 告警匯出 | GET /iot/alerts/export | 200, file | Content-Type |
| TC-07-018-01 | FN-07-018 | WebSocket 即時告警 | WS /ws/alerts | alert message | severity |

---

## 5. 電表與停電 (FN-07-019 ~ FN-07-022)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-019-01 | FN-07-019 電表數據 | 電表 telemetry 接收 | POST /iot/meter/telemetry | 200, stored | 持久化 |
| TC-07-020-01 | FN-07-020 區域停電偵測 | 多設備同時斷電 | (內部) | 停電事件 | zone identified |
| TC-07-021-01 | FN-07-021 回路跳脫偵測 | 回路電流歸零 | (內部) | 跳脫告警 | circuit alert |
| TC-07-022-01 | FN-07-022 電表總覽 | 電表狀態查詢 | GET /iot/meters/status | 200, summary | online count |

---

## 6. 調光控制 (FN-07-023 ~ FN-07-029)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-023-01 | FN-07-023 單燈調光 | 即時調光 0~100% | POST /iot/dimming/instant | 200, ACK | dimLevel |
| TC-07-024-01 | FN-07-024 群組調光 | 群組批次調光 | POST /iot/dimming/group | 200, batch ACK | success count |
| TC-07-025-01 | FN-07-025 排程 CRUD | 新增調光排程 | CRUD /iot/dimming/schedules | 200 | cron + level |
| TC-07-026-01 | FN-07-026 排程執行 | 排程觸發調光 | (排程) | 指令下發 | command sent |
| TC-07-027-01 | FN-07-027 Fail-Safe | 指令逾時 → 安全模式 | (內部) | 恢復預設亮度 | dimLevel=100% |
| TC-07-028-01 | FN-07-028 群組管理 | CRUD 調光群組 | CRUD /iot/dimming/groups | 200 | 含 device list |
| TC-07-029-01 | FN-07-029 指令紀錄 | 查詢調光指令 | GET /iot/dimming/logs | 200, list | 含 status |

---

## 7. 連線 & 設定 (FN-07-030 ~ FN-07-036)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-030-01 | FN-07-030 最後連線 | 查詢 lastSeen | GET /devices/{id}/connection | 200, lastSeen | timestamp |
| TC-07-031-01 | FN-07-031 連線 Log | 7 天連線記錄 | GET /devices/{id}/connection/logs | 200, list | 7 天內 |
| TC-07-032-01 | FN-07-032 連線匯出 | 連線統計匯出 | GET /iot/connection/export | 200, file | Excel |
| TC-07-033-01 | FN-07-033 日間亮燈設定 | 設定日間偵測 | PUT /alert-config/daytime | 200 | startHour/endHour |
| TC-07-034-01 | FN-07-034 夜間異常設定 | 設定夜間偵測 | PUT /alert-config/nighttime | 200 | startHour/endHour |
| TC-07-035-01 | FN-07-035 訊號中斷閾值 | 設定 timeout | PUT /alert-config/timeout | 200 | minutes |
| TC-07-036-01 | FN-07-036 電力變化閾值 | 設定 power delta | PUT /alert-config/power | 200 | threshold % |

---

## 8. 統計 & 隧道 (FN-07-037 ~ FN-07-043)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-07-037-01 | FN-07-037 KPI 數據 | 提供 KPI 原始數據 | GET /iot/kpi-data | 200, data | metric + value |
| TC-07-038-01 | FN-07-038 用電統計 | 智能路燈用電 | GET /iot/statistics/power | 200, by period | kWh aggregated |
| TC-07-039-01 | FN-07-039 告警統計 | 自動告警統計 | GET /iot/statistics/alerts | 200, by severity | count by type |
| TC-07-040-01 | FN-07-040 故障統計 | IoT 故障統計 | GET /iot/statistics/faults | 200, by category | count |
| TC-07-041-01 | FN-07-041 隧道監控 | 隧道照明狀態 | GET /iot/tunnel/{id}/status | 200, status | online/offline |
| TC-07-042-01 | FN-07-042 隧道告警 | 隧道異常偵測 | (內部) | alert | severity |
| TC-07-043-01 | FN-07-043 隧道控制 | 遠端控制 | POST /iot/tunnel/{id}/control | 200, ACK | command sent |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ⬜ 待實作 | 0 |
| ✅ 已覆蓋 (Unit/Integration) | 62 |
| ⏭️ 延後 (WebSocket/內部/P2) | 20 |
| **總 TC 數** | **82** |
