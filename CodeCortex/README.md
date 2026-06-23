# CodeCortex — Personal Code Knowledge Graph for AI-Assisted Development

> 🌐 Language: **English** | [繁體中文](README.zh-TW.md)

---

## 1. Pain Points

### The Core Problem: AI Guessing in a Large Codebase

The IoT platform template contains 14 business modules, 100 REST endpoints, and 324 Java classes.  
When an AI Agent tries to generate new features based on this template, it faces three layers of failure:

| Failure Mode | Root Cause |
|---|---|
| **DTO field hallucination** | `userName` or `username`? The AI doesn't know — it guesses |
| **Module boundary violations** | Directly importing internal classes from other modules, breaking architectural isolation |
| **Style inconsistency** | Generated Service naming and error handling clashes with existing code |
| **Business rule blindness** | Unaware of implicit constraints like "targetDeptIds is required when scope=DEPT" |
| **Test contract ignorance** | Modifying a Service without knowing which behaviors are locked by tests — breaking them silently |
| **Context overflow** | Dozens of modules can't fit in a context window — AI only reads fragments |

---

## 2. How It Solves Them

### Core Strategy: Make Implicit Knowledge Explicitly Queryable

Instead of making AI read more code, CodeCortex **extracts, structures, and indexes** the knowledge embedded in the codebase into SQLite, then exposes it via an **MCP Server (20 tools)** for AI agents to query on demand.

```
Java / Vue / TS source code
        │  tree-sitter AST parsing
        ↓
  knowledge.db (SQLite + FTS5)
        │  MCP Server
        ↓
  AI Agent precise queries
        │
        ↓
  Code generated to match existing conventions
```

### Five Key Design Decisions

**1. DTO Contracts (eliminate field hallucination)**  
`feature_contracts` automatically converts Java `@RequestBody` / return types into TypeScript Interfaces, stored in DB and emitted as `frontend/src/types/generated/<module>.contracts.ts`. AI calls `get_feature_contract()` and gets exact types — no more guessing.

**2. Explicit Module Boundaries (prevent architectural violations)**  
`module_exports` (public classes exposed externally) and `module_coupling` (actual cross-module reference edges) tell the AI exactly which classes can be referenced and which cannot.

**3. Golden Example Injection (style consistency)**  
`module_examples` stores real source code for each module × class type combination. AI calls `get_example_code()` for few-shot prompting — directly replicating naming conventions and structural patterns.

**4. Business Constraint Extraction (prevent rule blindness)**  
`code_constraints` auto-extracts implicit business rules from `throw new XxxException("...")` and `log.warn/error(...)`. These are **living data maintained by engineers** — no one dares touch an Exception message that would break production.

**5. Test Semantics (prevent contract violations)**  
`test_rules` converts JUnit `@Test` method names into readable behavioral contracts (`approve_shouldThrow_whenWrongUser` → `"Approve should throw when wrong user"`). AI queries before modifying a Service to ensure generated code includes the required guards.

---

## 3. Architecture & Workflow

### Knowledge Extraction Pipeline (offline)

```
scripts/
├── tree-sitter-analyzer.py   # Phase 1: Java AST → modules / classes / endpoints
├── frontend-analyzer.py      # Phase 2: Vue/TS → frontend_views / fe_be_bindings
├── phase35-enhancer.py       # Phase 3.5: feature_contracts / module_exports / coupling / examples
├── phase5a-constraints.py    # Phase 5a: throw/log → code_constraints (business rules)
├── phase5b-semantics.py      # Phase 5b: @Operation + @Schema + JUnit → test_rules + JSDoc
├── generate-contract.py      # Self-growing: new Controller → update DB + emit .ts contracts
├── watch.py                  # Phase 4a: Watchdog live monitor, Ctrl+S → incremental update
└── pre-commit-sync.py        # Phase 4b: Git hook bridge, ensure DB sync before commit
```

### Knowledge Database (knowledge.db)

| Table | Rows | Description |
|---|---|---|
| `modules` | 14 | Business module descriptions |
| `classes` | 324 | Java classes (fields / methods) |
| `endpoints` | 100 | REST endpoints |
| `module_deps` | 65 | Inter-module dependencies |
| `feature_contracts` | 100 | DTO → TypeScript Interface (with JSDoc from @Schema) |
| `module_exports` | 90 | Public classes exposed by each module |
| `module_coupling` | 250 | Cross-module coupling edges |
| `module_examples` | 61 | Golden example source code |
| `code_constraints` | 231 | Business rules (extracted from throw / log) |
| `test_rules` | 1,015 | Behavioral contracts (extracted from JUnit @Test names) |
| `frontend_views` | 31 | Vue pages |
| `frontend_api_functions` | 121 | Frontend API functions |
| `fe_be_bindings` | 85 | Frontend function → backend endpoint mappings (83% coverage) |

FTS5 full-text indexes: `modules_fts`, `classes_fts`, `endpoints_fts`, `contracts_fts`, `code_constraints_fts`, `test_rules_fts`

### MCP Server (20 Tools)

```
mcp-server/server.py
```

After reloading VS Code, use `#codecortex-knowledge` in Copilot Agent chat.

| Tool | Problem Solved |
|---|---|
| `search_code_entity(keyword)` | FTS5 search across modules / endpoints / classes |
| `get_feature_contract(endpoint)` | Exact TS Interface — eliminates DTO hallucination |
| `get_module_exports(module)` | Which classes can be referenced cross-module |
| `get_module_coupling(from, to)` | Actual coupling edges — prevents architectural violations |
| `get_example_code(module, type)` | Golden example — ensures style consistency |
| `get_code_constraints(module)` | Business rule constraints — prevents rule blindness |
| `get_test_rules(class_name)` | Behavioral contracts — prevents breaking existing tests |
| `resolve_dependencies(modules[])` | Calculate transitive dependencies + migration list |
| *(12 base query tools)* | modules / classes / endpoints / frontend / routes… |

### Developer Workflow (auto-sync in real time)

```
Ctrl+S  save .java / .vue / .ts
    └→ watch.py (debounce 0.5s)
           └→ incremental update knowledge.db (< 1s)

git add + git commit
    └→ .githooks/pre-commit
           └→ pre-commit-sync.py confirms sync
           └→ knowledge.db auto-staged into commit

AI generates new Controller
    └→ ⇧⌘B (VS Code Task)
           └→ generate-contract.py
           └→ updates feature_contracts
           └→ emits frontend/src/types/generated/<module>.contracts.ts
```

### Mandatory AI Agent SOP (.github/copilot-instructions.md)

```
Step 1   search_code_entity()          confirm existing state
Step 2   get_example_code()            retrieve golden examples
Step 3   get_module_exports/coupling() confirm cross-module dependencies
Step 4   get_feature_contract()        confirm DTO contracts
Step 4.5 get_code_constraints()        query business constraints (required)
Step 4.6 get_test_rules()              query test boundaries before modifying Service (required)
Step 5   generate code
Step 6   generate-contract.py         update knowledge base after generating Controller (required)
```

### Setup

```bash
python3 -m venv venv && source venv/bin/activate
pip install tree-sitter tree-sitter-java tree-sitter-typescript mcp watchdog

python scripts/tree-sitter-analyzer.py          # Phase 1: backend
python scripts/frontend-analyzer.py             # Phase 2: frontend
python scripts/phase35-enhancer.py              # Phase 3.5: DTO contracts + module boundaries
python scripts/phase5a-constraints.py --db knowledge.db   # business constraints
python scripts/phase5b-semantics.py --db knowledge.db     # test semantics + JSDoc

python scripts/watch.py   # start live monitor (VS Code Task: Start File Watcher)
```

### Tech Stack

**Backend**: Spring Boot 3.4.1 · Java 21 · Maven · PostgreSQL · Flyway · JWT · Redis  
**Frontend**: Vue 3 · TypeScript · Pinia · Element Plus · Vite · Axios  
**Knowledge Graph**: Python 3.13 · tree-sitter · SQLite · MCP Python SDK · watchdog

---

## 4. Future Roadmap

### Short-term

**1. Phase 5b incremental watch**  
`watch.py` currently only updates `code_constraints` (Phase 5a) on Ctrl+S. Adding `_update_semantics_for_file()` would make `@Operation` / `@Schema` JSDoc sync instantly on save, without manually running `phase5b-semantics.py`.

**2. Improve @Schema coverage**  
Currently only `announcement` and `platform` modules have full `@Schema(description=...)` annotations. Completing the annotations across all modules would make JSDoc automatically appear in `.contracts.ts` files — frontend developers see field descriptions on hover.

**3. Test coverage heatmap**  
`test_rules` has 1,015 behavioral contracts but there is no dashboard showing which modules have low test density. Using `query_db()` to generate a per-module test rule count heatmap would guide where to add tests.

### Mid-term

**4. Cross-project knowledge sharing**  
`knowledge.db` is currently project-local. In a monorepo with multiple sub-projects (SmartParking, SmartBuilding…) sharing the CodeCortex template, upgrading the MCP Server to a remote HTTP server would let AI agents across different workspaces query the same knowledge base.

**5. Auto conflict detection**  
When `generate-contract.py` updates `feature_contracts`, if new DTO field names don't match existing API calls in `fe_be_bindings`, automatically flag the conflict and notify the developer before frontend and backend go out of sync.

**6. Flyway migration semantics**  
`COMMENT ON COLUMN` in SQL migration files is another source of living, engineer-maintained data. Phase 5c: parse migration SQL and inject column descriptions into `feature_contracts` JSDoc, bringing data-layer semantics into the knowledge graph.

### Long-term

**7. Multi-modal input**  
Current knowledge sources are code (Java / Vue / TS). Future extensions:
- PR descriptions → auto-extract ADRs (Architecture Decision Records)
- Swagger UI user comments → supplement API purpose descriptions
- Jira / Linear ticket titles → link to endpoints for requirement traceability

**8. Knowledge graph quality scoring**  
Build quantitative metrics (`@Schema` coverage, `test_rules` density, anomalous `module_coupling` edge count), output a quality report after each CI run, and make "knowledge graph completeness" an engineering health indicator.

---

## Module List (14 Business Modules)

| Module | Description |
|---|---|
| `auth` | Authentication — JWT, login/logout, session, OAuth/LDAP |
| `common` | Shared utilities — exception handling, multi-tenant filtering, response wrapper |
| `tenant` | Multi-tenancy — tenant management, isolation, data filtering |
| `user` | Users — CRUD, password management, soft delete |
| `dept` | Department management — tree structure, hierarchy paths |
| `rbac` | Role-based access control — roles, menus, permissions, scope filtering |
| `workflow` | Workflow engine — configurable approval flows, SLA |
| `notification` | Notification system — in-app notifications, Email/SMS |
| `audit` | Audit logs — operation auditing, login logs |
| `announcement` | Announcement system — CRUD, attachments, versioning, read receipts |
| `assettransfer` | Asset transfer — application and approval workflow |
| `platform` | Platform management — cross-tenant operations |
| `setting` | System settings — password policy |
| `config` | Spring Boot config — CORS, Swagger |

### 核心問題：AI 在大型 codebase 裡邊寫邊猜

IoT 平台模板已有 14 個業務模組、100 個 REST 端點、324 個 Java 類別。  
當 AI Agent 要基於這份模板生成新功能時，面臨三層失敗：

| 失敗模式 | 根本原因 |
|---|---|
| **DTO 欄位幻覺** | `userName` 還是 `username`？AI 不知道，只能猜 |
| **模組邊界違規** | 直接 import 其他模組的 internal class，破壞架構隔離 |
| **風格不一致** | 生成的 Service 命名、錯誤處理與現有程式碼格格不入 |
| **業務規則失盲** | 不知道「scope=DEPT 時 targetDeptIds 必填」這類隱性約束 |
| **測試合約無視** | 修改 Service 卻不知道哪些行為是被測試鎖定的，導致現有測試壞掉 |
| **Context 溢出** | 幾十個模組塞不進 context window，AI 只能讀片段 |

---

## 二、用什麼方式解決

### 核心策略：把隱含知識顯式化為可查詢的 SQLite 結構

不是讓 AI 讀更多程式碼，而是把程式碼裡的知識**萃取、結構化、索引化**，  
再透過 **MCP Server（20 個工具）** 讓 AI Agent 按需精準查詢。

```
Java / Vue / TS 原始碼
        │  tree-sitter AST 解析
        ↓
  knowledge.db (SQLite + FTS5)
        │  MCP Server
        ↓
  AI Agent 精準查詢
        │
        ↓
  生成符合現有慣例的程式碼
```

### 五個關鍵設計決策

**1. DTO 契約化（消滅欄位幻覺）**  
`feature_contracts` 把 Java `@RequestBody` / return type 自動轉成 TypeScript Interface，存入 DB 並輸出為 `frontend/src/types/generated/<module>.contracts.ts`。AI 查 `get_feature_contract()` 就拿到精確型別，不再猜。

**2. 模組邊界顯式化（防止架構違規）**  
`module_exports`（對外暴露的 public class）和 `module_coupling`（實際跨模組引用邊）兩張表，讓 AI 知道哪些 class 可以引用、哪些不行。

**3. 黃金範例注射（風格一致）**  
`module_examples` 為每個模組的每種 class type 儲存真實原始碼。AI 呼叫 `get_example_code()` 後做 few-shot prompting，直接複製命名與結構風格。

**4. 業務約束萃取（防止規則失盲）**  
`code_constraints` 從程式碼中的 `throw new XxxException("...")` 和 `log.warn/error(...)` 自動萃取隱性業務規則。這些是**有人維護的活資料**——工程師絕對不敢亂改讓程式壞掉的 Exception 訊息。

**5. 測試語意化（防止合約違反）**  
`test_rules` 把 JUnit `@Test` 方法名轉成可讀行為合約（`approve_shouldThrow_whenWrongUser` → `"Approve should throw when wrong user"`）。AI 修改 Service 前查詢，確保生成的程式碼包含對應的防呆邏輯。

---

## 三、技術架構與流程

### 知識萃取流水線（離線）

```
scripts/
├── tree-sitter-analyzer.py   # Phase 1：Java AST → modules / classes / endpoints
├── frontend-analyzer.py      # Phase 2：Vue/TS → frontend_views / fe_be_bindings
├── phase35-enhancer.py       # Phase 3.5：feature_contracts / module_exports / coupling / examples
├── phase5a-constraints.py    # Phase 5a：throw/log → code_constraints（業務約束）
├── phase5b-semantics.py      # Phase 5b：@Operation + @Schema + JUnit → test_rules + JSDoc
├── generate-contract.py      # 知識庫自我生長：新 Controller → 更新 DB + 生成 .ts 合約
├── watch.py                  # Phase 4a：Watchdog 即時監控，Ctrl+S → 增量更新
└── pre-commit-sync.py        # Phase 4b：Git hook 橋接，commit 前確保 DB 同步
```

### 知識庫（knowledge.db）

| 資料表 | 筆數 | 說明 |
|---|---|---|
| `modules` | 14 | 業務模組描述 |
| `classes` | 324 | Java 類別（fields / methods） |
| `endpoints` | 100 | REST 端點 |
| `module_deps` | 65 | 模組間依賴 |
| `feature_contracts` | 100 | DTO → TypeScript Interface（含 JSDoc from @Schema） |
| `module_exports` | 90 | 模組對外暴露的 public class |
| `module_coupling` | 250 | 跨模組耦合邊 |
| `module_examples` | 61 | 黃金範例原始碼 |
| `code_constraints` | 231 | 業務規則（萃取自 throw / log） |
| `test_rules` | 1,015 | 行為合約（萃取自 JUnit @Test 方法名） |
| `frontend_views` | 31 | Vue 頁面 |
| `frontend_api_functions` | 121 | 前端 API 函式 |
| `fe_be_bindings` | 85 | 前端函式 → 後端端點對應（83% 覆蓋率） |

FTS5 全文索引：`modules_fts`, `classes_fts`, `endpoints_fts`, `contracts_fts`, `code_constraints_fts`, `test_rules_fts`

### MCP Server（20 個工具）

```
mcp-server/server.py
```

Reload VS Code 後，在 Copilot Agent 對話中直接使用 `#codecortex-knowledge`。

| 工具 | 解決的問題 |
|---|---|
| `search_code_entity(keyword)` | FTS5 搜尋，找模組 / 端點 / class |
| `get_feature_contract(endpoint)` | 精確 TS Interface — 消滅 DTO 幻覺 |
| `get_module_exports(module)` | 哪些 class 可以跨模組引用 |
| `get_module_coupling(from, to)` | 實際耦合邊，防止架構違規 |
| `get_example_code(module, type)` | 黃金範例，風格一致 |
| `get_code_constraints(module)` | 業務規則約束，防止規則失盲 |
| `get_test_rules(class_name)` | 行為合約，防止改壞現有測試 |
| `resolve_dependencies(modules[])` | 計算 transitive 依賴 + migration 清單 |
| *(12 個基礎查詢工具)* | modules / classes / endpoints / frontend / routes… |

### 開發者工作流（即時自動同步）

```
Ctrl+S 儲存 .java / .vue / .ts
    └→ watch.py (debounce 0.5s)
           └→ 增量更新 knowledge.db（< 1s）

git add + git commit
    └→ .githooks/pre-commit
           └→ pre-commit-sync.py 確認同步
           └→ knowledge.db 自動 stage 進 commit

AI 生成新 Controller
    └→ ⇧⌘B (VS Code Task)
           └→ generate-contract.py
           └→ 更新 feature_contracts
           └→ 輸出 frontend/src/types/generated/<module>.contracts.ts
```

### AI Agent 強制 SOP（.github/copilot-instructions.md）

```
Step 1  search_code_entity()         確認現有狀態
Step 2  get_example_code()           取得黃金範例
Step 3  get_module_exports/coupling() 確認跨模組依賴
Step 4  get_feature_contract()       確認 DTO 合約
Step 4.5 get_code_constraints()      查詢業務約束（必做）
Step 4.6 get_test_rules()            查詢測試邊界（修改 Service 前必做）
Step 5  生成程式碼
Step 6  generate-contract.py        更新知識庫（生成 Controller 後必做）
```

### 建置流程

```bash
python3 -m venv venv && source venv/bin/activate
pip install tree-sitter tree-sitter-java tree-sitter-typescript mcp watchdog

python scripts/tree-sitter-analyzer.py   # Phase 1：後端
python scripts/frontend-analyzer.py      # Phase 2：前端
python scripts/phase35-enhancer.py       # Phase 3.5：DTO 契約 + 模組邊界
python scripts/phase5a-constraints.py --db knowledge.db   # 業務約束
python scripts/phase5b-semantics.py --db knowledge.db     # 測試語意 + JSDoc

python scripts/watch.py  # 啟動即時監控（VS Code Task: Start File Watcher）
```

### 技術棧

**後端**：Spring Boot 3.4.1 · Java 21 · Maven · PostgreSQL · Flyway · JWT · Redis  
**前端**：Vue 3 · TypeScript · Pinia · Element Plus · Vite · Axios  
**Knowledge Graph**：Python 3.13 · tree-sitter · SQLite · MCP Python SDK · watchdog

---

## 四、未來精進的方向

### 短期（下一個衝刺）

**1. Phase 5b 增量監控**  
目前 `watch.py` 在 Ctrl+S 後只更新 `code_constraints`（Phase 5a），尚未接入 Phase 5b。  
應在 `update_java_file()` 裡追加 `_update_semantics_for_file()`，讓 `@Operation` / `@Schema` 的 JSDoc 也能在儲存後即時同步，不需要手動跑 `phase5b-semantics.py`。

**2. @Schema 覆蓋率提升**  
目前只有 `announcement` 和 `platform` 模組的 DTO 有完整 `@Schema(description=...)` 標注（3 rows JSDoc），其他模組的 `AnnouncementRequest`、`WorkflowApproveRequest` 等只有空殼。  
補齊標注後，JSDoc 自動出現在 `.contracts.ts`，前端開發者 hover 就能看到欄位說明。

**3. 測試覆蓋率視覺化**  
`test_rules` 已有 1,015 筆行為合約，但目前沒有 dashboard 顯示哪些模組的測試密度偏低。  
可用 `query_db()` 產出各模組的測試規則數量熱圖，指導補測優先順序。

### 中期

**4. 知識庫跨專案共享**  
目前 `knowledge.db` 是 project-local。在 monorepo 架構下，多個子專案（SmartParking、SmartBuilding…）共享同一份 CodeCortex 模板，應研究將 MCP Server 改為 remote HTTP server，讓不同 workspace 的 AI Agent 查詢同一個知識庫。

**5. 自動衝突檢測**  
當 `generate-contract.py` 更新 `feature_contracts` 時，若新 DTO 欄位名稱與 FE `fe_be_bindings` 裡的既有 API 呼叫不符，應自動標記衝突並通知開發者，避免前後端不同步。

**6. Flyway migration 語意化**  
SQL migration 檔案的 `COMMENT ON COLUMN` 是另一個「有人維護的活資料」。  
Phase 5c：解析 migration SQL，把欄位說明也注入 `feature_contracts` 的 JSDoc，讓資料層的業務語意也進入知識圖譜。

### 長期

**7. 多模態輸入**  
目前知識來源是程式碼（Java / Vue / TS）。未來可擴展至：
- PR description → 自動提取 ADR（架構決策記錄）
- Swagger UI 使用者留言 → 補充 API 用途說明
- Jira / Linear ticket 標題 → 與 endpoint 建立需求溯源連結

**8. 知識圖譜品質評分**  
建立一套量化指標（`@Schema` 覆蓋率、`test_rules` 密度、`module_coupling` 異常邊數量），每次 CI 執行後輸出品質報告，把「知識圖譜完整度」納入工程健康指標。

---

## 模組清單（14 個業務模組）

| 模組 | 說明 |
|---|---|
| `auth` | 認證授權 — JWT、登入/登出、Session、OAuth/LDAP |
| `common` | 共用工具 — 異常處理、多租戶過濾、Response 封裝 |
| `tenant` | 多租戶 — 租戶管理、隔離、資料過濾 |
| `user` | 使用者 — CRUD、密碼管理、軟刪除 |
| `dept` | 部門管理 — 樹狀結構、階層路徑 |
| `rbac` | 角色權限 — 角色、選單、權限、Scope 過濾 |
| `workflow` | 工作流程 — 可配置引擎、審批流程、SLA |
| `notification` | 通知系統 — 站內通知、Email/SMS |
| `audit` | 稽核日誌 — 操作審計、登入日誌 |
| `announcement` | 公告系統 — CRUD、附件、版本、已讀回執 |
| `assettransfer` | 資產移轉 — 申請與審核流程 |
| `platform` | 平台管理 — 跨租戶操作 |
| `setting` | 系統設定 — 密碼策略 |
| `config` | Spring Boot 配置 — CORS、Swagger |
