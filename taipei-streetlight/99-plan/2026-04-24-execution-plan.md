# 專案執行計畫 — Phase 5+ 修正方案

> **建立日期**: 2026-04-24
> **依據**: `risk/2026-04-24-requirements-gap-risk.md` 風險揭露分析
> **範圍**: Phase 5–10 重新規劃

---

## 一、現況盤點

| 指標 | 已完成 | 待開發 |
|------|--------|--------|
| 開發階段 | Phase 1–4 ✅ | Phase 5–10 ❌ |
| DB Tables | 39 張 (V1–V48) | ~18 張 |
| SA Functions | ~217 個 (§1–§7) | ~183 個 (§8–§11 + 整合) |
| 後端測試 | 506+ passing | — |
| 文件覆蓋 | SRS/SA/SD/TS 全 11 模組 | 實作計畫待建立 |

---

## 二、修正後的階段規劃

原本 Phase 5 混合了太多內容，且 §8/§9/§10/§11 的依賴關係未被正確排序。修正如下：

### 總覽

```
Phase 5A  跨模組整合驗證            ← 立即開始
Phase 5B  基礎缺口補齊              ← 與 5A 並行
Phase 5C  GIS 地圖基礎建設          ← 與 5A 並行（選型 → PoC → 基礎圖層）
Phase 6   績效管理 §9               ← 5A 完成後立即接
Phase 7   智能路燈 §8               ← 5C + 外部串接就緒後
Phase 8   儀表板 §11                ← 6 + 7 的數據源就緒後
Phase 9   行動 APP §10              ← 技術選型確定後（可與 7/8 並行）
Phase 10  NFR 效能/資安合規          ← 功能凍結後
```

### 依賴關係圖

```
Phase 1–4 (done)
    │
    ├── Phase 5A 跨模組整合 ──┬── Phase 6 績效 §9 ──┐
    │                        │                      │
    ├── Phase 5B 基礎缺口 ───┘                      ├── Phase 8 儀表板 §11 ── Phase 10 NFR
    │                                               │
    └── Phase 5C GIS 基礎 ── Phase 7 IoT §8 ───────┘
                                    │
                             Phase 9 APP §10 ─────────┘

    Track X: 外部 API 申請（臺北通/1999/IoT 廠商）← 立即啟動，平行等待
```

---

## 三、各階段詳細內容

### Phase 5A — 跨模組整合驗證

**目標**: 確保 Phase 1–4 的 13 個事件（E1–E13）端到端正確運作

| 工作項 | 說明 |
|--------|------|
| FK 補齊 | 跨模組外鍵約束 (repair→device, replacement→material 等) |
| E1–E13 驗證 | 每個事件走完整 happy path + exception path |
| 前端路由統一 | 待辦清單 → 點擊直達各模組詳情頁 |
| 整合測試 | fault→repair→replacement→material→close 全流程 |

### Phase 5B — 基礎缺口補齊（可與 5A 並行）

| 功能 | 需求出處 | 複雜度 | 說明 |
|------|---------|--------|------|
| 密碼重設 | §2-(5) | 低 | Email token + 一次性連結 |
| 待辦通知強化 | §2-(10) | 低 | 待辦件數 badge + 點擊直達 |
| QR Code 產生 | §6-(1) | 低 | 燈桿號碼牌 + 報修連結 QR Code |
| 公開報修網頁 | §5-(1) | 中 | 匿名/實名制報修 + 附件上傳 + 進度查詢 |
| ClamAV 整合 | §5-(3) | 中 | 上傳附件病毒掃描 |
| 動態欄位 UI | §1-(5) | 中 | JSONB attributes 的前端編輯介面 |

### Phase 5C — GIS 地圖基礎建設

| 工作項 | 技術選擇 | 說明 |
|--------|---------|------|
| 空間資料庫 | **PostGIS** (GPL) | PostgreSQL 擴充，ALTER EXTENSION |
| 前端地圖引擎 | **OpenLayers** (BSD) | WebGL 支援，TWD97 投影原生支援 |
| 底圖來源 | **NLSC 國土測繪 WMTS** (免費) | 千分之一地形圖、正射影像、段籍圖 |
| 街景 | **Google Maps Embed** / NLSC 街景 | 連結即可，不需 SDK |
| 坐標轉換 | **Proj4js** (MIT) | TWD97(EPSG:3826) ↔ WGS84 ↔ TWD67 |
| GML 匯出 | 自研 XML template | GML 本質是 XML schema |
| 空間索引 | PostGIS GiST index | 距離查詢、polygon 包含 |
| 大量點位 | OpenLayers **WebGLPointsLayer** | 16 萬點 GPU 渲染 |

### Phase 6 — 績效管理 §9

**提前理由**: 績效直接影響契約扣款金額，機關最在意；技術上純內部計算，不需外部系統。

| 工作項 | 說明 |
|--------|------|
| DB | kpi_definitions, kpi_periods, kpi_scores, kpi_data_imports (4 tables) |
| 公式引擎 | SpEL 為主 / GraalJS sandbox 為輔 |
| 資料收集 | 自動從 repair/replacement/material/IoT 模組拉取 |
| 鎖定機制 | GOV_MGR 鎖定當期，GOV_CHIEF 可解鎖 |
| 報表匯出 | 月報/年報/跨廠商比較，ODS/XLS/CSV |
| 前端 | 6 頁面：指標管理、資料匯入、計分結果、鎖定、報表、廠商自助 |

### Phase 7 — 智能路燈 §8

| 工作項 | 說明 |
|--------|------|
| IoT Gateway | MQTT broker (Mosquitto/EMQX) + REST adapter |
| DB | telemetry, dimming_schedules, alert_rules, alert_events, connection_logs (5+ tables) |
| 即時狀態地圖 | OpenLayers + WebSocket 推送設備狀態變色 |
| 告警引擎 | 規則設定 → 自動故障判斷 → 建立 fault_ticket |
| 調光控制 | 單燈/群組/排程，Fail-Safe 設計 |
| 智慧電表 | 跨設備關聯分析（停電/跳脫） |

### Phase 8 — 儀表板 §11

| 工作項 | 說明 |
|--------|------|
| 前端框架 | vue-grid-layout + ECharts |
| 10 類 widget | 維護情形、停電、故障熱區、績效、路燈數、開關箱、附掛物、電費、智慧電表、GIS 疊圖 |
| 使用者自訂 | 版面配置存 DB，per-user 個人化 |
| 即時推送 | WebSocket 關鍵指標更新 |

### Phase 9 — 行動 APP §10

| 工作項 | 說明 |
|--------|------|
| 技術 | **Flutter** (建議，見下方分析) |
| 資產普查 | QR scan → 編輯 → GPS 照片上傳 |
| 巡查 | 路線打卡 (≤50m GPS)、檢查表、異常→自動建單 |
| 照片管控 | EXIF 驗證、50m 近距離檢查、浮水印 |
| 離線 | SQLite 本地暫存、斷網續傳（chunked upload ≤2MB） |

### Phase 10 — NFR 效能/資安合規

| 工作項 | 說明 |
|--------|------|
| 壓力測試 | 100 user 並發，P95 ≤ 5s (JMeter/Gatling) |
| 資安合規 | 附表十「普級」逐條自評 |
| 滲透測試 | OWASP ZAP / Burp Suite |
| 瀏覽器相容 | Chrome/Edge/Firefox/Safari |
| 備份/DR | PostgreSQL streaming replication + WAL archiving |

---

## 四、GIS 技術選型 — 開源方案

### 為什麼不用 ArcGIS

| 項目 | ArcGIS | 問題 |
|------|--------|------|
| ArcGIS Online | NT$150K+/年 | 訂閱制，持續成本 |
| ArcGIS Enterprise | NT$500K+/年 | 需自建 Server |
| ArcGIS JS SDK | 免費但需 Online 帳號 | 功能受限於訂閱等級 |
| ArcGIS Runtime (APP) | NT$300K+/年 | 行動端最貴的部分 |

需求原文寫「精確圖資採用 ArcGIS」是建議方案，非強制。以下開源方案可滿足同等需求：

### 開源 GIS 技術棧（零授權費）

```
┌─────────────────────────────────────────────┐
│                  前端地圖                      │
│  OpenLayers (BSD) + Proj4js (MIT)            │
│  - WebGLPointsLayer: 16萬點 GPU 渲染          │
│  - Heatmap Layer: 故障熱區                    │
│  - WMTS/WMS 底圖介接                         │
│  - TWD97 (EPSG:3826) 投影原生支援             │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│                  底圖來源                      │
│  NLSC 國土測繪 WMTS (免費)                     │
│  - 千分之一地形圖 ✅                           │
│  - 正射影像 ✅                                │
│  - 段籍圖 ✅                                  │
│  - 門牌地址定位 API ✅                         │
│  OpenStreetMap (免費) — 通用底圖               │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│                空間資料庫                       │
│  PostGIS (GPL) — PostgreSQL 擴充              │
│  - GiST 空間索引                              │
│  - ST_Distance: 距離查詢 (50m 電子圍籬)        │
│  - ST_Contains: 分區範圍判斷                   │
│  - ST_Transform: 伺服器端坐標轉換              │
│  - 管線、回路三維坐標 (POINTZ/LINESTRINGZ)     │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│              圖資服務 (選用)                    │
│  GeoServer (GPL)                              │
│  - WMS/WFS/WMTS 標準協定                      │
│  - GML 匯出 ✅                                │
│  - SHP/DXF/GeoJSON 格式轉換                   │
│  - 或自研 Spring Boot REST + GeoJSON           │
└─────────────────────────────────────────────┘
```

### 需求對應能力

| 需求 | 開源方案 | 能否取代 ArcGIS |
|------|---------|:---:|
| §4.1-A 路燈地圖 (千分之一) | OpenLayers + NLSC WMTS | ✅ |
| §4.1-B 坐標轉換 TWD97↔GPS↔TWD67 | Proj4js + PostGIS ST_Transform | ✅ |
| §4.1-C 街景 | Google Maps Embed (免費額度) / NLSC | ✅ |
| §4.1-D 基本圖資 (圖例/符號) | OpenLayers Style + SVG | ✅ |
| §4.1-E 三維坐標 | PostGIS POINTZ/LINESTRINGZ | ✅ |
| §4.1-F(B) GML 匯出 | GeoServer 或自研 XML template | ✅ |
| §4.1-F(E) 向量圖面輸出 (1/1000, A3) | QGIS Server API 或 SVG→PDF | ⚠️ 可行但需開發 |
| §4.3-G 圖形化搜尋 | OpenLayers + PostGIS 空間查詢 | ✅ |
| §5-(10) 繪圖編輯 (ArcGIS/AutoCAD) | ⚠️ 需與機關討論替代方案 | ❌ 需確認 |
| §8-(2) 即時狀態地圖 | OpenLayers + WebSocket | ✅ |
| §10 APP 地圖 | flutter_map (Leaflet binding) | ✅ |
| §11-(2)-C 故障熱區 | OpenLayers Heatmap | ✅ |

### §5-(10) 繪圖功能替代方案

這是唯一可能需要 ArcGIS/AutoCAD 的場景。替代思路：

1. **PDF 報告 + 靜態地圖截圖** — 用 OpenLayers `map.renderSync()` 截圖嵌入 PDF，加上點位標註
2. **SVG 編輯器** — 嵌入輕量 SVG 編輯器（如 SVG-Edit），讓使用者在地圖截圖上加標記
3. **QGIS Server** — 透過 WMS GetMap API 產生圖面，支援 DXF 匯出

**建議**: 先與機關確認§5-(10) 的實際使用場景，很可能「PDF 報告 + 地圖截圖 + 標記」就夠用。

---

## 五、需要現在就啟動的外部事項

| # | 事項 | 負責方 | 預估時間 | 說明 |
|---|------|--------|---------|------|
| 1 | 臺北通 OAuth API 申請 | 機關協助申請 | 2–4 週 | 需向臺北市資訊局提出 |
| 2 | Taipeion 驗證 API 規格 | 機關提供 | 1–2 週 | 確認 OAuth2/OIDC/SAML |
| 3 | 1999 陳情系統 API | 機關協助申請 | 2–4 週 | 確認介接格式 |
| 4 | IoT 廠商介面規格 | 各區得標廠商 | 4–8 週 | 最大不確定性，需盡早 |
| 5 | NLSC 圖資 WMTS 申請 | 廠商自行申請 | 1 週 | 國土測繪中心線上申請 |
| 6 | 臺北市資料大平臺格式 | 機關提供 | 1 週 | 確認「路燈位置」「維修資料」匯出格式 |
| 7 | 工務局管線資料庫格式 | 機關協助 | 2 週 | 道管中心圖資下載格式 |

---

## 六、技術選型決策摘要

| # | 決策項目 | 建議方案 | 理由 |
|---|---------|---------|------|
| 1 | **GIS 前端** | OpenLayers 9 (BSD) | TWD97 投影原生支援、WebGL 16 萬點、開源免費 |
| 2 | **空間 DB** | PostGIS 3.4 (GPL) | 已用 PostgreSQL，加擴充即可，零遷移成本 |
| 3 | **底圖** | NLSC WMTS + OSM | 免費、含千分之一地形圖 |
| 4 | **坐標轉換** | Proj4js (前端) + PostGIS (後端) | TWD97/WGS84/TWD67 全支援 |
| 5 | **行動 APP** | Flutter | 跨平台、離線 SQLite 原生、GPS/Camera 成熟 |
| 6 | **IoT Broker** | EMQX (開源版) 或 Mosquitto | 待確認 IoT 廠商協定後最終決定 |
| 7 | **GML 匯出** | 自研 XML template | GML 是固定 schema，不需要 GeoServer |
| 8 | **KPI 公式** | SpEL (Spring 內建) | 已在技術棧內，零額外依賴 |

---

## 七、最大風險與緩解

| 風險 | 影響 | 緩解措施 |
|------|------|---------|
| GIS 是橫向依賴，延遲影響 §5/§8/§10/§11 | 🔴 高 | Phase 5C 立即啟動 PoC |
| IoT 廠商介面規格未知 | 🔴 高 | 先定義 Adapter 介面，廠商就緒後補實作 |
| §5-(10) 繪圖需求可能被低估 | 🟠 中 | 與機關確認是否接受 PDF+截圖替代 |
| 行動 APP 離線續傳複雜度 | 🟠 中 | Flutter + SQLite + chunked upload 已有成熟方案 |
| 100 人 5 秒效能驗收 | 🟡 低 | Phase 10 集中壓測，但需提前在各模組做基本效能考量 |
