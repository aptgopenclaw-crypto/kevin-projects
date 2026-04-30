# SA-05 換裝維護 Function List

> **對應需求**：§6-(1) ~ §6-(12)  
> **SRS 對應**：SRS-06-001 ~ SRS-06-012  
> **Spec 來源**：`/02-spec/06-replacement-maintenance.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-05-01 | 號碼牌管理 | GOV_ADMIN | 編號產生 + QR Code |
| UC-05-02 | 資產清冊匯入 | GOV_ADMIN | 普查批次匯入 |
| UC-05-03 | 換裝工單管理 | GOV_ADMIN, OPERATOR | 建單+派工+狀態管理 |
| UC-05-04 | 自主檢核 | FIELD_USER, CONTRACTOR | 現場自檢+資產預更新 |
| UC-05-05 | 換裝審核 | GOV_MGR, GOV_CHIEF | 報竣審核+結案 |
| UC-05-06 | 材料使用管控 | 系統 | 驗證合格材料 |
| UC-05-07 | 統計匯出 | GOV_ADMIN | 統計+清冊匯出 |

---

## Function List

### 號碼牌管理 (§6-1)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-001 | 號碼牌列表查詢 | R | GOV_ADMIN | 關鍵字、分頁 | 號碼牌清單 | — | SRS-06-001 | §6-1 | GET /v1/auth/pole-numbers |
| FN-05-002 | 產生號碼牌編號 | C | GOV_ADMIN | 區域、數量 | 新編號清單 | 依機關規則不重複；含 QR Code URL | SRS-06-001 | §6-1 | POST /v1/auth/pole-numbers/generate |
| FN-05-003 | 重製 QR Code | U | GOV_ADMIN | 號碼牌 ID | QR Code 圖片 | 連結至報修網頁 (/public/repair?pole={code}) | SRS-06-001 | §6-1 | GET /v1/auth/pole-numbers/{id}/qr |
| FN-05-004 | 刪除號碼牌 | D | GOV_ADMIN | 號碼牌 ID | 刪除結果 | 已綁定設備不可刪 | SRS-06-001 | §6-1 | DELETE /v1/auth/pole-numbers/{id} |

### 資產清冊匯入 (§6-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-005 | 批次匯入資產清冊 | I | GOV_ADMIN | Excel/CSV 檔案 | 匯入結果(成功/失敗/差異) | 欄位：分電箱編號、地址、台電桿號、換裝前後燈種瓦數；比對現有→差異需確認 | SRS-06-002 | §6-2 | POST /v1/auth/replacement/import |
| FN-05-006 | 匯入差異確認 | U | GOV_ADMIN | 差異清單 + 確認/放棄 | 更新結果 | 人工確認後寫入 devices | SRS-06-002 | §6-2 | PUT /v1/auth/replacement/import/confirm |

### 換裝工單管理 (§6-3, §6-6, §6-7)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-007 | 換裝工單列表 | R | GOV_ADMIN, OPERATOR | 狀態、類型、廠商、分頁 | 工單清單 | DataScope 限制 | SRS-06-006 | §6-6 | GET /v1/auth/replacement |
| FN-05-008 | 新增換裝工單 | C | GOV_ADMIN | 類型、事由、地點、數量、工期、契約ID、(repair_ticket_id) | 工單資料 | 雙路徑：報修轉換裝 / 獨立建單；啟動 REPLACEMENT_REVIEW 流程 | SRS-06-006 | §6-6 | POST /v1/auth/replacement |
| FN-05-009 | 查看工單詳情 | R | GOV_ADMIN | 工單 ID | 完整資料+項目+附件+流程 | — | SRS-06-006 | §6-6 | GET /v1/auth/replacement/{id} |
| FN-05-010 | 派工 | U | GOV_ADMIN | 工單 ID、廠商 | 狀態→DISPATCHED | DRAFT→DISPATCHED；指派廠商 | SRS-06-006 | §6-6 | PUT /v1/auth/replacement/{id}/dispatch |
| FN-05-011 | 開工 | U | FIELD_USER | 工單 ID | 狀態→IN_PROGRESS | DISPATCHED→IN_PROGRESS | SRS-06-006 | §6-6 | PUT /v1/auth/replacement/{id}/start |
| FN-05-012 | 案件類別管理 | C/U/D | GOV_ADMIN | 類別名稱 | 類別資料 | 新設/換裝/遷移/停用/調整/加裝遮光罩；可彈性調整 | SRS-06-007 | §6-7 | CRUD /v1/auth/replacement/types |

### 新設/遷移預匯入 (§6-8)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-013 | 預匯入路燈編號+位置 | I | GOV_ADMIN | 工單 ID、Excel(編號+坐標) | 匯入結果 | 新設/遷移案件；地圖可查看預匯入位置 | SRS-06-008 | §6-8 | POST /v1/auth/replacement/{id}/pre-import |
| FN-05-014 | 預匯入地圖查看 | R | GOV_ADMIN | 工單 ID | 預匯入點位 GeoJSON | 便於施工辨別位置 | SRS-06-008 | §6-8 | GET /v1/auth/replacement/{id}/pre-import/map |

### 換裝項目管理 (§6-4, §6-5)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-015 | 新增換裝項目 | C | GOV_ADMIN, FIELD_USER | 工單 ID、材料規格 ID、設備 ID、數量 | 項目資料 | 材料必須為合格材料 (approved_materials.status=ACTIVE)；否則拒絕 | SRS-06-005 | §6-5 | POST /v1/auth/replacement/{id}/items |
| FN-05-016 | 換裝項目列表 | R | GOV_ADMIN | 工單 ID | 項目清單 | — | SRS-06-005 | §6-4 | GET /v1/auth/replacement/{id}/items |
| FN-05-017 | 編輯換裝項目 | U | GOV_ADMIN | 項目 ID、修改欄位 | 更新結果 | CLOSED 後不可改 | SRS-06-005 | §6-5 | PUT /v1/auth/replacement/items/{itemId} |
| FN-05-018 | 刪除換裝項目 | D | GOV_ADMIN | 項目 ID | 刪除結果 | — | SRS-06-005 | §6-5 | DELETE /v1/auth/replacement/items/{itemId} |
| FN-05-019 | 合格材料查驗 | P | 系統 | 材料規格 ID | 合格/不合格 | 查詢 approved_materials.status=ACTIVE | SRS-06-005 | §6-5 | (內部呼叫) |
| FN-05-020 | 材料清單批次匯入 | I | GOV_ADMIN | Excel(燈具/控制器審驗清單) | 匯入結果 | 欄位：契約、編號、合格日期、批號、廠牌型號 | SRS-06-004 | §6-4 | POST /v1/auth/approved-materials/import |

### 自主檢核 (§6-9)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-021 | 提交自主檢核 | U | FIELD_USER, CONTRACTOR | 工單 ID、檢核結果、附件 | 狀態→SELF_CHECKED | IN_PROGRESS→SELF_CHECKED；預先更新系統資產（device 屬性+新設備 ID writeback） | SRS-06-009 | §6-9 | PUT /v1/auth/replacement/{id}/self-check |
| FN-05-022 | 自檢後地圖/清冊查看 | R | GOV_ADMIN, OPERATOR | 工單 ID | 更新後資產 | 自檢完成後可在地圖/清冊查看最新內容 | SRS-06-009 | §6-9 | (組合既有查詢) |
| FN-05-023 | 組件替換（自檢） | U | FIELD_USER | 項目 ID、舊設備 ID、新設備 ID、新 deviceCode | 替換結果 | 舊設備→DECOMMISSIONED(標記)；新設備→寫入 attributes；觸發 E6(領料) | SRS-06-009 | §6-9 | (含於 FN-05-021) |

### 換裝審核 (§6-10)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-024 | 報竣送審 | U | GOV_ADMIN | 工單 ID | 狀態→PENDING_REVIEW | SELF_CHECKED→PENDING_REVIEW | SRS-06-010 | §6-10 | PUT /v1/auth/replacement/{id}/submit-review |
| FN-05-025 | 審核通過(結案) | P | GOV_MGR, GOV_CHIEF | 流程實例 ID | 狀態→CLOSED | 觸發 E10：舊設備 DECOMMISSIONED + 新設備 ACTIVE + device_event | SRS-06-010 | §6-10 | (via workflow) |
| FN-05-026 | 退回補件 | P | GOV_MGR | 流程實例 ID、原因 | 狀態→RETURNED | PENDING_REVIEW→RETURNED | SRS-06-010 | §6-10 | (via workflow) |
| FN-05-027 | 補件重送 | U | GOV_ADMIN | 工單 ID | 狀態→PENDING_REVIEW | RETURNED→PENDING_REVIEW；可更正後重送 | SRS-06-010 | §6-10 | PUT /v1/auth/replacement/{id}/resubmit |
| FN-05-028 | 結案後補救機制 | U | SUPER_ADMIN | 工單 ID、補救原因 | 狀態→RETURNED | 已結案但查核發現錯誤→可退回重開 | SRS-06-010 | §6-10 | PUT /v1/auth/replacement/{id}/reopen |

### 資產異動管理 (§6-3)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-029 | 異動紀錄查詢 | R | GOV_ADMIN | 設備 ID / 工單 ID | 異動歷程 | 資產加帳/除帳/變更紀錄 | SRS-06-003 | §6-3 | GET /v1/auth/replacement/{id}/changes |

### 統計匯出 (§6-11, §6-12)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-05-030 | 派工案件類型統計 | R | GOV_ADMIN | 契約ID、時間範圍 | 各類型數量+進度 | — | SRS-06-011 | §6-11 | GET /v1/auth/replacement/statistics |
| FN-05-031 | 交付/完成件數統計 | R | GOV_ADMIN | 契約ID | 已交付vs完成件數 | — | SRS-06-011 | §6-11 | GET /v1/auth/replacement/statistics/progress |
| FN-05-032 | 匯出派工清冊 | E | GOV_ADMIN | 篩選條件 | ODS/XLS/CSV | — | SRS-06-012 | §6-12 | GET /v1/auth/replacement/export |
| FN-05-033 | 匯出異動路燈地圖 | E | GOV_ADMIN | 工單 ID | GeoJSON / 圖檔 | — | SRS-06-012 | §6-12 | GET /v1/auth/replacement/{id}/export/map |
| FN-05-034 | 產生竣工清單 | E | GOV_ADMIN | 工單 ID | PDF/Excel | 竣工查核/環境清潔/圖資校核 | SRS-06-012 | §6-12 | GET /v1/auth/replacement/{id}/report/completion |
| FN-05-035 | 產生用電申請表 | E | GOV_ADMIN | 工單 ID | PDF/Excel | 受理用電變更 | SRS-06-012 | §6-12 | GET /v1/auth/replacement/{id}/report/power-application |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 換裝工單列表 | /admin/replacement/orders | 列表+篩選+新增 | FN-05-007~008 |
| 工單詳情 | /admin/replacement/orders/:id | Tabs+Action | FN-05-009~027 |
| 自主檢核 | /admin/replacement/orders/:id/self-check | 自檢表單 | FN-05-021~023 |
| 號碼牌管理 | /admin/replacement/pole-numbers | CRUD+QR | FN-05-001~004 |
