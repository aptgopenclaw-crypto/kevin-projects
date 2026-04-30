# SA-09 行動 APP Function List

> **對應需求**：§10-(1) ~ §10-(5)  
> **SRS 對應**：SRS-10-001 ~ SRS-10-005  
> **Spec 來源**：`/02-spec/10-mobile-app.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-09-01 | 行動登入 | FIELD_USER, CONTRACTOR | APP 登入+Token 快取 |
| UC-09-02 | 資產清查 | FIELD_USER | 現場掃碼盤點 |
| UC-09-03 | 巡查養護紀錄 | FIELD_USER | 現場巡查+拍照+GPS |
| UC-09-04 | 維修/換裝拍照 | FIELD_USER | 照片管控(GPS+時間戳+浮水印) |
| UC-09-05 | 離線作業 | FIELD_USER | 離線儲存+回線同步 |

---

## Function List

### 行動登入 (§10 共通)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-09-001 | APP 登入 | P | FIELD_USER | 帳號/密碼 | JWT Token | 共用 AuthController；Token 本地快取 | SRS-10-001 | §10 | POST /v1/auth/login |
| FN-09-002 | Token 自動更新 | P | 系統(APP) | refreshToken | 新 Token | 過期前自動刷新 | SRS-10-001 | §10 | POST /v1/auth/refresh |
| FN-09-003 | APP 推播接收 | N | FIELD_USER | FCM/APNs Token 註冊 | — | 前端註冊推播 Token → 後端儲存 | SRS-10-001 | §10 | POST /v1/auth/push-tokens |

### 資產清查 (§10-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-09-004 | QR Code 掃碼查詢 | R | FIELD_USER | QR Code / 桿號 | 設備資訊 | 掃碼→API 查詢→顯示 | SRS-10-001 | §10-1 | GET /v1/auth/devices/by-pole?poleNumber={qr} |
| FN-09-005 | 資產清查任務列表 | R | FIELD_USER | — | 待盤清單 | 依分配任務 | SRS-10-001 | §10-1 | GET /v1/auth/mobile/census/tasks |
| FN-09-006 | 資產盤點回報 | C | FIELD_USER | 設備 ID、狀態、位置(GPS)、照片 | 盤點結果 | GPS 座標+時間戳自動記錄 | SRS-10-001 | §10-1 | POST /v1/auth/mobile/census/report |
| FN-09-007 | 資產異常回報 | C | FIELD_USER | 設備 ID、異常描述、照片 | 異常紀錄 | 可觸發 E5 建立故障通報 | SRS-10-001 | §10-1 | POST /v1/auth/mobile/census/anomaly |

### 巡查養護 (§10-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-09-008 | 巡查任務列表 | R | FIELD_USER | — | 今日巡查路線+任務 | — | SRS-10-002 | §10-2 | GET /v1/auth/mobile/inspection/tasks |
| FN-09-009 | 巡查打卡 | C | FIELD_USER | 任務 ID、GPS 座標 | 打卡結果 | GPS 距離 ≤ 50m 才可打卡 | SRS-10-002 | §10-2 | POST /v1/auth/mobile/inspection/checkin |
| FN-09-010 | 巡查紀錄填報 | C | FIELD_USER | 任務 ID、檢查項目(checklist)、照片、備註 | 巡查紀錄 | — | SRS-10-002 | §10-2 | POST /v1/auth/mobile/inspection/records |
| FN-09-011 | 巡查異常通報 | C | FIELD_USER | 位置、描述、照片 | 異常紀錄 | 觸發 E13(巡查→維修派工) | SRS-10-002 | §10-2 | POST /v1/auth/mobile/inspection/anomaly |

### 維修/換裝拍照 (§10-3, §10-4)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-09-012 | 工單任務列表 | R | FIELD_USER | — | 待施工工單 | 含維修單+換裝單 | SRS-10-003 | §10-3 | GET /v1/auth/mobile/work-orders |
| FN-09-013 | 施工前拍照 | C | FIELD_USER | 工單 ID、照片 | 附件紀錄 | GPS 定位自動嵌入 EXIF；50m 範圍驗證 | SRS-10-004 | §10-4 | POST /v1/auth/mobile/photos/before |
| FN-09-014 | 施工後拍照 | C | FIELD_USER | 工單 ID、照片 | 附件紀錄 | 同上；照片浮水印(日期+GPS+施工人員) | SRS-10-004 | §10-4 | POST /v1/auth/mobile/photos/after |
| FN-09-015 | 照片管控驗證 | P | 系統(APP) | 照片 EXIF | 驗證結果 | GPS 距離 ≤ 50m + 時間合理 + EXIF 完整 | SRS-10-004 | §10-4 | (APP 端) |
| FN-09-016 | 施工完工回報 | U | FIELD_USER | 工單 ID、完工備註 | 狀態更新 | 觸發 E2(維修)/E6(換裝) 狀態推進 | SRS-10-003 | §10-3 | PUT /v1/auth/mobile/work-orders/{id}/complete |

### 離線同步 (§10-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-09-017 | 離線數據暫存 | P | 系統(APP) | 操作紀錄+照片 | 本地 SQLite | 無網路時儲存至 SQLite | SRS-10-005 | §10-5 | (APP 端) |
| FN-09-018 | 網路恢復偵測 | P | 系統(APP) | 網路狀態 | 觸發同步 | — | SRS-10-005 | §10-5 | (APP 端) |
| FN-09-019 | 離線數據同步 | I | 系統(APP) | 離線暫存數據 | 同步結果 | 增量上傳；衝突偵測(server wins) | SRS-10-005 | §10-5 | POST /v1/auth/mobile/sync |
| FN-09-020 | 大檔分塊上傳 | I | 系統(APP) | 照片(chunk) | 上傳進度 | 分塊上傳(chunk ≤ 2MB)；斷點續傳 | SRS-10-005 | §10-5 | POST /v1/auth/mobile/upload/chunk |
| FN-09-021 | 同步狀態查詢 | R | FIELD_USER | — | 待同步筆數+上傳進度 | — | SRS-10-005 | §10-5 | (APP 端) |

---

## 前端頁面清單 (APP)

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 登入 | /login | APP 登入 | FN-09-001~002 |
| 首頁 | /home | 任務總覽+待辦 | FN-09-005, 008, 012 |
| 掃碼清查 | /census | QR 掃碼+盤點 | FN-09-004~007 |
| 巡查 | /inspection | 路線+打卡+紀錄 | FN-09-008~011 |
| 工單 | /work-orders | 維修/換裝任務 | FN-09-012, 016 |
| 拍照 | /camera | 施工前後拍照 | FN-09-013~015 |
| 同步狀態 | /sync | 離線→在線同步 | FN-09-017~021 |
