為您將 `VendorDashboardController.java` 的程式碼轉化為 **系統設計文件 (System Design Document, SD)** 中標準的「介面設計 / API 規格」格式。

此模組為數據密集型（Dashboard）的讀取端點，與前兩個設定管理的 Controller 不同，這裡專注於**聚合查詢與數據視覺化支援**。您可以直接將以下內容整合到您的 SD 文件中：

---

### 模組：廠商得標分析儀表板 (Vendor Award Analysis Dashboard)

#### 1. 元件概述 (Component Overview)
* **元件名稱**：`VendorDashboardController`
* **元件職責**：作為 RESTful API 的入口控制器，專門提供「廠商維度」的得標數據統計、趨勢分析、業務版圖與關聯洞察，以支援前端儀表板 (Dashboard) 的各種圖表與卡片渲染。
* **基礎路徑 (Base Path)**：`/v1/tender/vendor-dashboard`
* **所屬領域**：招標系統 (Tender) / 數據分析與儀表板 (Analytics & Dashboard)

#### 2. 權限與安全設計 (Security & Authorization)
本控制器採用 Spring Security 進行**類別級別**的權限控管，所有端點統一套用：
* **檢視權限 (Read)**：需具備 `tender:award:view` 角色/權限。

#### 3. API 介面規格 (API Specification)

> **⚠️ 核心識別邏輯備註**：所有需要指定廠商的端點，均遵循「**以 `vendorTaxId` (統一編號) 精確比對為主；若未提供 `vendorTaxId`，則回退 (fallback) 至 `vendorName` (廠商名稱) 進行完全相符比對**」的邏輯。

##### 3.1 廠商模糊搜尋建議 (Vendor Suggest)
* **路徑**：`GET /v1/tender/vendor-dashboard/suggest`
* **說明**：提供前端輸入框的自動完成 (Auto-complete) 建議列表。
* **請求參數 (Query Parameters)**：
  | 參數名稱 | 類型 | 必填 | 預設值 | 說明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `q` | String | 否 | `""` (空字串) | 搜尋關鍵字。 |
* **內部邏輯**：若 `q` 為 null 或去除空白後長度小於 2，直接回傳空列表，避免無意義的資料庫查詢。
* **回應規格**：`BaseResponse<List<VendorSuggestResponse>>`

##### 3.2 KPI 摘要卡片 (Overview)
* **路徑**：`GET /v1/tender/vendor-dashboard/overview`
* **說明**：取得該廠商的核心關鍵績效指標 (如：總得標數、總得標金額、平均決標金額等)。
* **請求參數 (Query Parameters)**：
  | 參數名稱 | 類型 | 必填 | 說明 |
  | :--- | :--- | :--- | :--- |
  | `vendorTaxId` | String | 否 | 廠商統一編號 (優先使用)。 |
  | `vendorName` | String | **是** | 廠商名稱 (當無 taxId 時作為主要識別)。 |
* **回應規格**：`BaseResponse<VendorOverviewResponse>`

##### 3.3 得標時間趨勢 (Trend)
* **路徑**：`GET /v1/tender/vendor-dashboard/trend`
* **說明**：取得廠商得標數量或金額的時間序列數據。
* **請求參數**：同 3.2 (`vendorTaxId`, `vendorName`)
* **內部邏輯**：Service 層需根據查詢的資料時間跨度 (Span)，**自動動態調整聚合粒度**（例如：短期資料按「日」，中期按「月」，長期按「季」），以優化圖表顯示與查詢效能。
* **回應規格**：`BaseResponse<VendorTrendResponse>`

##### 3.4 Solution × Keyword 業務版圖 (Solution Breakdown)
* **路徑**：`GET /v1/tender/vendor-dashboard/solution-breakdown`
* **說明**：取得該廠商在不同解決方案 (Solution) 與關鍵字 (Keyword) 組合下的得標分布，專為前端 **Treemap (樹狀圖)** 視覺化提供資料結構。
* **請求參數**：同 3.2 (`vendorTaxId`, `vendorName`)
* **回應規格**：`BaseResponse<List<VendorSolutionNode>>`

##### 3.5 發包機關排行榜 (Top Agencies)
* **路徑**：`GET /v1/tender/vendor-dashboard/top-agencies`
* **說明**：取得該廠商最常得標（或得標金額最高）的發包機關排名。
* **請求參數**：
  | 參數名稱 | 類型 | 必填 | 預設值 | 說明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `vendorTaxId` | String | 否 | 無 | 廠商統一編號。 |
  | `vendorName` | String | **是** | 無 | 廠商名稱。 |
  | `limit` | Integer | 否 | `10` | 回傳筆數上限。 |
* **內部邏輯**：具備防禦性程式設計，後端會強制限制 `limit` 最大值為 `30` (`Math.min(limit, 30)`)，防止過度查詢。
* **回應規格**：`BaseResponse<List<VendorTopAgencyResponse>>`

##### 3.6 採購屬性分布 (Procurement Profile)
* **路徑**：`GET /v1/tender/vendor-dashboard/procurement-profile`
* **說明**：取得該廠商得標案件的屬性統計，通常用於繪製圓餅圖或長條圖。包含：招標方式、採購類型、決標方式的分布比例。
* **請求參數**：同 3.2 (`vendorTaxId`, `vendorName`)
* **回應規格**：`BaseResponse<VendorProcurementProfileResponse>`

##### 3.7 共同得標廠商排行 (Co-Vendors)
* **路徑**：`GET /v1/tender/vendor-dashboard/co-vendors`
* **說明**：分析並列出與目標廠商「出現在同一個標案中」的其他廠商及其共同得標次數，用於發現潛在的合作夥伴或競爭對手網絡。
* **請求參數**：同 3.2 (`vendorTaxId`, `vendorName`)
* **回應規格**：`BaseResponse<List<VendorCoVendorResponse>>`

---

#### 4. 相依組件與資料傳輸 (Dependencies & DTOs)
* **相依服務 (Service Layer)**：
  * `VendorDashboardService`：負責複雜的 SQL 聚合查詢 (Aggregation)、分組 (Group By) 邏輯，以及動態時間粒度的計算。
* **資料傳輸物件 (DTOs)**：
  * 請求：本 Controller 主要依賴 `@RequestParam`，無複雜 Request Body。
  * 回應：`VendorSuggestResponse`, `VendorOverviewResponse`, `VendorTrendResponse`, `VendorSolutionNode`, `VendorTopAgencyResponse`, `VendorProcurementProfileResponse`, `VendorCoVendorResponse`。
  * 包裝：`BaseResponse`。

---

#### 💡 SD 文件撰寫進階建議 (給您的提醒)

由於這是 **Dashboard (儀表板)** 模組，查詢通常涉及大量資料的掃描與聚合，建議在 SD 文件的「非功能性需求 (Non-Functional Requirements)」或「資料庫設計」章節補充以下說明：

1. **查詢效能與索引 (Indexing)**：
   * 必須確保底層標案/得標資料表在 `(vendor_tax_id)` 和 `(vendor_name)` 欄位上建立了適當的 Index。
   * 若資料量龐大，Service 層的 `getTrend` 或 `getSolutionBreakdown` 可能需要考慮引入 **Read Replica (讀取複製)** 或 **Redis 快取**，並設定合理的 TTL (例如 1 小時)，以免拖垮主資料庫。
2. **廠商名稱清洗**：
   * 在「回退至 `vendorName` 完全相符」的邏輯中，需確認 Service 層是否有處理名稱前後的空白 (trim) 或全半形轉換，以確保比對成功率。
3. **空值處理 (Null Safety)**：
   * 對於新成立或尚未得標的廠商，所有聚合查詢應確保回傳結構完整（例如數值回傳 `0`，列表回傳 `[]`），避免前端圖表庫因收到 `null` 而崩潰。