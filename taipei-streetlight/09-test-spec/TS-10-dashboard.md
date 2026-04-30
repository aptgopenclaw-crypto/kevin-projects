# TS-10 儀表板 — Test Specification (Forward Design)

> **對應 SA**：SA-10-dashboard (FN-10-001 ~ FN-10-022)  
> **對應 SD**：SD-10-dashboard  
> **Test Classes**：0 (尚未實作)  
> **Phase**：Phase 8 — 儀表板與 GIS

---

## 使用方式

本文件為 **前瞻設計 TC**，用於 Phase 8 實作時的驗收標準。  
所有 TC 均為 ⬜ 狀態，待實作時轉為 ✅。

---

## 1. 版面管理 (FN-10-001 ~ FN-10-003)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-001-01 | FN-10-001 版面查詢 | 載入用戶版面 | GET /dashboard/layout | 200, layout JSON | widget 配置 |
| TC-10-001-02 | FN-10-001 | 首次使用 | GET /dashboard/layout | 200, default layout | 預設版面 |
| TC-10-002-01 | FN-10-002 版面儲存 | 拖拉後儲存 | PUT /dashboard/layout | 200 | layout persisted |
| TC-10-003-01 | FN-10-003 重置 | 重置為預設 | POST /dashboard/layout/reset | 200, default | default layout |

---

## 2. 養護 Widget (FN-10-004 ~ FN-10-005)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-004-01 | FN-10-004 養護統計 | 案件數/完成率 | GET /widgets/maintenance | 200, stats | total/completed/rate |
| TC-10-005-01 | FN-10-005 養護趨勢 | 月趨勢圖 | GET /widgets/maintenance/trend | 200, monthly data | 12 months |

---

## 3. 停電 Widget (FN-10-006 ~ FN-10-007)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-006-01 | FN-10-006 停電統計 | 當月停電次數 | GET /widgets/outage | 200, count | by zone |
| TC-10-007-01 | FN-10-007 停電趨勢 | 歷史趨勢 | GET /widgets/outage/trend | 200, trend | monthly |

---

## 4. 故障 Widget (FN-10-008 ~ FN-10-009)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-008-01 | FN-10-008 故障熱力圖 | GeoJSON 熱力數據 | GET /widgets/fault-heatmap | 200, GeoJSON | intensity |
| TC-10-009-01 | FN-10-009 故障分類 | 按分類統計 | GET /widgets/fault-category | 200, by category | count per type |

---

## 5. KPI Widget (FN-10-010 ~ FN-10-011)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-010-01 | FN-10-010 KPI 摘要 | KPI 卡片數據 | GET /widgets/kpi | 200, indicators | score + grade |
| TC-10-011-01 | FN-10-011 KPI 趨勢 | KPI 月趨勢 | GET /widgets/kpi/trend | 200, trend | 6 months |

---

## 6. 路燈 Widget (FN-10-012 ~ FN-10-013)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-012-01 | FN-10-012 路燈數量 | 總數/分區 | GET /widgets/lamp-count | 200, count | by district |
| TC-10-013-01 | FN-10-013 在線/離線 | 在線率 | GET /widgets/lamp-status | 200, online/offline | percentage |

---

## 7. 配電箱 Widget (FN-10-014 ~ FN-10-015)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-014-01 | FN-10-014 用電統計 | 配電箱用電 | GET /widgets/panel-box | 200, kWh | by zone |
| TC-10-015-01 | FN-10-015 異常告警 | 配電箱告警 | GET /widgets/panel-box/alerts | 200, alerts | severity |

---

## 8. 其他 Widget (FN-10-016 ~ FN-10-020)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-016-01 | FN-10-016 附件統計 | 附件/照片數 | GET /widgets/attachments | 200, count | by type |
| TC-10-017-01 | FN-10-017 月電費 | 月電費統計 | GET /widgets/electricity-cost | 200, cost | NTD |
| TC-10-018-01 | FN-10-018 電費趨勢 | 電費趨勢圖 | GET /widgets/electricity-cost/trend | 200, trend | 12 months |
| TC-10-019-01 | FN-10-019 電表讀數 | 即時讀數 | GET /widgets/meter | 200, readings | kW + kWh |
| TC-10-020-01 | FN-10-020 電表趨勢 | 用電趨勢 | GET /widgets/meter/trend | 200, trend | daily |

---

## 9. GIS & 鑽取 (FN-10-021 ~ FN-10-022)

| TC ID | FN | 場景 | API | 預期結果 | 驗證點 |
|-------|-----|------|-----|---------|--------|
| TC-10-021-01 | FN-10-021 GIS 總覽 | 設備 GeoJSON | GET /widgets/gis | 200, GeoJSON | 全市設備 |
| TC-10-022-01 | FN-10-022 Widget 鑽取 | 點擊數字 → 明細 | (前端路由) | 導向對應列表頁 | router.push |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ⬜ 全部待實作 | 24 |
| **總 TC 數** | **24** |
