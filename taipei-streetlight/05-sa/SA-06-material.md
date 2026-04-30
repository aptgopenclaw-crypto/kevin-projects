# SA-06 材料管理 Function List

> **對應需求**：§7-(1) ~ §7-(2)  
> **SRS 對應**：SRS-07-001 ~ SRS-07-007  
> **Spec 來源**：`/02-spec/07-material-management.md`

---

## Use Case 清單

| UC ID | Use Case 名稱 | Actor | 說明 |
|-------|--------------|-------|------|
| UC-06-01 | 基礎資料管理 | GOV_ADMIN | 庫別/材料規格/供應商 CRUD |
| UC-06-02 | 採購管理 | GOV_ADMIN | 採購單+驗收 |
| UC-06-03 | 收料管理 | GOV_ADMIN | 進貨+轉庫 |
| UC-06-04 | 庫存管理 | GOV_ADMIN | 查詢+盤點+預警 |
| UC-06-05 | 出料管理 | GOV_ADMIN, FIELD_USER | 領料申請+出料 |
| UC-06-06 | 報核管理 | GOV_ADMIN | 費用報表+廢品處理 |
| UC-06-07 | 廠商查詢 | CONTRACTOR | 材料/用量/分析 |

---

## Function List

### 基礎資料 — 倉庫 (§7-1A)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-001 | 倉庫列表查詢 | R | GOV_ADMIN | 關鍵字、分頁 | 倉庫清單 | — | SRS-07-001 | §7-1A | GET /v1/auth/material/warehouses |
| FN-06-002 | 新增倉庫 | C | GOV_ADMIN | 名稱、類型、地址、管理人 | 倉庫資料 | 名稱不重複 | SRS-07-001 | §7-1A | POST /v1/auth/material/warehouses |
| FN-06-003 | 編輯倉庫 | U | GOV_ADMIN | 倉庫 ID、修改欄位 | 更新結果 | — | SRS-07-001 | §7-1A | PUT /v1/auth/material/warehouses/{id} |
| FN-06-004 | 刪除倉庫 | D | GOV_ADMIN | 倉庫 ID | 刪除結果 | 有庫存時不可刪 | SRS-07-001 | §7-1A | DELETE /v1/auth/material/warehouses/{id} |

### 基礎資料 — 材料規格 (§7-1A)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-005 | 材料規格列表 | R | GOV_ADMIN, CONTRACTOR | 類別、關鍵字、分頁 | 規格清單 | — | SRS-07-001 | §7-1A | GET /v1/auth/material/specs |
| FN-06-006 | 新增材料規格 | C | GOV_ADMIN | 名稱、類別、規格(JSONB)、單位 | 規格資料 | 規格編號不重複 | SRS-07-001 | §7-1A | POST /v1/auth/material/specs |
| FN-06-007 | 編輯材料規格 | U | GOV_ADMIN | 規格 ID、修改欄位 | 更新結果 | — | SRS-07-001 | §7-1A | PUT /v1/auth/material/specs/{id} |
| FN-06-008 | 刪除材料規格 | D | GOV_ADMIN | 規格 ID | 刪除結果 | 有庫存/採購引用時不可刪 | SRS-07-001 | §7-1A | DELETE /v1/auth/material/specs/{id} |

### 基礎資料 — 供應商 (§7-1A)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-009 | 供應商列表 | R | GOV_ADMIN | 關鍵字、分頁 | 供應商清單 | — | SRS-07-001 | §7-1A | GET /v1/auth/material/suppliers |
| FN-06-010 | 新增供應商 | C | GOV_ADMIN | 名稱、統編、聯絡人、電話 | 供應商資料 | 統編不重複 | SRS-07-001 | §7-1A | POST /v1/auth/material/suppliers |
| FN-06-011 | 編輯供應商 | U | GOV_ADMIN | 供應商 ID | 更新結果 | — | SRS-07-001 | §7-1A | PUT /v1/auth/material/suppliers/{id} |
| FN-06-012 | 刪除供應商 | D | GOV_ADMIN | 供應商 ID | 刪除結果 | — | SRS-07-001 | §7-1A | DELETE /v1/auth/material/suppliers/{id} |

### 合格材料管理 (§6-4 匯入)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-013 | 合格材料列表 | R | GOV_ADMIN | 材料規格、狀態、分頁 | 合格材料清單 | — | SRS-07-001 | §6-4 | GET /v1/auth/material/approved |
| FN-06-014 | 新增合格材料 | C | GOV_ADMIN | 契約、編號、合格日期、批號、規格 | 合格材料 | 預設 ACTIVE | SRS-07-001 | §6-4 | POST /v1/auth/material/approved |
| FN-06-015 | 批次匯入合格材料 | I | GOV_ADMIN | Excel (燈具/控制器審驗清單) | 匯入結果 | 欄位驗證+重複檢查 | SRS-07-001 | §6-4 | POST /v1/auth/material/approved/import |
| FN-06-016 | 停用合格材料 | U | GOV_ADMIN | 材料 ID | 更新結果 | ACTIVE→INACTIVE | SRS-07-001 | §6-4 | PUT /v1/auth/material/approved/{id}/disable |

### 採購管理 (§7-1B)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-017 | 採購單列表 | R | GOV_ADMIN | 狀態、供應商、分頁 | 採購單清單 | — | SRS-07-002 | §7-1B | GET /v1/auth/material/purchase-orders |
| FN-06-018 | 新增採購單 | C | GOV_ADMIN | 供應商、項目清單(規格+數量+單價) | 採購單 | 預設 DRAFT；自動計算總金額 | SRS-07-002 | §7-1B | POST /v1/auth/material/purchase-orders |
| FN-06-019 | 編輯採購單 | U | GOV_ADMIN | 採購單 ID | 更新結果 | DRAFT 才可編輯 | SRS-07-002 | §7-1B | PUT /v1/auth/material/purchase-orders/{id} |
| FN-06-020 | 送審採購單 | U | GOV_ADMIN | 採購單 ID | 狀態→SUBMITTED | DRAFT→SUBMITTED | SRS-07-002 | §7-1B | PUT /v1/auth/material/purchase-orders/{id}/submit |
| FN-06-021 | 核准採購單 | U | GOV_MGR | 採購單 ID | 狀態→APPROVED | SUBMITTED→APPROVED | SRS-07-002 | §7-1B | PUT /v1/auth/material/purchase-orders/{id}/approve |
| FN-06-022 | 完成採購 | U | GOV_ADMIN | 採購單 ID | 狀態→COMPLETED | 全部收料完成 | SRS-07-002 | §7-1B | PUT /v1/auth/material/purchase-orders/{id}/complete |

### 收料入庫 (§7-1C)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-023 | 新增收料紀錄 | C | GOV_ADMIN | 採購單 ID、倉庫 ID、材料規格 ID、數量 | 收料紀錄 | 自動 UPSERT inventory（庫存+數量） | SRS-07-003 | §7-1C | POST /v1/auth/material/receiving |
| FN-06-024 | 收料紀錄列表 | R | GOV_ADMIN | 採購單 ID、倉庫、分頁 | 收料清單 | — | SRS-07-003 | §7-1C | GET /v1/auth/material/receiving |
| FN-06-025 | 轉庫作業 | U | GOV_ADMIN | 來源倉庫、目標倉庫、材料、數量 | 轉庫結果 | 來源扣減+目標增加 | SRS-07-003 | §7-1C | POST /v1/auth/material/inventory/transfer |

### 庫存管理 (§7-1D)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-026 | 庫存總覽查詢 | R | GOV_ADMIN, CONTRACTOR | 倉庫、類別、分頁 | 庫存清單 | 含現有量+安全量+差異 | SRS-07-004 | §7-1D | GET /v1/auth/material/inventory |
| FN-06-027 | 庫存盤點 | U | GOV_ADMIN | 倉庫 ID、材料 ID、實際數量、原因 | 調整紀錄 | 盤盈/盤虧自動計算差異 | SRS-07-004 | §7-1D | POST /v1/auth/material/inventory/count |
| FN-06-028 | 庫存調整 | U | GOV_ADMIN | 調整類型、數量、原因 | 調整紀錄 | — | SRS-07-004 | §7-1D | POST /v1/auth/material/inventory/adjust |
| FN-06-029 | 安全庫存量設定 | U | GOV_ADMIN | 材料 ID、安全量 | 更新結果 | — | SRS-07-004 | §7-1D | PUT /v1/auth/material/inventory/{id}/safety-stock |
| FN-06-030 | 安全庫存預警 (E12) | N | 系統 | 每日排程 | 預警通知 | inventory.quantity < safety_stock → 推送 LowStockAlert | SRS-07-004 | §7-1D | (排程) |
| FN-06-031 | 低庫存預警列表 | R | GOV_ADMIN | — | 低庫存材料清單 | — | SRS-07-004 | §7-1D | GET /v1/auth/material/inventory/low-stock |

### 出料管理 (§7-1E)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-032 | 領料申請列表 | R | GOV_ADMIN | 狀態、分頁 | 申請清單 | — | SRS-07-005 | §7-1E | GET /v1/auth/material/issues |
| FN-06-033 | 新增領料申請 | C | GOV_ADMIN, FIELD_USER | 材料、數量、用途、(工單 ID) | 申請資料 | 可手動或 E6 自動建立(換裝→領料) | SRS-07-005 | §7-1E | POST /v1/auth/material/issues |
| FN-06-034 | 核准領料 | U | GOV_ADMIN | 申請 ID | 狀態→APPROVED | — | SRS-07-005 | §7-1E | PUT /v1/auth/material/issues/{id}/approve |
| FN-06-035 | 確認出料(扣庫) | U | GOV_ADMIN | 申請 ID、倉庫 ID | 狀態→COMPLETED | inventory.quantity -= 出料量；量不足→拒絕 | SRS-07-005 | §7-1E | PUT /v1/auth/material/issues/{id}/confirm |
| FN-06-036 | 出料紀錄查詢 | R | GOV_ADMIN | 倉庫、材料、時間、分頁 | 出料紀錄 | — | SRS-07-005 | §7-1E | GET /v1/auth/material/issues/records |

### 報核/廢品 (§7-1F)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-037 | 費用支出報表 | R/E | GOV_ADMIN | 時間範圍、倉庫 | 費用報表 | 含採購金額+出料成本 | SRS-07-006 | §7-1F | GET /v1/auth/material/reports/cost |
| FN-06-038 | 廢品處理(繳庫) | C | GOV_ADMIN | 材料、數量、原因、處理方式 | 廢品紀錄 | 扣減庫存 | SRS-07-006 | §7-1F | POST /v1/auth/material/disposal |
| FN-06-039 | 廢品紀錄查詢 | R | GOV_ADMIN | 分頁 | 廢品清單 | — | SRS-07-006 | §7-1F | GET /v1/auth/material/disposal |

### 廠商端 (§7-2)

| FN ID | 功能名稱 | 類型 | Actor | 輸入 | 輸出 | 商業規則 | SRS | Spec | API |
|-------|---------|------|-------|------|------|---------|-----|------|-----|
| FN-06-040 | 廠商材料規格查詢 | R | CONTRACTOR | 契約 ID、類別 | 材料規格清單 | 僅顯示該廠商契約相關 | SRS-07-007 | §7-2 | GET /v1/auth/material/contractor/specs |
| FN-06-041 | 廠商月(年)用量查詢 | R | CONTRACTOR | 材料 ID、時間範圍 | 用量統計 | — | SRS-07-007 | §7-2 | GET /v1/auth/material/contractor/usage |
| FN-06-042 | 廠商安全庫存查詢 | R | CONTRACTOR | — | 庫存+安全量 | 僅顯示該廠商相關 | SRS-07-007 | §7-2 | GET /v1/auth/material/contractor/inventory |
| FN-06-043 | 材料壽命分析 | R | CONTRACTOR | 材料類別 | 壽命統計 | 平均使用壽命+故障率 | SRS-07-007 | §7-2 | GET /v1/auth/material/contractor/lifetime |
| FN-06-044 | 材料品質分析 | R | CONTRACTOR | 材料類別、時間 | 品質指標 | 故障率+更換率 | SRS-07-007 | §7-2 | GET /v1/auth/material/contractor/quality |
| FN-06-045 | 備存需求分析 | R | CONTRACTOR | — | 建議備存量 | 依用量趨勢+安全量計算 | SRS-07-007 | §7-2 | GET /v1/auth/material/contractor/stock-suggest |

---

## 前端頁面清單

| 頁面 | 路由 | 功能 | FN 對應 |
|------|------|------|---------|
| 材料規格 | /admin/material/specs | CRUD | FN-06-005~008 |
| 倉庫管理 | /admin/material/warehouses | CRUD | FN-06-001~004 |
| 供應商管理 | /admin/material/suppliers | CRUD | FN-06-009~012 |
| 合格材料 | /admin/material/approved | CRUD+批次匯入 | FN-06-013~016 |
| 採購管理 | /admin/material/purchase | 採購單+審核 | FN-06-017~022 |
| 收料入庫 | /admin/material/receiving | 收料+轉庫 | FN-06-023~025 |
| 庫存管理 | /admin/material/inventory | 查詢+盤點+預警 | FN-06-026~031 |
| 領料管理 | /admin/material/issues | 申請+核准+出料 | FN-06-032~036 |
| 盤點調整 | /admin/material/adjustment | 盤點+調整 | FN-06-027~028 |
| 報廢處理 | /admin/material/disposal | 廢品 | FN-06-038~039 |
