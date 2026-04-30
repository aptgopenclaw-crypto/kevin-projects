# 臺北市路燈管理平台

> 路燈資產全生命週期管理系統 — 涵蓋設備資產、報修派工、材料管理、簽核流程等模組。

## 技術棧

| 層級 | 技術 |
|------|------|
| **Backend** | Java 21 · Spring Boot 3.4 · Spring Security · JPA/Hibernate · Flyway |
| **Frontend** | Vue 3 · TypeScript · Element Plus · Pinia · vue-i18n · ECharts |
| **Database** | PostgreSQL 15+ (schema: `taipei_streetlight`) |
| **Auth** | JWT + CAPTCHA · RBAC + DataScope 多租戶 |
| **Build** | Maven · Vite |

## 專案結構

```
taipei-streetlight/
├── project/
│   ├── backend/              # Spring Boot 後端
│   │   └── src/main/
│   │       ├── java/com/taipei/iot/
│   │       │   ├── auth/         # 登入驗證 (JWT + CAPTCHA)
│   │       │   ├── rbac/         # 角色權限 (RBAC + DataScope)
│   │       │   ├── user/         # 用戶管理
│   │       │   ├── dept/         # 部門管理 (樹狀結構)
│   │       │   ├── audit/        # 操作稽核 (AOP 非同步寫入)
│   │       │   ├── announcement/ # 系統公告
│   │       │   ├── setting/      # 系統設定
│   │       │   ├── notification/ # 通知提示
│   │       │   ├── tenant/       # 多租戶 (Discriminator + Filter)
│   │       │   ├── device/       # 設備資產 (迴路/設備)
│   │       │   ├── fault/        # 障礙工單 + 故障關聯
│   │       │   ├── workflow/     # 簽核流程 + 代理人
│   │       │   ├── repair/       # 報修派工 + 巡查
│   │       │   ├── replacement/  # 換裝維護 + 燈桿號 + 自主檢查
│   │       │   ├── material/     # 材料管理 (採購/庫存/領用/報廢)
│   │       │   └── config/       # Security/CORS/Flyway 配置
│   │       └── resources/
│   │           └── db/migration/ # Flyway 遷移腳本 (V1–V48)
│   └── frontend/             # Vue 3 前端
│       └── src/
│           ├── views/            # 頁面 (admin/device/repair/…)
│           ├── stores/           # Pinia stores
│           ├── api/              # Axios API 模組
│           ├── types/            # TypeScript 型別
│           ├── composables/      # 組合式函式 (idle timeout 等)
│           ├── locales/          # i18n (zh-TW / zh-CN / en)
│           └── components/       # 共用元件
├── 01-requirement/           # 原始需求文件
├── 02-spec/                  # 功能規格文件 (§1–§11)
├── 03-srs/                   # 軟體需求規格 (SRS-01–SRS-11 + NFR)
├── 04-wbs/                   # 工作分解結構 (Phase 1–5)
├── 05-sa/                    # 系統分析 Function List (SA)
├── 06-sd/                    # 系統設計 (SD)
├── 07-traceability/          # FN 需求追溯矩陣
├── 08-verification/          # 驗證報告 (VR-P1–P4)
├── 09-test-spec/             # 測試規格 (TS)
├── 99-adr/                   # 架構決策紀錄 (ADR) + 知識庫 (KB)
├── 99-plan/                  # 專案執行計畫
├── 99-risk/                  # 風險揭露
└── _archive/                 # 歷史開發計劃 (已歸檔)
```

## 已完成模組

| 階段 | 模組 | 後端 | 前端 | 狀態 |
|------|------|:----:|:----:|:----:|
| Phase 1 | 帳號管理 (CRUD + 角色指派) | ✅ | ✅ | Done |
| Phase 1 | 角色權限 (RBAC + DataScope) | ✅ | ✅ | Done |
| Phase 1 | 登入管理 (JWT + CAPTCHA) | ✅ | ✅ | Done |
| Phase 1 | 部門管理 (樹狀結構) | ✅ | ✅ | Done |
| Phase 1 | 選單管理 (動態選單 + 權限綁定) | ✅ | ✅ | Done |
| Phase 1 | 操作稽核 (AOP + 非同步寫入) | ✅ | ✅ | Done |
| Phase 1 | 系統公告 | ✅ | ✅ | Done |
| Phase 1 | 系統設定 (閒置登出 + 密碼策略) | ✅ | ✅ | Done |
| Phase 1 | 通知提示 (Polling 60s) | ✅ | ✅ | Done |
| Phase 1 | 設備/迴路/障礙工單 | ✅ | ✅ | Done |
| Phase 1 | 契約管理 | ✅ | ✅ | Done |
| Phase 1 | 簽核流程 + 代理人 | ✅ | ✅ | Done |
| Phase 2 | 報修工單 + 派工 | ✅ | ✅ | Done |
| Phase 2 | 巡查管理 | ✅ | ✅ | Done |
| Phase 3 | 材料規格/庫存/採購/領用 | ✅ | ✅ | Done |
| Phase 3 | 調整/報廢/合格材料/庫別/廠商 | ✅ | ✅ | Done |
| Phase 4 | 換裝工單/燈桿號/自主檢查 | ✅ | ✅ | Done |

## 快速開始

### 環境需求

- Java 21+
- Node.js 18+
- PostgreSQL 15+
- Maven 3.9+

### 資料庫

```bash
# 建立資料庫 (Flyway 會自動建立 schema 與表)
createdb -U postgres mydb
```

### 後端

```bash
cd project/backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 預設 http://localhost:8080
```

### 前端

```bash
cd project/frontend
npm install
npm run dev
# 預設 http://localhost:5173
```

### 測試

```bash
# 後端單元測試
cd project/backend && mvn test

# 前端型別檢查 + 建置
cd project/frontend && npm run build
```

## 預設帳號

| 帳號 | 密碼 | 角色 |
|------|------|------|
| `admin` | `Admin123!` | SUPER_ADMIN |

> 詳見 `project/SECRET.md`

## API 文件

啟動後端後訪問：`http://localhost:8080/swagger-ui.html`

## 開發計劃

| 階段 | 內容 | 狀態 |
|------|------|:----:|
| Phase 1 | 基礎架構 + 系統管理 + 資產管理 + 簽核 | ✅ |
| Phase 2 | 報修派工 + 巡查管理 | ✅ |
| Phase 3 | 材料管理 | ✅ |
| Phase 4 | 換裝維護管理 | ✅ |
| Phase 5 | 跨模組整合 + GIS 地圖 + 公開報修 | 🔲 |
| Phase 6 | 智能路燈 (IoT) + 績效管理 (KPI) | 🔲 |
| Phase 7 | 行動 APP + 儀表板 (Dashboard) | 🔲 |

詳見 `05-sa/` 系統分析、`99-plan/` 執行計畫、`99-risk/` 風險揭露。

## 文件索引

| 目錄 | 內容 | 檔案數 |
|------|------|-------:|
| `01-requirement/` | 原始需求文件 | 3 |
| `02-spec/` | 功能規格 (§1–§11) | 12 |
| `03-srs/` | 軟體需求規格 (SRS) | 13 |
| `04-wbs/` | 工作分解結構 (WBS) | 6 |
| `05-sa/` | 系統分析 Function List | 12 |
| `06-sd/` | 系統設計 | 12 |
| `07-traceability/` | FN 需求追溯矩陣 | 1 |
| `08-verification/` | 驗證報告 (Phase 1–4) | 5 |
| `09-test-spec/` | 測試規格 | 11 |
| `99-adr/` | 架構決策紀錄 + 知識庫 | 3 |
| `99-plan/` | 專案執行計畫 | 1 |
| `99-risk/` | 需求覆蓋缺口風險揭露 | 1 |
