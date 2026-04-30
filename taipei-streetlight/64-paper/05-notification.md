# 05 — 通知模組（Notification Module）

## 1. 模組概述

通知模組負責台北市路燈管理系統的多通道訊息派送，採用策略模式（Strategy Pattern）支援以下通道：

- **IN_APP**：站內通知，寫入 `notifications` 資料表並透過 WebSocket（STOMP）即時推送
- **EMAIL**：電子郵件通知，依使用者 `notifyEmailFlag` 設定決定是否發送
- **SMS**：簡訊通知（目前為 NoOp 實作，未接入 SMS Gateway）

模組透過監聽 `WorkflowTransitionEvent`（Spring ApplicationEvent）自動觸發通知，實現工作流程與通知的解耦。

---

## 2. 資料表結構

### 2.1 notifications

| 欄位 | 型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶 ID |
| user_id | VARCHAR(50) | NOT NULL | 收件人 ID |
| type | VARCHAR(20) | NOT NULL, ENUM | 通知類型（TODO/ALERT/INFO） |
| title | VARCHAR(200) | NOT NULL | 通知標題 |
| content | VARCHAR(2000) | | 通知內容 |
| ref_type | VARCHAR(50) | ENUM | 關聯業務類型 |
| ref_id | VARCHAR(50) | | 關聯業務 ID |
| read | BOOLEAN | NOT NULL, 預設 false | 是否已讀 |
| read_at | TIMESTAMP | | 已讀時間 |
| created_at | TIMESTAMP | NOT NULL, 不可更新, @CreatedDate | 建立時間 |
| updated_at | TIMESTAMP | @LastModifiedDate | 更新時間 |

---

## 3. 實體關聯

```
NotificationEntity (N) ──── user_id ────> (1) UserEntity（收件人）
NotificationEntity (N) ──── ref_type + ref_id ────> (1) 各業務實體
  │
  ├── ref_type=FAULT    → FaultTicket
  ├── ref_type=REPAIR   → RepairTicket
  ├── ref_type=REPLACEMENT → ReplacementOrder
  ├── ref_type=WORKFLOW  → WorkflowInstance
  ├── ref_type=ANNOUNCEMENT → Announcement
  └── ref_type=MATERIAL  → MaterialIssue
```

---

## 4. API 端點摘要

### REST API `/v1/auth/notifications`

| 方法 | 路徑 | 說明 |
|---|---|---|
| GET | `/?page=&size=` | 查詢通知列表（分頁，按建立時間倒序） |
| GET | `/todos?page=&size=` | 查詢待辦通知（僅 type=TODO） |
| GET | `/unread-count` | 取得未讀通知數量 |
| PATCH | `/{id}/read` | 標記單則通知為已讀 |
| PATCH | `/read-all` | 標記全部通知為已讀 |

### WebSocket

| 端點 | 協定 | 說明 |
|---|---|---|
| `/ws` | STOMP over SockJS | WebSocket 連線端點 |
| `/user/{userId}/queue/notifications` | STOMP 訂閱 | 個人通知即時推送通道 |

**STOMP 認證**：連線時透過 `Authorization: Bearer {token}` Header 進行 JWT 驗證（StompAuthInterceptor）。

**Broker 配置**：
- 簡易 Broker 前綴：`/topic`、`/queue`
- 應用程式目標前綴：`/app`
- 使用者目標前綴：`/user`

---

## 5. 業務邏輯

### 5.1 多通道派送

`NotificationService.send()` 遍歷所有已註冊的 `NotificationChannel` 實作，逐一呼叫 `send()`。任一通道失敗不影響其他通道（catch + log.warn）。

### 5.2 通道實作

| 通道 | 類別 | 行為 |
|---|---|---|
| IN_APP | InAppChannel | 1) 寫入 `notifications` 表 2) 透過 `SimpMessagingTemplate` 推送 WebSocket |
| EMAIL | EmailChannel | 依 `user.notifyEmailFlag` 與 `user.email` 判斷是否寄信（Profile: `!test`） |
| EMAIL | NoOpEmailChannel | 測試環境空實作（Profile: `test`） |
| SMS | NoOpSmsChannel | 空實作，預留 SMS Gateway 擴充點 |

### 5.3 事件驅動通知觸發

以下 Listener 監聽 `WorkflowTransitionEvent` 並依流程類型/步驟條件觸發通知：

| Listener | 觸發條件 | 通知對象 | 通知類型 |
|---|---|---|---|
| RepairDispatchNotificationListener | REPAIR_DISPATCH + targetStep=DISPATCHED | 被派工人員 | TODO |
| CompletionReportedNotificationListener | REPAIR_CLOSE + targetStep=PENDING_REVIEW | 流程發起人 | TODO |
| RepairClosedNotificationListener | REPAIR_CLOSE + targetStep=CLOSED | 原建單人 | INFO |
| ReplacementDispatchNotificationListener | REPLACEMENT_REVIEW + targetStep=DISPATCHED | 被派工人員 | TODO |
| IssueRequestedNotificationListener | REPLACEMENT_REVIEW + targetStep=DISPATCHED | ADMIN + OPERATOR 角色 | TODO |

### 5.4 已讀管理

- `markRead()`：檢查通知歸屬（userId 必須匹配），設定 `read=true` 與 `readAt`
- `markAllRead()`：批次 UPDATE，一次將使用者所有未讀通知標記為已讀

---

## 6. 資料流程

```
工作流程狀態轉換
  │
  ▼
WorkflowTransitionEvent 發布
  │
  ▼
各 NotificationListener（@EventListener）
  ├─ 判斷 workflowType + targetStep 是否匹配
  ├─ 組裝 NotificationPayload（tenantId, userIds, type, title, content, refType, refId）
  └─ 呼叫 NotificationService.send(payload)
       │
       ▼
     遍歷所有 NotificationChannel
       │
       ├─ InAppChannel
       │    ├─ 寫入 notifications 表
       │    └─ SimpMessagingTemplate.convertAndSendToUser()
       │         │
       │         ▼
       │       WebSocket STOMP → 前端即時收到通知
       │
       ├─ EmailChannel
       │    ├─ 查詢 UserEntity
       │    ├─ 檢查 notifyEmailFlag && email != null
       │    └─ JavaMailSender.send()
       │
       └─ NoOpSmsChannel（略過）

前端查詢
  │
  ▼
GET /v1/auth/notifications
  └─ findByUserIdOrderByCreatedAtDesc() → 分頁回傳

前端標記已讀
  │
  ▼
PATCH /v1/auth/notifications/{id}/read
  └─ 驗證 userId → 設定 read=true, readAt=now
```

---

## 7. 列舉值定義

### NotificationType

| 值 | 說明 |
|---|---|
| TODO | 待辦事項（需使用者處理） |
| ALERT | 警示通知 |
| INFO | 資訊通知（僅通知，無需操作） |

### NotificationRefType

| 值 | 說明 |
|---|---|
| FAULT | 通報單 |
| REPAIR | 報修單 |
| REPLACEMENT | 換裝派工單 |
| WORKFLOW | 工作流程 |
| ANNOUNCEMENT | 公告 |
| MATERIAL | 材料/領料 |
