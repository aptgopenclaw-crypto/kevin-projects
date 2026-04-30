# ADR-003: GIS 技術選型 — 採用開源方案

> **狀態**: Accepted
> **日期**: 2026-04-24
> **決策者**: Kevin
> **關聯需求**: §4.1 地理資訊、§5-(10) 報告繪圖、§8-(2) 即時狀態地圖、§10 APP 地圖、§11-(2)-C 故障熱區

---

## 背景

需求原文 §4.1-A 寫道：「精確圖資採用 ArcGIS 圖資平台建置及更新，但行動運用瀏覽則轉置於開放式圖台（Google Map、Bing Map 或 Open Layer）」。

ArcGIS 授權費用高昂，公司不會考慮投入額外授權成本。需評估開源方案是否能滿足同等需求。

## 決策

**全面採用開源 GIS 技術棧，不採購 ArcGIS 或其他商用 GIS 授權。**

## 商用方案成本（排除原因）

| 產品 | 估算年費 | 說明 |
|------|---------|------|
| ArcGIS Online | NT$150K+/年 | 訂閱制，人頭計費 |
| ArcGIS Enterprise | NT$500K+/年 | 需自建 Server |
| ArcGIS JS SDK | 免費但需 Online 帳號 | 功能受限於訂閱等級 |
| ArcGIS Runtime (APP) | NT$300K+/年 | 行動端 SDK |
| Google Maps Platform | ~NT$50K+/年 | 超過免費額度後按量計費 |

**結論：公司不會考慮花這筆錢，因此商用方案全數排除。**

## 開源技術棧

```
┌─────────────────────────────────────────────┐
│                  前端地圖                      │
│  OpenLayers 9 (BSD-2-Clause)                 │
│  - WebGLPointsLayer: 16萬點 GPU 渲染          │
│  - Heatmap Layer: 故障熱區                    │
│  - WMTS/WMS 底圖介接                         │
│  - TWD97 (EPSG:3826) 投影原生支援             │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│                  底圖來源                      │
│  NLSC 國土測繪中心 WMTS (免費申請)              │
│  - 千分之一地形圖 ✅                           │
│  - 正射影像 ✅                                │
│  - 段籍圖 ✅                                  │
│  - 門牌地址定位 API ✅                         │
│  OpenStreetMap (免費) — 通用底圖               │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│              坐標轉換                          │
│  Proj4js (MIT) — 前端                        │
│  PostGIS ST_Transform — 後端                  │
│  - TWD97 (EPSG:3826) ↔ WGS84 (EPSG:4326)   │
│  - TWD67 TM2 (EPSG:3828) ↔ WGS84           │
│  - 台電電力坐標 = TWD67 TM2 編碼              │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│              空間資料庫                         │
│  PostGIS 3.4 (GPL) — PostgreSQL 擴充          │
│  - GiST 空間索引                              │
│  - ST_Distance: 距離查詢 (50m 電子圍籬)        │
│  - ST_Contains: 分區範圍判斷                   │
│  - POINTZ / LINESTRINGZ: 三維坐標             │
│  - 管線回路綁定: 空間拓撲查詢                   │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│              匯出服務                          │
│  GML: 自研 XML template (JAXB)               │
│  DXF: 自研或 QGIS Server API (GPL)           │
│  GeoJSON: PostGIS ST_AsGeoJSON 原生           │
│  PDF 圖面: OpenLayers renderSync → PDF        │
└─────────────────────────────────────────────┘
```

## 需求逐項對應

| 需求 | 條文 | 開源方案 | 可行性 |
|------|------|---------|:---:|
| 路燈地圖（千分之一） | §4.1-A | OpenLayers + NLSC WMTS | ✅ |
| 16 萬點單畫面顯示 | §4.1-A | OpenLayers WebGLPointsLayer | ✅ |
| 坐標轉換 TWD97↔GPS↔TWD67 | §4.1-B | Proj4js (前端) + PostGIS (後端) | ✅ |
| 街景連結 | §4.1-C | Google Maps Embed (免費額度) / NLSC 街景 | ✅ |
| 圖例符號向量圖元 | §4.1-D(A) | OpenLayers Style + SVG 圖元 | ✅ |
| 千分之一地形圖底圖 | §4.1-D(B) | NLSC WMTS TWD97 底圖 | ✅ |
| 分區範圍管理 | §4.1-D(C) | PostGIS polygon + OpenLayers VectorLayer | ✅ |
| 三維坐標（含架空管線） | §4.1-E(B) | PostGIS POINTZ / LINESTRINGZ | ✅ |
| 平面/高架/隧道差異化 | §4.1-E(C) | Z 值 + 圖層分層顯示 | ✅ |
| 匯入比對確認 | §4.1-F(A) | 自研比對邏輯 | ✅ |
| GML 匯出 | §4.1-F(B) | JAXB XML template | ✅ |
| 臺北市資料大平臺匯出 | §4.1-F(C) | 自研 CSV/JSON 格式轉換 | ✅ |
| 工務局管線匯入 | §4.1-F(D) | 自研 parser（需確認格式） | ✅ |
| 向量圖面匯出 (1/1000, A3) | §4.1-F(E) | OpenLayers SVG export → PDF | ⚠️ |
| 施工範圍整合 | §4.1-F(F) | PostGIS polygon overlay | ✅ |
| 圖形化搜尋 | §4.3-G | OpenLayers + PostGIS 空間查詢 | ✅ |
| 報告單繪圖 | §5-(10) | ⚠️ 見下方替代方案 | ⚠️ |
| 即時狀態地圖 | §8-(2) | OpenLayers + WebSocket 推送 | ✅ |
| 故障熱區 | §11-(2)-C | OpenLayers Heatmap layer | ✅ |
| APP 地圖 | §10 | flutter_map (Leaflet binding) | ✅ |
| 50m 電子圍籬 | §10-(4) | PostGIS ST_DWithin / 前端 GPS 距離計算 | ✅ |

## 需注意的項目

### 1. §4.1-F(E) 向量圖面輸出 (1/1000, A3)

需求要求「匯出指定區域之圖資資料（含坐標向量）至電腦繪圖軟體使用，需可設定輸出圖資比例尺及圖面尺寸」。

**方案**: OpenLayers `map.renderSync()` 輸出 SVG/Canvas → 搭配 jsPDF 產生指定尺寸 PDF，或透過 QGIS Server WMS GetMap API 指定 BBOX + 尺寸產出。精度可達 1/1000，但不如 AutoCAD 原生 DWG。

**風險**: 中。若機關要求 DWG/DXF 格式，需額外整合 QGIS Server 的 DXF 匯出功能。

### 2. §5-(10) 報告單/查報單/設計單繪圖

需求原文：「需具備繪圖編輯功能（如呼叫 ArcGIS 或 AutoCAD 物件）」。

**替代方案**:
1. **PDF 報告 + 靜態地圖截圖** — OpenLayers 截圖嵌入 PDF，加上點位標註（滿足 80% 場景）
2. **SVG 線上編輯** — 嵌入輕量 SVG 編輯器，讓使用者在地圖截圖上加標記、量測
3. **QGIS Server** — WMS GetMap + Print Layout API 產生排版圖面

**建議**: 與機關確認實際使用場景。「呼叫 ArcGIS 或 AutoCAD 物件」是需求描述中的舉例（用「如」開頭），並非強制指定特定產品。PDF + 地圖截圖 + 標記大機率可被接受。

## 授權合規

| 元件 | 授權 | 商用限制 |
|------|------|---------|
| OpenLayers | BSD-2-Clause | 無限制 |
| Proj4js | MIT | 無限制 |
| PostGIS | GPL-2.0 | 伺服器端使用，不影響專案程式碼授權 |
| OpenStreetMap 底圖 | ODbL | 需標註來源 |
| NLSC WMTS | 免費申請 | 需遵守國土測繪中心使用規範 |
| flutter_map | BSD-3-Clause | 無限制 |
| QGIS Server (選用) | GPL-2.0 | 獨立服務，不影響專案程式碼授權 |

所有元件均可合法商用，無授權費用。

## 後果

### 正面
- 零授權費，無持續訂閱成本
- 技術棧完全自主可控
- PostGIS 已在現有 PostgreSQL 上，加擴充即可，零遷移成本
- 社群活躍，文件豐富

### 負面
- §5-(10) 繪圖編輯功能需要自研或替代方案，需與機關溝通
- 向量圖面輸出精度可能不如 AutoCAD 原生
- 團隊需學習 OpenLayers API（學習曲線約 1–2 週）
- NLSC WMTS 偶有服務不穩定的風險，需考慮快取/降級策略
