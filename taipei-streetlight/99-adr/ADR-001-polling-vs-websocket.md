# ADR-001: Polling vs WebSocket 策略

> **日期**：2026-04-24  
> **狀態**：已決議  
> **參與者**：Kevin + AI  
> **關聯規格**：§2-10 通知、§8 智慧路燈、§11 儀表板、§4 資產管理

---

## 背景

通知引擎（FN-00-014~019）後端 + 前端已完成，但需要釐清：
1. 前端更新策略用 Polling 還是 WebSocket？
2. 設備 telemetry 的資料流如何設計？
3. WebSocket 基礎設施已建好（STOMP + SockJS + JWT），何時啟用？

---

## 決議

### 核心原則：混用策略，按場景選擇

不是「全 Polling」也不是「全 WebSocket」，而是依據資料特性分層。

### 一、前端更新策略

| 場景 | 方式 | 更新頻率 | 原因 | 實作階段 |
|------|------|---------|------|---------|
| 通知鈴鐺（未讀數） | **Polling** | 30~60s | 已實作，低頻數據足夠 | ✅ 已完成 |
| Dashboard 統計面板 | **Polling** | 30~60s | 聚合數據，變化慢 | Phase 5 |
| 地圖初始載入 | **Polling** (GET 全量) | 進頁面一次 | 載入所有設備狀態 | Phase 5 |
| 地圖狀態差異更新 | **WebSocket push** | 即時 | 5000+ 設備只推變化，避免全量拉取浪費 | Phase 5 |
| 即時告警彈窗 | **WebSocket push** | 即時 | 管理員需立即看到緊急事件 | Phase 5 |
| 工單派工/結案通知 | **Polling**（目前） → **WebSocket**（未來） | — | 目前 Polling 夠用，Phase 5 可升級 | Phase 1~4: Polling |

### 二、資料流架構（三層分離）

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: 設備 → 後端（IoT 協議層）                            │
│                                                             │
│   路燈設備 ──MQTT/CoAP/HTTP──▶ IoT Gateway ──▶ Backend 寫 DB │
│                                                             │
│   ※ 這層跟前端 WebSocket 無關，是 IoT 通訊協議               │
├─────────────────────────────────────────────────────────────┤
│ Layer 2: 後端規則引擎（判斷層）                                │
│                                                             │
│   Backend 收到 telemetry → 寫 DB                             │
│         ├─ 正常 → 等前端 Polling 來拿                         │
│         └─ 異常（符合預定義規則）→ 觸發告警事件                 │
│                                                             │
│   預定義規則範例：                                            │
│     • 心跳超時 > 5 分鐘 → 設備離線                            │
│     • 電流值超過安全閾值 → 設備異常                            │
│     • 電壓驟降 → 可能故障                                     │
│                                                             │
│   ※ 判斷邏輯在後端，前端不做閾值判斷                          │
├─────────────────────────────────────────────────────────────┤
│ Layer 3: 後端 → 前端（推送層）                                │
│                                                             │
│   一般數據 ──Polling GET──▶ 前端 timer 更新                   │
│   異常告警 ──WebSocket push──▶ 前端地圖圖標變色 / 彈窗         │
│                                                             │
│   ※ WebSocket 只推差異，不推原始 telemetry                    │
└─────────────────────────────────────────────────────────────┘
```

### 三、WebSocket 的角色定義

> **WebSocket 在本專案中 = 後端主動通知前端「有事發生了」**  
> 不是用來傳輸大量 telemetry 原始數據。

推送內容是精簡的差異訊息，例如：
```json
{ "deviceId": "SL-1234", "status": "OFFLINE", "timestamp": "..." }
```
前端收到後只更新對應圖標，不需重新拉取全量。

---

## 現有基礎設施盤點

| 項目 | 狀態 | 說明 |
|------|------|------|
| STOMP + SockJS 配置 | ✅ 已建 | `WebSocketConfig.java` + `/ws` endpoint |
| JWT 認證攔截 | ✅ 已建 | `StompAuthInterceptor` CONNECT 階段驗 JWT |
| Per-user 頻道 | ✅ 已建 | `/user/topic/notifications` |
| `NotificationService.send()` | ✅ 已建 | 多管道 dispatch（InApp/Email/SMS） |
| `SimpMessagingTemplate` 推送 | ⬜ 未接 | InAppChannel 未呼叫 `convertAndSendToUser()` |
| 前端 STOMP 客戶端 | ⬜ 未建 | 目前只有 Polling |
| `NotificationService.send()` 生產呼叫 | ⬜ 零呼叫 | 5 個 Listener 只做 DB 更新，不發通知 |

---

## 通知事件 Use Case 清單

### 一、工單流程推送（最高優先）

| 事件 | 觸發時機 | 推送對象 | 通知類型 | refType |
|------|---------|---------|---------|---------|
| 障礙審核通過 | `FAULT_REVIEW → CONFIRMED` | 維修管理員 | `TODO` | `FAULT` |
| 報修派工 | `REPAIR_DISPATCH → DISPATCHED` | 被指派維修人員 | `TODO` | `REPAIR` |
| 外勤完工回報 | 維修人員填完工 | 派工者（dispatched_by） | `TODO` | `REPAIR` |
| 報修結案 | `REPAIR_CLOSE → CLOSED` | 原報案人 | `INFO` | `REPAIR` |
| 換修派工 | `REPLACEMENT_REVIEW → DISPATCHED` | 被指派維修人員 | `TODO` | `REPLACEMENT` |
| 換修結案 | `REPLACEMENT_REVIEW → CLOSED` | 原報案人 | `INFO` | `REPLACEMENT` |

### 二、庫存告警推送

| 事件 | 觸發時機 | 推送對象 | 通知類型 | refType |
|------|---------|---------|---------|---------|
| 安全庫存低於門檻 | `@Scheduled` 每日 08:00（`LowStockAlertEvent` 已發佈，**但沒有 Listener**） | `MATERIAL_MANAGE` 權限用戶 | `ALERT` | `MATERIAL` |

### 三、簽核待辦推送

| 事件 | 觸發時機 | 推送對象 | 通知類型 | refType |
|------|---------|---------|---------|---------|
| 簽核到你 | 工單流轉到某步驟 | 該步驟的審核人 | `TODO` | `WORKFLOW` |
| 簽核被退回 | 審核人退件 | 原提出人 | `ALERT` | `WORKFLOW` |

### 四、公告推送

| 事件 | 觸發時機 | 推送對象 | 通知類型 | refType |
|------|---------|---------|---------|---------|
| 新公告發佈 | 管理員發佈/排程公告上線 | 全租戶用戶 | `INFO` | `ANNOUNCEMENT` |

### 五、設備即時告警（Phase 5+）

| 事件 | 觸發時機 | 推送對象 | 通知類型 | refType |
|------|---------|---------|---------|---------|
| Gateway 離線 | 智慧路燈 heartbeat timeout | 該區域管理員 | `ALERT` | `FAULT` |
| SIM 到期預警 | 排程掃描 SIM 到期日 | 設備管理員 | `ALERT` | `FAULT` |
| 回路故障 | IoT 平台事件 | 維修管理員 | `ALERT` | `FAULT` |

---

## 實作路徑

### Phase 1~4：Polling（現行）

既有 Listener 需補上 `notificationService.send()` 呼叫：

```
既有 Listener                    → 加 notificationService.send()
────────────────────────────────────────────────────────────
FaultApprovedListener           → 通知維修管理員
RepairDispatchedListener        → 通知被指派人
RepairClosedListener            → 通知原報案人
ReplacementNeedMaterialListener → 通知被指派人
ReplacementClosedListener       → 通知原報案人

缺少的 Listener                  → 新建
────────────────────────────────────────────────────────────
LowStockAlertListener           → 監聽 LowStockAlertEvent → send()
WorkflowPendingListener         → 監聽簽核到站 → send()
```

### Phase 5：啟用 WebSocket

1. `InAppChannel` 加 `SimpMessagingTemplate.convertAndSendToUser()` → 寫 DB 同時推 WS
2. 前端加 STOMP 客戶端（stompjs），訂閱 `/user/topic/notifications`
3. 地圖頁訂閱 `/topic/device-status`，接收設備狀態差異推送

