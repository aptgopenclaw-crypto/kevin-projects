# SRS-09 績效管理

> **對應需求**：§9-(1) ~ §9-(2)  
> **設計參照**：`/02-spec/09-performance-management.md`  
> **狀態**：❌ 全模組未開始

---

## SRS-09-001 KPI 定義管理

**來源**：§9-(1)-A

### User Story

> 身為 **GOV_ADMIN**，我可新增/修改/刪除績效指標評核項目、計算公式及條件設定。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-09-001-1 | 可新增績效指標：名稱、分類、計算公式、權重、滿分 |
| AC-09-001-2 | 計算公式支援動態定義（非寫死程式碼） |
| AC-09-001-3 | 可設定加分/扣分條件 |
| AC-09-001-4 | 可修改/刪除/停用指標（停用不影響歷史計分） |

### 資料模型

```
kpi_definitions: id, name, category, formula_expr, weight, max_score,
                 conditions(JSONB), description, enabled, tenant_id
kpi_periods: id, period_name, start_date, end_date, status(OPEN/LOCKED), tenant_id
```

### 技術設計

- **公式引擎**：SpEL（Spring Expression Language）或嵌入式 JavaScript 引擎（GraalJS）
- **公式範例**：`(completed_count / total_count) * 100 * weight`
- **變數來源**：跨模組查詢（維修完成數、智能路燈可用率、巡查完成率等）

### 狀態：❌ 未開始

---

## SRS-09-002 KPI 數據匯入

**來源**：§9-(1)-B

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-09-002-1 | 可單筆輸入計分所需數據 |
| AC-09-002-2 | 可整批匯入數據（CSV/Excel） |
| AC-09-002-3 | 匯入前驗證資料格式 |

### 資料模型

```
kpi_data_imports: id, period_id, definition_id, value, source(AUTO/MANUAL/IMPORT),
                  imported_by, imported_at, tenant_id
```

### 狀態：❌ 未開始

---

## SRS-09-003 自動計算/呈現/匯出

**來源**：§9-(1)-C

### User Story

> 身為 **GOV_MGR**，我可查看各期各項績效指標的計算結果、服務水準與績效分數，並匯出報表。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-09-003-1 | 依公式自動計算各期各項指標得分 |
| AC-09-003-2 | 介面呈現績效統計分數（表格+圖表） |
| AC-09-003-3 | 可匯出加扣點明細報表 |
| AC-09-003-4 | 可匯出估驗計價報表 |

### 資料模型

```
kpi_results: id, period_id, definition_id, raw_value, calculated_score,
             details(JSONB), calculated_at, tenant_id
```

### 跨模組資料來源

| 指標 | 來源模組 | 數據 |
|------|---------|------|
| 照明妥善率 | §5 + §8 | 可用設備數 / 總設備數 |
| 維修時效 | §5 | 維修案件平均完修時間 |
| 智能路燈可用率 | §8 | 在線設備數 / 總智能設備數 |
| 巡查完成率 | §5 | 已完成巡查 / 排定巡查 |
| 換裝進度 | §6 | 已完成 / 派工數 |

### 狀態：❌ 未開始

---

## SRS-09-004 期間鎖定功能

**來源**：§9-(1)-D

### User Story

> 身為 **GOV_MGR**，我可鎖定某期績效計分，鎖定後即使後續資料更新也不影響該期分數。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-09-004-1 | 可鎖定指定期間的績效計分 |
| AC-09-004-2 | 鎖定後該期分數不因資料更新而變動 |
| AC-09-004-3 | 鎖定操作需經授權審核 |
| AC-09-004-4 | 如需解鎖需特殊審核程序 |

### 資料模型

```
kpi_locks: id, period_id, locked_by, locked_at, unlock_by, unlock_at, reason
```

### 狀態：❌ 未開始

---

## SRS-09-005 公式彈性調整

**來源**：§9-(2)

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-09-005-1 | 績效計算公式可由機關依決議調整，無需修改程式碼 |
| AC-09-005-2 | 公式變更後，新期間使用新公式，歷史期間（已鎖定）不受影響 |
| AC-09-005-3 | 公式變更留稽核紀錄 |

### 狀態：❌ 未開始
