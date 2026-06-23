# CodeCortex — Copilot Agent Instructions

## 知識圖譜工具優先原則

你擁有 `codecortex-knowledge` MCP server 的工具存取權。
**在寫任何後端或前端程式碼之前，必須先查詢知識圖譜**，而不是依賴記憶或猜測。

---

## 生成新功能的標準工作流程

### Step 1：確認現有狀態
```
search_code_entity("<功能名稱>")          # 確認是否已有相關 class/endpoint
get_module_info("<target_module>")        # 確認模組職責與邊界
```

### Step 2：取得風格參考（黃金範例）
```
get_example_code("<module>", "controller")   # 後端 Controller 寫法
get_example_code("<module>", "service")      # Service 層寫法
get_example_code("<module>", "entity")       # Entity 寫法
```

### Step 3：確認跨模組依賴
```
get_module_exports("<dependency_module>")    # 對方暴露了哪些 public class
get_module_coupling("<from>", "<to>")        # 過去如何依賴，確保不破壞邊界
```

### Step 4：取得現有 DTO 合約
```
get_feature_contract("<EndpointPath>")       # 確認既有 API 的 request/response 結構
```

### Step 4.5：查詢業務約束（新增 🌟）
在寫 Service 層邏輯前，**必須**查詢該模組的隱式業務規則：
```
get_code_constraints(module="<target_module>")      # 看該模組有哪些 Exception 邊界
get_code_constraints(class_name="<TargetService>")  # 針對特定 Service
get_code_constraints(keyword="<關鍵字>")             # FTS5 語意搜尋
```
這些是前人留下的業務規則的程式碼形式——例如「指定部門公告必須選擇至少一個部門」。
**不查此工具直接寫 Service，等同於跳過業務規則審查。**

### Step 4.6：查詢測試邊界（新增 🌟）
在**修改**任何已存在的 Service 邏輯前，**必須**查詢測試規則：
```
get_test_rules(class_name="<TargetService>")   # 看有哪些行為合約
get_test_rules(module="<module>")              # 看整個模組的測試邊界
```
如果返回結果中有 `should throw when X`，你生成的程式碼**必須**包含對應的防呆邏輯。
**違反測試邊界 = 破壞現有功能，絕對不允許。**

### Step 5：生成程式碼
- Controller、Service、Repository、Entity 分層符合現有 `get_example_code` 返回的風格
- Request/Response DTO 欄位名稱**完全對齊** `get_feature_contract` 的 TypeScript Interface

---

## ⚡ 生成新 Controller 後的必要動作

> **這是最重要的規則。每次生成或修改 Java Controller 後，你必須立即執行以下動作。**

### 方法一：呼叫 VS Code Task（推薦）
在程式碼生成完成後，告知使用者：
```
已產生以下檔案：
  - backend/.../XxxController.java
  - backend/.../dto/XxxRequest.java
  - backend/.../dto/XxxResponse.java

請按下 ⇧⌘B 選擇「CodeCortex: Generate Contract (active file)」
或執行：
  python scripts/generate-contract.py --file path/to/XxxController.java
```

### 方法二：在終端直接執行
當你有終端存取權時，生成程式碼後**立即執行**：
```bash
# 針對新建的 Controller（最常見）
python scripts/generate-contract.py --file backend/src/main/java/com/taipei/iot/<module>/controller/XxxController.java

# 針對整個模組
python scripts/generate-contract.py --module <module_name>
```

### 執行後的效果（知識庫「自我生長」）
1. `feature_contracts` 表自動新增新 API 的 request/response TS interface
2. `frontend/src/types/generated/<module>.contracts.ts` 自動生成/更新
3. 下一次 AI 查詢 `get_feature_contract()` 就能拿到這個新 API 的精確 TS 型別

---

## 前後端資料結構一致性規則

1. Java `@RequestBody` 的欄位名稱 = TypeScript `axios.post(url, payload)` 的 payload key
2. Java return type（unwrapped from `BaseResponse<T>`）的欄位 = Vue 元件裡 `response.data` 的型別
3. 所有新 DTO 必須在生成後立即執行 `generate-contract.py` 讓知識庫同步
4. **嚴禁猜測 DTO 欄位**：先查 `get_feature_contract()`，查不到就執行 `generate-contract.py`

---

## 模組邊界規則

- 不同模組之間**只能**透過 `get_module_exports()` 確認的 public class 進行依賴
- 不得直接 import 其他模組的 internal class（非 exports 清單內的 class）
- 新建模組時，先用 `list_modules()` 確認命名慣例

---

## 常用快捷查詢範例

```
# 找郵件/通知相關 API
search_code_entity("郵件 通知", "api")

# 查某 endpoint 的完整 TS 合約
get_feature_contract("/api/v1/announcements")

# 查 auth 模組對外暴露的 class
get_module_exports("auth")

# 確認 user 和 dept 的耦合關係
get_module_coupling("user", "dept")

# 取得 rbac 模組的 Service 黃金範例
get_example_code("rbac", "service")
```
