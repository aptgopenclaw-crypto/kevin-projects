# SRS-06 換裝維護管理

> **對應需求**：§6-(1) ~ §6-(12)  
> **設計參照**：`/02-spec/06-replacement-maintenance.md`、`/_archive/x-plan/phase-4-replacement.md`、`/_archive/x-plan/phase-4-todo.md`  
> **狀態**：⚠️ 核心流程已完成，批次匯入/統計/匯出待開發

---

## SRS-06-001 號碼牌編號管理

**來源**：§6-(1)

### User Story

> 身為 **GOV_STAFF**，我可依編號規則產出不重複的路燈號碼牌編號，並產出對應的 QR Code 供製作張貼。

### 主要流程

1. 系統依機關編號規則（行政區碼+流水號）產生不重複編號
2. 可批次產出 N 筆新編號
3. 每筆編號自動產生 QR Code（連結至公開報修頁 `{frontend_base_url}/public/repair?pole={pole_number}`）
4. 可匯出 QR Code 圖檔供印刷
5. 可重製（補印）遺失/損壞的號碼牌

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-001-1 | 依機關規則產出不重複編號 |
| AC-06-001-2 | 每筆編號自動產出 QR Code |
| AC-06-001-3 | QR Code 掃描後連結至報修網頁，自動帶入路燈資訊 |
| AC-06-001-4 | 可批次產出、匯出、重製 |

### 資料模型

```
light_pole_numbers: id, pole_number, qr_code_url, status(AVAILABLE/ASSIGNED/DAMAGED),
                    assigned_device_id, tenant_id
```

### API 端點

| Method | Path | 說明 |
|--------|------|------|
| GET | `/v1/replacement/pole-numbers` | 列表 |
| POST | `/v1/replacement/pole-numbers/batch` | 批次產出 |
| PUT | `/v1/replacement/pole-numbers/{id}` | 修改 |
| POST | `/v1/replacement/pole-numbers/{id}/regenerate-qr` | 重製 QR |

### 狀態：✅ 已完成

---

## SRS-06-002 路燈資產清冊批次匯入

**來源**：§6-(2)

### User Story

> 身為 **CTR_ADMIN**，我可上傳 CSV/Excel 檔批次匯入路燈普查資料，一次更新大量資產資訊。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-002-1 | 支援 CSV 與 Excel（.xlsx）格式上傳 |
| AC-06-002-2 | 匯入前驗證資料格式（必填欄位、資料型態、編號唯一性） |
| AC-06-002-3 | 普查資訊含：分電箱編號、詳細地址、地點描述、台電桿號、換裝前後燈種/瓦數 |
| AC-06-002-4 | 匯入結果顯示成功/失敗筆數，失敗原因可下載 |
| AC-06-002-5 | 匯入前可預覽差異，確認後才寫入 |

### 狀態：❌ 未開始

---

## SRS-06-003 資產異動案件管理

**來源**：§6-(3)  
**驗收**：以案件方式管理資產異動紀錄，每筆異動可追溯。  
**狀態**：✅ 已完成（replacement_orders + replacement_items + device_events）

---

## SRS-06-004 材料清單批次匯入

**來源**：§6-(4)

### User Story

> 身為 **GOV_STAFF**，我可批次上傳審驗合格的燈具/控制器材料清單。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-004-1 | 支援 CSV / Excel 批次匯入 |
| AC-06-004-2 | 清單含：契約名稱、燈具/控制器編號、審驗合格日期、生產批號、廠牌、型號 |
| AC-06-004-3 | 燈具另含：型號、色溫、額定光通量、電壓、電流、功率 |
| AC-06-004-4 | 匯入前驗證編號唯一性、必填欄位 |

### 資料模型

```
approved_materials: id, material_spec_id, serial_number, batch_number,
                    brand, model, approval_date, contract_name,
                    status(ACTIVE/EXPIRED/REVOKED), tenant_id
```

### 狀態：⚠️ 單筆 CRUD 已完成，批次匯入待開發

---

## SRS-06-005 合格材料管控

**來源**：§6-(5)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-005-1 | 維護或換裝時，僅允許使用經審驗合格且狀態 ACTIVE 的材料 |
| AC-06-005-2 | 燈具、智能控制器、號碼牌等均需比對平台已建置編號 |
| AC-06-005-3 | 使用未登錄或非 ACTIVE 材料時系統拒絕並提示 |

### 技術實作

- `ReplacementItemService.addItem()` 驗證 `approvedMaterialId` 存在且 `status = ACTIVE`
- ErrorCode: `MATERIAL_NOT_AVAILABLE("85011", 400)`, `APPROVED_MATERIAL_NOT_FOUND("85008", 404)`

### 狀態：✅ 已完成（含測試）

---

## SRS-06-006 派工管理

**來源**：§6-(6)

### User Story

> 身為 **GOV_STAFF**，我可依契約開立換裝派工單，填寫事由/類別/地點/數量/工期，指派給廠商。

### 主要流程

1. 承辦人建立換裝派工單（ReplacementOrder）
2. 填寫：派工事由、案件類別、地點、預計數量、預計工期
3. 指派給維護廠商
4. 廠商接收後，逐項新增換裝明細（ReplacementItem）
5. 廠商透過派工單管控進度

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-006-1 | 可建立派工單，含事由、類別、地點、數量、工期 |
| AC-06-006-2 | 可指派給維護廠商 |
| AC-06-006-3 | 廠商可查看派工單、新增換裝明細 |
| AC-06-006-4 | 可追蹤每張派工單的完成進度 |

### 資料模型

```
replacement_orders: id, order_number, order_type, status(DRAFT/DISPATCHED/IN_PROGRESS/
                    SELF_CHECKED/PENDING_REVIEW/CLOSED/RETURNED), description,
                    location, expected_quantity, expected_completion_date,
                    contractor_id, created_by, tenant_id
replacement_items: id, order_id, parent_device_id, old_device_id, new_device_id,
                   material_spec_id, approved_material_id, status, 
                   before_spec(JSONB), after_spec(JSONB), device_code, notes
```

### 狀態：✅ 已完成（16 API endpoints）

---

## SRS-06-007 派工案件類別管理

**來源**：§6-(7)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-007-1 | 案件類別含：新設（補裝）、換裝（更換規格）、遷移、停用（廢止）、調整方向/角度、加裝遮光罩 |
| AC-06-007-2 | 類別可依機關需求彈性新增/修改/停用 |

### 技術實作

```java
enum ReplacementOrderType {
    NEW_INSTALL, REPLACEMENT, RELOCATION, 
    DECOMMISSION, ADJUSTMENT, SHADE_INSTALL
}
```

### 狀態：✅ 已完成

---

## SRS-06-008 新設/遷移預匯入+地圖查詢

**來源**：§6-(8)  
**驗收**：新設/遷移案件可預先匯入路燈編號與位置，提供地圖查詢便於現場辨識。  
**狀態**：❌ 未開始（依賴 GIS 整合）

---

## SRS-06-009 廠商自主檢核

**來源**：§6-(9)

### User Story

> 身為 **CTR_ADMIN**，換裝完成後我執行自主檢核，系統預先更新資產資訊，地圖/清冊立即反映最新狀態。

### 主要流程

1. 廠商完成現場換裝作業
2. 進入自主檢核頁面（SelfCheckView），逐設備確認
3. 填入新設備 deviceCode（掃描實體標籤）、備註
4. 提交自主檢核 → 系統呼叫 `DeviceService.replaceComponent()`
5. 舊設備除役（DECOMMISSIONED）、新設備建立並掛載至燈桿
6. 地圖與清冊立即查看最新資產

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-009-1 | 廠商可在獨立頁面執行自主檢核 |
| AC-06-009-2 | 自主檢核完成即預先更新系統資產 |
| AC-06-009-3 | 更新後可於路燈地圖/清冊查看最新資產 |
| AC-06-009-4 | 每項檢核記錄 deviceCode 與備註 |

### 事件驅動

- **E10**：換裝結案 → device_events + 確認庫存扣減

### 狀態：✅ 已完成（SelfCheckView.vue + 4-step ReplacementItemDialog）

---

## SRS-06-010 審核流程

**來源**：§6-(10)

### 狀態流轉圖

```
DRAFT → DISPATCHED → IN_PROGRESS → SELF_CHECKED → PENDING_REVIEW → CLOSED
                                                 ↘ RETURNED → PENDING_REVIEW
```

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-010-1 | 完工後自主檢核、報竣審核、結案審核等多階段機制 |
| AC-06-010-2 | 審退後有更正補件重送機制 |
| AC-06-010-3 | 已結案案件被查核發現錯誤時，具備補救機制 |
| AC-06-010-4 | 審核歷程完整可追溯 |

### 狀態：✅ 已完成（REPLACEMENT_REVIEW 工作流 + 7 狀態 FSM）

---

## SRS-06-011 統計報表

**來源**：§6-(11)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-011-1 | 可查看各契約之派工案件類型 |
| AC-06-011-2 | 已交付件數與實際完成件數 |
| AC-06-011-3 | 分類累積數量 |
| AC-06-011-4 | 執行進度百分比 |

### 狀態：❌ 未開始

---

## SRS-06-012 匯出清冊與竣工清單

**來源**：§6-(12)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-06-012-1 | 可依篩選條件匯出派工案件清冊 |
| AC-06-012-2 | 可匯出異動路燈地圖 |
| AC-06-012-3 | 可產出竣工清單供查核 |
| AC-06-012-4 | 可產出用電申請地點表供受理用電變更 |

### 狀態：❌ 未開始
