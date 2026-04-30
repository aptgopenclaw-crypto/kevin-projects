Code Review 通常檢查以下幾個面向：

---

### 1. 正確性 (Correctness)
- 邏輯是否正確、有沒有 off-by-one、null pointer、race condition
- 邊界條件是否處理（空集合、null 輸入、超大數值）
- 錯誤處理是否完整（try-catch 有沒有吞掉例外）

### 2. 安全性 (Security)
- SQL Injection、XSS、CSRF 等 OWASP Top 10 風險
- 敏感資料是否外洩（log 裡印密碼、error message 洩漏內部資訊）
- 權限檢查是否完整（API 有沒有漏掛 `@PreAuthorize`）
- 硬編碼的密鑰、密碼、Token

### 3. 可讀性 (Readability)
- 命名是否清楚（變數、方法、類別）
- 函式是否太長、職責是否單一
- 有沒有「魔術數字」應該抽成常數
- 註解是否必要且正確（過時的註解比沒有更糟）

### 4. 設計與架構 (Design)
- 是否符合專案既有的分層架構（Controller → Service → Repository）
- 有沒有違反 SOLID 原則（例如一個 Service 做太多事）
- 耦合度是否過高、是否方便後續擴充
- DTO / Entity 是否正確分離（不直接把 Entity 回傳給前端）

### 5. 效能 (Performance)
- N+1 查詢問題（迴圈裡呼叫 DB）
- 不必要的全表掃描或缺少索引
- 大量資料未分頁
- 重複計算可以快取的結果

### 6. 測試 (Testing)
- 有沒有對應的單元測試 / 整合測試
- 測試是否覆蓋 happy path + edge case
- 測試是否穩定（不依賴外部狀態、順序）

### 7. 一致性 (Consistency)
- Coding style 是否符合團隊規範（命名慣例、縮排、import 排序）
- 錯誤回傳格式是否統一（例如你們的 `BaseResponse<T>`）
- API 路徑命名慣例（RESTful 風格是否一致）
- 相似功能的實作方式是否一致

### 8. 依賴與設定 (Dependencies & Config)
- 新增的依賴是否必要、有無已知 CVE
- 設定值是否用環境變數而非硬編碼
- 有沒有引入冗餘或重複的 library

---

### 實務上常用的輔助工具

| 類型 | 工具 | 作用 |
|---|---|---|
| **自動化風格檢查** | Checkstyle, ESLint, Prettier | 統一格式、減少 review 瑣碎討論 |
| **靜態分析** | SonarQube, SpotBugs | 自動找潛在 bug 和壞味道 |
| **PR Review 流程** | GitHub PR / GitLab MR | 逐行 diff、留言討論、Approve 機制 |
| **AI 輔助 Review** | GitHub Copilot Code Review | 自動掃描 PR 提供建議 |

---

### 重點觀念

> Code Review **不是抓語法錯誤**（那是 compiler 和 linter 的工作），而是檢查**人類才能判斷的東西**：邏輯合理性、設計決策、可維護性、安全風險。

以你的專案來說，最值得 review 的重點會是：
- **權限控制**有沒有每個 API 都掛到（你們有 URL-level + Method-level 雙層，要確認不漏）
- **多租戶隔離**的查詢有沒有繞過 TenantFilter 的情況
- **新 API** 是否遵循既有的 `BaseResponse<T>` + `@Valid` + `@AuditEvent` 模式