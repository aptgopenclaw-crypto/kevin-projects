# Phase 4 完成度追蹤

> 審查日期：2026-04-24
> 補齊日期：2026-04-24（全 6 項已修正，453 tests / 0 failures）

## 全部完成 ✅

### 後端（40+ 檔案，453 tests 全過 / 0 failures）

| 類別 | 檔案數 | 狀態 |
|------|--------|------|
| Entities + Enums | 7 | ✅ ReplacementOrder, ReplacementItem, LightPoleNumber + 4 enums |
| Repositories | 3 | ✅ TenantScopedRepository + 篩選查詢 + dateRange |
| DTOs | 8 | ✅ Request/Response/QueryParams(含dateFrom/dateTo)/SelfCheck/PoleNumber |
| Services | 3 | ✅ OrderService(12 methods) + ItemService(含材料驗證+deviceCode填入) + PoleNumberService |
| Listeners | 2 | ✅ E6(派工→領料) + E10(結案) |
| Controllers | 2 | ✅ 16 endpoints 全數符合 §8-4，list 支援 dateFrom/dateTo |
| Flyway | 3 | ✅ V45(tables) + V46(menus/perms) + V47(fix routes) |
| 既有檔案修改 | 6 | ✅ ErrorCode/AuditCategory/AuditEventType/SettingKey/SystemSettingService/IssueService |
| Tests | 7 suites | ✅ OrderService(11) + ItemService(8) + PoleNumber(2) + OrderController(9) + PoleNumberController(3) + ClosedListener(2) + NeedMaterialListener(2) |

### 前端（核心 + 進階元件）

| 檔案 | 狀態 |
|------|------|
| `types/replacement.ts` | ✅ 全部型別定義 |
| `api/replacement/index.ts` | ✅ 全部 API 呼叫 |
| `views/admin/replacement/ReplacementOrderView.vue` | ✅ 列表+篩選+分頁+建立 dialog，版面整理+i18n |
| `views/admin/replacement/ReplacementOrderDetailView.vue` | ✅ 詳情+明細+狀態轉換+規格比較，版面整理+i18n |
| `views/admin/replacement/ReplacementItemDialog.vue` | ✅ 4 步驟精靈（選燈桿→選設備→選材料→確認） |
| `views/admin/replacement/SelfCheckView.vue` | ✅ 自主檢核獨立頁面（逐設備確認+填備註） |
| `views/admin/replacement/PoleNumberView.vue` | ✅ 號碼牌管理，版面整理+i18n |
| `components/BeforeAfterSpecComparison.vue` | ✅ 換裝前後規格雙欄 diff 比較元件 |
| `router/index.ts` | ✅ 4 條靜態路由（含 self-check） |
| `locales/zh-TW.ts` / `en.ts` / `zh-CN.ts` | ✅ replacement section 完整（含 dialog/selfCheck/spec keys） |

---

## 已補齊項目（2026-04-24）

### ~~1. ReplacementItemService.addItem() 缺少合格材料驗證~~ ✅ 已修正
- 注入 `ApprovedMaterialRepository`，校驗 `approvedMaterialId` 存在且 `status = ACTIVE`
- 校驗 `oldDevice.parentDeviceId == request.parentDeviceId`
- 新增 3 個測試案例

### ~~2. ReplacementItemResponse.toResponse() 未填入 deviceCode~~ ✅ 已修正
- toResponse() 透過 `deviceService.getById()` 查詢填入 parentDeviceCode/oldDeviceCode/newDeviceCode
- try/catch 防止 id 為 null 時 NPE

### ~~3. ReplacementOrderQueryParams 缺少 dateRange~~ ✅ 已修正
- 加入 `LocalDate dateFrom` / `LocalDate dateTo`
- Repository JPQL 加入日期範圍條件
- Controller 加入 `@DateTimeFormat(iso = ISO.DATE)` 參數

### ~~4. ReplacementItemDialog.vue~~ ✅ 已建立
- 4 步驟精靈：選燈桿 → 選舊設備 → 選合格材料 → 確認
- 支援跳過材料選擇

### ~~5. SelfCheckView.vue~~ ✅ 已建立
- 獨立頁面，列出待檢核項目
- 逐設備填入 deviceCode + 備註，提交後導回詳情頁

### ~~6. BeforeAfterSpecComparison.vue~~ ✅ 已建立
- 雙欄 diff 顯示換裝前後規格
- 變更項目自動標記 badge
