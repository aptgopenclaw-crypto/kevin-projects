# TS-09 行動 APP — Test Specification (Forward Design)

> **對應 SA**：SA-09-mobile (FN-09-001 ~ FN-09-021)  
> **對應 SD**：SD-09-mobile  
> **Test Classes**：0 (尚未實作)  
> **Phase**：Phase 7 — 行動 APP

---

## 使用方式

本文件為 **前瞻設計 TC**，用於 Phase 7 實作時的驗收標準。  
所有 TC 均為 ⬜ 狀態，待實作時轉為 ✅。

---

## 1. 認證與推播 (FN-09-001 ~ FN-09-003)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-09-001-01 | FN-09-001 APP 登入 | 行動裝置登入 | POST /auth/login | 200, JWT | accessToken |
| TC-09-001-02 | FN-09-001 | 帳密錯誤 | POST /auth/login | 401 | errorCode |
| TC-09-002-01 | FN-09-002 Token 更新 | 自動更新 Token | POST /auth/refresh | 200, new token | seamless |
| TC-09-003-01 | FN-09-003 推播接收 | 註冊推播 token | POST /push-tokens | 200 | deviceToken stored |

---

## 2. 掃碼查詢 (FN-09-004)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-09-004-01 | FN-09-004 QR 掃碼 | 掃碼查設備 | GET /devices/by-pole?poleNumber=PN-001 | 200, device | 含座標+歷程 |
| TC-09-004-02 | FN-09-004 | 號碼不存在 | GET /devices/by-pole?poleNumber=XXX | 404 | not found |

---

## 3. 資產清查 (FN-09-005 ~ FN-09-007)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-09-005-01 | FN-09-005 清查任務 | 我的清查任務 | GET /mobile/census/tasks | 200, list | assigned to me |
| TC-09-006-01 | FN-09-006 盤點回報 | 上報盤點結果 | POST /mobile/census/report | 200 | 含 GPS + photo |
| TC-09-007-01 | FN-09-007 異常回報 | 上報資產異常 | POST /mobile/census/anomaly | 200 | 產生 faultTicket |

---

## 4. 巡查 (FN-09-008 ~ FN-09-011)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-09-008-01 | FN-09-008 巡查任務 | 我的巡查任務 | GET /mobile/inspection/tasks | 200, list | 含路線 |
| TC-09-009-01 | FN-09-009 巡查打卡 | GPS 打卡 | POST /mobile/inspection/checkin | 200, checkedIn | GPS 座標 |
| TC-09-010-01 | FN-09-010 紀錄填報 | 巡查紀錄 | POST /mobile/inspection/records | 200 | 含照片 |
| TC-09-011-01 | FN-09-011 異常通報 | 巡查發現異常 | POST /mobile/inspection/anomaly | 200 | → faultTicket |

---

## 5. 施工管理 (FN-09-012 ~ FN-09-016)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-09-012-01 | FN-09-012 工單列表 | 我的工單 | GET /mobile/work-orders | 200, list | status filter |
| TC-09-013-01 | FN-09-013 施工前拍照 | 上傳施工前照片 | POST /mobile/photos/before | 200 | GPS + timestamp |
| TC-09-014-01 | FN-09-014 施工後拍照 | 上傳施工後照片 | POST /mobile/photos/after | 200 | GPS + timestamp |
| TC-09-015-01 | FN-09-015 照片管控 | GPS/時間驗證 | (APP) | 拒絕篡改照片 | EXIF 驗證 |
| TC-09-016-01 | FN-09-016 完工回報 | 施工完工 | PUT /mobile/work-orders/{id}/complete | 200 | status=COMPLETED |

---

## 6. 離線同步 (FN-09-017 ~ FN-09-021)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-09-017-01 | FN-09-017 離線暫存 | 無網路時暫存 | (APP) | local DB stored | sqlite/realm |
| TC-09-018-01 | FN-09-018 網路偵測 | 網路恢復偵測 | (APP) | 自動觸發同步 | event trigger |
| TC-09-019-01 | FN-09-019 數據同步 | 批次同步上傳 | POST /mobile/sync | 200, synced count | 衝突解決 |
| TC-09-019-02 | FN-09-019 | 衝突處理 | POST /mobile/sync | 200, conflicts | conflict list |
| TC-09-020-01 | FN-09-020 分塊上傳 | 大檔分塊 | POST /mobile/upload/chunk | 200, chunkId | resumable |
| TC-09-021-01 | FN-09-021 同步狀態 | 查詢同步進度 | (APP) | pending count | UI 顯示 |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ⬜ 全部待實作 | 23 |
| **總 TC 數** | **23** |
