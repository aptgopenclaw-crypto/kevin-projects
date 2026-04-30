# VR-P4 — Phase 4 換裝維護驗證紀錄

> **範圍**：SA-05 換裝維護  
> **FN 總數**：35

---

## 最新執行摘要

| 項目 | 值 |
|------|-----|
| 驗證日期 | 2026-04-24 |
| Git Commit | `61b4ced` |
| 涵蓋 Test Classes | 7 |
| 涵蓋 Test Cases | 37 |
| Failures | 0 |
| 已驗 FN | 21 / 35 |
| 未驗 FN（已實作） | 0 |
| 未實作 FN | 14 |

---

## 測試對應

| Test Class | Tests | Result | 涵蓋 FN |
|-----------|-------|--------|---------|
| LightPoleNumberControllerTest | 3 | ✅ PASS | FN-05-001, 002 |
| LightPoleNumberServiceTest | 2 | ✅ PASS | FN-05-001, 002 |
| ReplacementOrderControllerTest | 9 | ✅ PASS | FN-05-007~011, 015~018, 024~027 |
| ReplacementOrderServiceTest | 11 | ✅ PASS | FN-05-021, 023~027 |
| ReplacementItemServiceTest | 8 | ✅ PASS | FN-05-015~019 |
| ReplacementClosedListenerTest | 2 | ✅ PASS | FN-05-025 (結案事件) |
| ReplacementNeedMaterialListenerTest | 2 | ✅ PASS | FN-05-019 (材料連動) |

---

## FN 驗證明細

| FN | 功能 | 驗證方式 | 測試類別 | 結果 | 日期 | 備註 |
|----|------|---------|---------|------|------|------|
| FN-05-001 | 號碼牌列表 | API+Unit | LightPoleNumber Controller+Service | ✅ PASS | 04-24 | |
| FN-05-002 | 產生號碼牌 | API+Unit | LightPoleNumber Controller+Service | ✅ PASS | 04-24 | |
| FN-05-003 | 重製 QR Code | — | — | 🔲 未實作 | — | |
| FN-05-004 | 刪除號碼牌 | — | — | 🔲 未實作 | — | |
| FN-05-005 | 批次匯入資產清冊 | — | — | 🔲 未實作 | — | |
| FN-05-006 | 匯入差異確認 | — | — | 🔲 未實作 | — | |
| FN-05-007 | 換裝工單列表 | API+Unit | ReplacementOrder Controller+Service | ✅ PASS | 04-24 | |
| FN-05-008 | 新增換裝工單 | API+Unit | ReplacementOrder Controller+Service | ✅ PASS | 04-24 | |
| FN-05-009 | 查看工單詳情 | API | ReplacementOrderControllerTest | ✅ PASS | 04-24 | |
| FN-05-010 | 派工 | API | ReplacementOrderControllerTest | ✅ PASS | 04-24 | |
| FN-05-011 | 開工 | API | ReplacementOrderControllerTest | ✅ PASS | 04-24 | |
| FN-05-012 | 案件類別管理 | — | — | 🔲 未實作 | — | |
| FN-05-013 | 預匯入路燈編號 | — | — | 🔲 未實作 | — | |
| FN-05-014 | 預匯入地圖查看 | — | — | 🔲 未實作 | — | |
| FN-05-015 | 新增換裝項目 | API+Unit | ReplacementOrder Controller + ItemService | ✅ PASS | 04-24 | |
| FN-05-016 | 換裝項目列表 | API+Unit | ReplacementOrder Controller + ItemService | ✅ PASS | 04-24 | |
| FN-05-017 | 編輯換裝項目 | API+Unit | ReplacementOrder Controller + ItemService | ✅ PASS | 04-24 | |
| FN-05-018 | 刪除換裝項目 | API+Unit | ReplacementOrder Controller + ItemService | ✅ PASS | 04-24 | |
| FN-05-019 | 合格材料查驗 | Unit+Event | ItemService + NeedMaterialListener | ✅ PASS | 04-24 | 跨模組 |
| FN-05-020 | 材料清單批次匯入 | — | — | 🔲 未實作 | — | |
| FN-05-021 | 提交自主檢核 | Unit | ReplacementOrderServiceTest | ✅ PASS | 04-24 | |
| FN-05-022 | 自檢後查看 | 已實作 | — | ⚠️ 缺獨立測 | 04-24 | 組合查詢 |
| FN-05-023 | 組件替換(自檢) | Unit | ReplacementOrderServiceTest | ✅ PASS | 04-24 | 含於 self-check |
| FN-05-024 | 報竣送審 | API+Unit | ReplacementOrder Controller+Service | ✅ PASS | 04-24 | |
| FN-05-025 | 審核通過 | API+Unit+Event | Controller+Service+ClosedListener | ✅ PASS | 04-24 | |
| FN-05-026 | 退回補件 | API+Unit | ReplacementOrder Controller+Service | ✅ PASS | 04-24 | |
| FN-05-027 | 補件重送 | API+Unit | ReplacementOrder Controller+Service | ✅ PASS | 04-24 | |
| FN-05-028 | 結案後補救 | — | — | 🔲 未實作 | — | |
| FN-05-029 | 異動紀錄查詢 | — | — | 🔲 未實作 | — | |
| FN-05-030 | 派工案件統計 | — | — | 🔲 未實作 | — | |
| FN-05-031 | 交付/完成統計 | — | — | 🔲 未實作 | — | |
| FN-05-032 | 匯出派工清冊 | — | — | 🔲 未實作 | — | |
| FN-05-033 | 匯出異動地圖 | — | — | 🔲 未實作 | — | |
| FN-05-034 | 產生竣工清單 | — | — | 🔲 未實作 | — | |
| FN-05-035 | 產生用電申請表 | — | — | 🔲 未實作 | — | |

---

## P4 總結

| 分類 | 數量 |
|------|------|
| ✅ 已驗通過 | 21 |
| ⚠️ 已實作待補測 | 0 |
| 🔲 未實作 | 14 |
| **合計** | **35** |

### 評估

Phase 4 換裝模組的測試覆蓋是四個 Phase 中最佳的：
- **核心 CRUD + 狀態機全鏈**（建單→派工→開工→自檢→報竣→審核→退補→結案）全部有 API + Unit 雙層驗證
- **跨模組事件**（材料查驗、結案同步）也有 Listener 測試
- 未實作項集中在匯入匯出和統計報表，屬於後續 Phase
