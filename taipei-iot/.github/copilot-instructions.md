# GitHub Copilot Instructions

## CodeGraph — 優先使用程式碼圖譜查詢

此專案已建立 CodeGraph 索引（`.codegraph/`）。**在執行 grep、find、read_file 等工具搜尋程式碼之前，優先使用 CodeGraph CLI** 以減少 token 消耗與工具呼叫次數。

### 常用指令

```bash
# 探索某個功能或流程（最常用）
codegraph explore "<問題或關鍵字>"

# 查看某個 symbol 的完整程式碼與呼叫者
codegraph node <SymbolName>

# 全庫搜尋 symbol 名稱
codegraph search <name>

# 查詢呼叫某 function 的所有地方
codegraph callers <SymbolName>

# 分析修改某 symbol 的影響範圍
codegraph impact <SymbolName>
```

### 使用原則

- 需要了解一段流程怎麼走 → `codegraph explore "..."` 一次取得
- 需要看某個 class/method 的程式碼與被誰呼叫 → `codegraph node ClassName`
- 需要確認修改前的影響範圍 → `codegraph impact MethodName`
- 只有在 CodeGraph 找不到答案時，才改用 `grep_search` / `read_file`

### 索引維護

```bash
# 更新索引（有大量檔案變動後執行）
codegraph sync

# 強制重建完整索引
codegraph index --force

# 查看索引狀態
codegraph status
```

---

## 專案結構

```
taipei-iot/
├── backend/          Spring Boot (Java 21)
│   └── src/main/java/com/taipei/iot/
│       ├── assettransfer/   資產異動申請模組
│       ├── workflow/        簽核引擎（WorkflowEngine）
│       ├── auth/            認證與 JWT
│       ├── rbac/            角色權限
│       ├── user/            使用者管理
│       └── dept/            部門管理
├── frontend/         Vue 3 + Element Plus + Vite
│   └── src/
│       ├── views/           頁面元件
│       ├── api/             API 呼叫層
│       ├── stores/          Pinia stores
│       └── router/          路由設定
└── .codegraph/       CodeGraph 索引（勿手動修改）
```

## 技術重點

- **DB schema**：`iot_workflowdb`，PostgreSQL，連線 `jdbc:postgresql://localhost:5432/mydb`
- **Flyway migration**：`backend/src/main/resources/db/migration/V*.sql`，最新版本請用 `ls -1 backend/src/main/resources/db/migration/ | sort | tail -5`
- **程式碼格式化**：修改 Java 後執行 `mvn spring-javaformat:apply -q`（否則 compile 失敗）
- **前端 API baseURL**：`/v1`，所有 API 請求帶 `/v1` 前綴
- **Tenant filter**：所有 entity 查詢受 `@Filter(tenantFilter)` 保護，須在 Hibernate session 啟用
