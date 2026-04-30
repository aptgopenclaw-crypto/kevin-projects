# VR-P2 — Phase 2 報修維護驗證紀錄

> **範圍**：SA-04 報修維護 + 巡查  
> **FN 總數**：47

---

## 最新執行摘要

| 項目 | 值 |
|------|-----|
| 驗證日期 | 2026-04-24 |
| Git Commit | `61b4ced` |
| 涵蓋 Test Classes | 8 |
| 涵蓋 Test Cases | 48 |
| Failures | 0 |
| 已驗 FN | 25 / 47 |
| 未驗 FN（已實作） | 0 |
| 未實作 FN | 22 |

---

## 測試對應

| Test Class | Tests | Result | 涵蓋 FN |
|-----------|-------|--------|---------|
| RepairTicketControllerTest | 10 | ✅ PASS | FN-04-010~014, 016, 020 |
| RepairTicketServiceTest | 10 | ✅ PASS | FN-04-010~016, 022, 023 |
| TicketAttachmentServiceTest | 4 | ✅ PASS | FN-04-006~008 |
| RepairDispatchServiceTest | 5 | ✅ PASS | FN-04-014 |
| FaultApprovedListenerTest | 4 | ✅ PASS | FN-04-013 (障礙→報修) |
| RepairClosedListenerTest | 3 | ✅ PASS | FN-04-018, 029 |
| InspectionControllerTest | 6 | ✅ PASS | FN-04-030~034 |
| InspectionServiceTest | 6 | ✅ PASS | FN-04-030~035 |

---

## FN 驗證明細

| FN | 功能 | 驗證方式 | 測試類別 | 結果 | 日期 | 備註 |
|----|------|---------|---------|------|------|------|
| FN-04-001 | 民眾報修頁面 | — | — | 🔲 未實作 | — | 前端 |
| FN-04-002 | 提交民眾報修 | — | — | 🔲 未實作 | — | |
| FN-04-003 | 報修進度查詢 | — | — | 🔲 未實作 | — | |
| FN-04-004 | 1999 案件接收 | — | — | 🔲 未實作 | — | 外部介接 |
| FN-04-005 | 1999 結果回覆 | — | — | 🔲 未實作 | — | 外部介接 |
| FN-04-006 | 上傳附件 | Unit | TicketAttachmentServiceTest | ✅ PASS | 04-24 | |
| FN-04-007 | 附件列表 | Unit | TicketAttachmentServiceTest | ✅ PASS | 04-24 | |
| FN-04-008 | 下載附件 | Unit | TicketAttachmentServiceTest | ✅ PASS | 04-24 | |
| FN-04-009 | 刪除附件 | — | — | 🔲 未實作 | — | |
| FN-04-010 | 報修工單列表 | API+Unit | RepairTicket Controller+Service | ✅ PASS | 04-24 | |
| FN-04-011 | 新增報修工單 | API+Unit | RepairTicket Controller+Service | ✅ PASS | 04-24 | |
| FN-04-012 | 查看工單詳情 | API+Unit | RepairTicket Controller+Service | ✅ PASS | 04-24 | |
| FN-04-013 | 收案 | API+Unit | RepairTicket Controller+Service | ✅ PASS | 04-24 | FaultApprovedListener |
| FN-04-014 | 派工 | API+Unit | RepairTicket+Dispatch | ✅ PASS | 04-24 | |
| FN-04-015 | 開始處理 | Unit | RepairTicketServiceTest | ✅ PASS | 04-24 | |
| FN-04-016 | 完工回報 | API+Unit | RepairTicket Controller+Service | ✅ PASS | 04-24 | |
| FN-04-017 | 送審 | — | — | 🔲 未實作 | — | |
| FN-04-018 | 結案審核通過 | Unit | RepairClosedListenerTest | ✅ PASS | 04-24 | |
| FN-04-019 | 退回補件 | Unit | WorkflowServiceTest | ✅ PASS | 04-24 | via workflow |
| FN-04-020 | 改分轉送 | API | RepairTicketControllerTest | ✅ PASS | 04-24 | |
| FN-04-021 | 匯出案件清冊 | — | — | 🔲 未實作 | — | |
| FN-04-022 | 報修資訊欄位 | Unit | RepairTicketServiceTest | ✅ PASS | 04-24 | 含於 detail |
| FN-04-023 | 維護資訊欄位 | Unit | RepairTicketServiceTest | ✅ PASS | 04-24 | 含於 detail |
| FN-04-024 | 設備維護履歷 | — | — | 🔲 未實作 | — | |
| FN-04-025 | 設備概覽 | — | — | 🔲 未實作 | — | |
| FN-04-026 | 通知機關報告單 | — | — | 🔲 未實作 | — | |
| FN-04-027 | 查報單 | — | — | 🔲 未實作 | — | |
| FN-04-028 | 設計單 | — | — | 🔲 未實作 | — | |
| FN-04-029 | 結案後更新圖資 | Unit | RepairClosedListenerTest | ✅ PASS | 04-24 | Event |
| FN-04-030 | 巡查任務列表 | API+Unit | Inspection Controller+Service | ✅ PASS | 04-24 | |
| FN-04-031 | 新增巡查任務 | API+Unit | Inspection Controller+Service | ✅ PASS | 04-24 | |
| FN-04-032 | 編輯巡查任務 | API+Unit | Inspection Controller+Service | ✅ PASS | 04-24 | |
| FN-04-033 | 停用巡查任務 | API+Unit | Inspection Controller+Service | ✅ PASS | 04-24 | |
| FN-04-034 | 巡查紀錄列表 | API+Unit | Inspection Controller+Service | ✅ PASS | 04-24 | |
| FN-04-035 | 新增巡查紀錄 | Unit | InspectionServiceTest | ✅ PASS | 04-24 | |
| FN-04-036 | 編輯巡查紀錄 | — | — | 🔲 未實作 | — | |
| FN-04-037 | 巡查派工 | — | — | 🔲 未實作 | — | |
| FN-04-038 | 非契約案件篩選 | — | — | 🔲 未實作 | — | |
| FN-04-039 | 開放資料匯出 | — | — | 🔲 未實作 | — | |
| FN-04-040 | 里長通知設定 | — | — | 🔲 未實作 | — | |
| FN-04-041 | 里內故障通知 | — | — | 🔲 未實作 | — | |
| FN-04-042 | 維護案件統計 | — | — | 🔲 未實作 | — | |
| FN-04-043 | 維護時間統計 | — | — | 🔲 未實作 | — | |
| FN-04-044 | 通報來源統計 | — | — | 🔲 未實作 | — | |
| FN-04-045 | 故障分類統計 | — | — | 🔲 未實作 | — | |
| FN-04-046 | 故障熱區統計 | — | — | 🔲 未實作 | — | |
| FN-04-047 | 材料換修統計 | — | — | 🔲 未實作 | — | |

---

## P2 總結

| 分類 | 數量 |
|------|------|
| ✅ 已驗通過 | 25 |
| ⚠️ 已實作待補測 | 0 |
| 🔲 未實作 | 22 |
| **合計** | **47** |

### 下一步行動

1. Phase 2 核心報修/巡查流程已完整驗證
2. 未實作項集中在：民眾端報修(1-5)、外部介接(1999)、報表統計(42-47)、文件產出(26-28)
3. 這些屬於 Phase 5+ 或 UAT 階段才需處理
