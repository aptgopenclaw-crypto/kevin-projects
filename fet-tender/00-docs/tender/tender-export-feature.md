# 招標公告 / 決標公告匯出功能

## 1. 功能概述

使用者可在招標公告列表或決標公告列表頁面，依目前篩選條件將全部符合的資料一次匯出為 **Excel（XLSX）** 檔案，以利後續分析或存檔。

---

## 2. 使用流程

1. 進入「招標公告」或「決標公告」頁面。
2. 視需要設定篩選條件（方案名稱、關鍵字、機關名稱、標案名稱、日期區間等）。
3. 點擊篩選欄右側的 **「匯出」** 按鈕（綠色）。
4. 瀏覽器自動下載：
   - 招標公告 → `tender-announcements.xlsx`
   - 決標公告 → `tender-awards.xlsx`

> 匯出資料與目前篩選條件一致，**不受分頁限制**，會包含所有符合條件的記錄。

---

## 3. API 規格

### 3.1 招標公告匯出

| 項目 | 說明 |
|------|------|
| **Method** | `GET` |
| **URL** | `/v1/tender/announcements/export` |
| **權限** | `tender:announcement:export` |
| **稽核事件** | `EXPORT_TENDER_ANNOUNCEMENT` |
| **Response Content-Type** | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| **下載檔名** | `tender-announcements.xlsx` |

**Query Parameters：**

| 參數 | 型別 | 說明 |
|------|------|------|
| `solution` | String（optional） | 方案名稱，精確比對 |
| `keyword` | String（optional） | 關鍵字，模糊比對 |
| `agency` | String（optional） | 機關名稱，模糊比對 |
| `name` | String（optional） | 標案名稱，模糊比對 |
| `dateFrom` | LocalDate（optional） | 公告日期起（`YYYY-MM-DD`） |
| `dateTo` | LocalDate（optional） | 公告日期迄（`YYYY-MM-DD`） |

---

### 3.2 決標公告匯出

| 項目 | 說明 |
|------|------|
| **Method** | `GET` |
| **URL** | `/v1/tender/awards/export` |
| **權限** | `tender:award:export` |
| **稽核事件** | `EXPORT_TENDER_AWARD` |
| **Response Content-Type** | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| **下載檔名** | `tender-awards.xlsx` |

**Query Parameters：**

| 參數 | 型別 | 說明 |
|------|------|------|
| `solution` | String（optional） | 方案名稱，精確比對 |
| `keyword` | String（optional） | 關鍵字，模糊比對 |
| `agency` | String（optional） | 機關名稱，模糊比對 |
| `name` | String（optional） | 標案名稱，模糊比對 |
| `vendorName` | String（optional） | 廠商名稱，模糊比對 |
| `dateFrom` | LocalDate（optional） | 決標公告日期起（`YYYY-MM-DD`） |
| `dateTo` | LocalDate（optional） | 決標公告日期迄（`YYYY-MM-DD`） |

---

## 4. 匯出欄位

### 4.1 招標公告（工作表名稱：招標資料）

| 欄位 | 對應屬性 |
|------|----------|
| 項次 | 自動流水號 |
| 相關的Solution | `solution` |
| 關鍵字 | `matchedKeyword` |
| 機關名稱 | `agencyName` |
| 標案案號 | `tenderNumber` |
| 標案名稱 | `tenderName` |
| 採購性質 | `procurementType` |
| 招標方式 | `tenderMethod` |
| 公告日期 | `announcementDate`（格式：`yyyy/MM/dd`） |
| 截止投標日期 | `deadline`（格式：`yyyy/MM/dd HH:mm`） |
| 預算金額 | `budgetAmountRaw` |
| 是否訂有底價 | `hasBasePrice`（是／否） |
| 履約地點 | `performanceLocation` |
| 開標時間 | `openingTime`（格式：`yyyy/MM/dd HH:mm`） |
| 開標地點 | `openingLocation` |
| 詳細連結 | `detailUrl` |
| 機關代碼 | `agencyCode` |
| 單位名稱 | `unitName` |
| 機關地址 | `agencyAddress` |
| 聯絡人 | `contactPerson` |
| 聯絡電話 | `contactPhone` |
| 電子郵件信箱 | `contactEmail` |
| 標的分類 | `tenderCategory` |
| 採購金額級距 | `procurementAmountRange` |

共 **24 欄**。

---

### 4.2 決標公告（工作表名稱：決標資料）

| 欄位 | 對應屬性 |
|------|----------|
| 項次 | 自動流水號 |
| 相關的Solution | `solution` |
| 關鍵字 | `matchedKeyword` |
| 機關名稱 | `agencyName` |
| 標案案號 | `tenderNumber` |
| 標案名稱 | `tenderName` |
| 採購性質 | `procurementType` |
| 招標方式 | `tenderMethod` |
| 決標公告日期 | `awardAnnounceDate`（格式：`yyyy/MM/dd`） |
| 決標公告序號 | `awardAnnounceSeq` |
| 決標日期 | `awardDate`（格式：`yyyy/MM/dd`） |
| 決標金額 | `awardAmountRaw` |
| 決標方式 | `awardMethod` |
| 是否訂有底價 | `hasBasePrice`（是／否） |
| 履約期限 | `performancePeriod` |
| 履約地點 | `performanceLocation` |
| 廠商序位 | `vendorOrderSeq` |
| 廠商名稱 | `vendorName` |
| 廠商代碼 | `vendorTaxId` |
| 廠商地址 | `vendorAddress` |
| 廠商電話 | `vendorPhone` |
| 廠商決標金額 | `vendorAwardAmountRaw` |
| 詳細連結 | `detailUrl` |
| 機關代碼 | `agencyCode` |
| 單位名稱 | `unitName` |
| 機關地址 | `agencyAddress` |
| 聯絡人 | `contactPerson` |
| 聯絡電話 | `contactPhone` |
| 電子郵件信箱 | `contactEmail` |
| 標的分類 | `tenderCategory` |
| 採購金額級距 | `procurementAmountRange` |

共 **31 欄**；決標公告因「同一標案可有多家廠商」，每家廠商各佔一列。去重邏輯以 `(tenderNumber, awardAnnounceDate, awardAnnounceSeq, vendorOrderSeq)` 為鍵，避免同一標案被多個關鍵字爬取而重複輸出。

---

## 5. Excel 格式規格

| 項目 | 規格 |
|------|------|
| 函式庫 | Apache POI `SXSSFWorkbook`（Streaming，記憶體視窗 100 列） |
| 標頭樣式 | 深藍底色（`DARK_BLUE`）、白色粗體字、水平置中、下框線 |
| 資料列樣式 | 不換行、下髮絲框線（`HAIR`） |
| 日期欄樣式 | 下髮絲框線（`HAIR`） |
| 自動篩選 | 第 1 列所有欄位套用 Auto Filter |
| 凍結窗格 | 凍結第 1 列（標頭列） |
| 欄寬 | 依各欄內容類型預設固定寬度（單位：1/256 字元寬） |

---

## 6. 權限設計

| 權限代碼 | permission_id | 名稱 | 群組 |
|----------|---------------|------|------|
| `tender:announcement:export` | `PERM_TENDER_ANN_EXPORT` | 匯出招標公告 | 招標管理 |
| `tender:award:export` | `PERM_TENDER_AWARD_EXPORT` | 匯出決標公告 | 招標管理 |

**角色綁定（Flyway V56）：**

| 角色 | 取得權限 |
|------|----------|
| `ADMIN` | `tender:announcement:export`、`tender:award:export` |
| `OPERATOR` | `tender:announcement:export`、`tender:award:export` |
| `VIEWER` | `tender:announcement:export`、`tender:award:export` |

> 所有可瀏覽招標資料的角色皆同時獲得匯出權限。

---

## 7. 稽核記錄

每次匯出動作均寫入稽核日誌（`user_event_log`）：

| 事件類型 | AuditCategory |
|----------|---------------|
| `EXPORT_TENDER_ANNOUNCEMENT` | `TENDER` |
| `EXPORT_TENDER_AWARD` | `TENDER` |

---

## 8. 相關程式檔案

| 類型 | 路徑 |
|------|------|
| Controller（招標） | `backend/.../tender/controller/TenderAnnouncementController.java` |
| Controller（決標） | `backend/.../tender/controller/TenderAwardController.java` |
| Service（招標） | `backend/.../tender/service/TenderAnnouncementService.java` |
| Service（決標） | `backend/.../tender/service/TenderAwardService.java` |
| Excel 產生器（招標） | `backend/.../tender/service/TenderAnnouncementExcelExporter.java` |
| Excel 產生器（決標） | `backend/.../tender/service/TenderAwardExcelExporter.java` |
| Repository（招標） | `backend/.../tender/repository/TenderAnnouncementRepository.java` |
| Repository（決標） | `backend/.../tender/repository/TenderAwardRepository.java` |
| 權限 Migration | `backend/.../db/migration/V56__tender_export_permissions.sql` |
| 前端 API | `frontend/src/api/tender/index.ts` |
| 前端頁面（招標） | `frontend/src/views/tender/TenderAnnouncementView.vue` |
| 前端頁面（決標） | `frontend/src/views/tender/TenderAwardView.vue` |
