# ADR-002: AI 影像辨識引擎策略

> **日期**：2026-04-24  
> **狀態**：Accepted  
> **決策者**：架構團隊  
> **關聯需求**：§1.(4)「AI 影像辨識引擎」、§5.(4) 維修照片 GPS、§10.(4) APP 拍照 EXIF 比對

---

## 背景

需求原文：

> 得標廠商需提供機關可合法使用之 AI 影像辨識引擎，可依據機關指定之條件（如拍照地點或相似性）將照片或含影像之檔案自動解析分類，並找出相似或重複之照片及影像。

另有關聯需求：
- §5.(4)：維修前、中、後照片需記錄日期、時間、GPS 位置
- §10.(4)：APP 拍照需定位於資產周邊 **50 公尺**內，非 APP 拍攝照片需比對 **EXIF** 資訊確認未經修改

---

## 需求拆解

| # | 能力 | 需求關鍵字 | 本質技術 | 是否需要 AI |
|---|------|-----------|---------|-----------|
| A | 依地點自動分類 | 「依拍照地點將照片自動解析分類」 | GPS 空間查詢 + 反向地理編碼 | ❌ 不需要 |
| B | 找出重複照片 | 「找出重複之照片」 | 感知雜湊（Perceptual Hash） | ❌ 演算法 |
| C | 找出相似照片 | 「找出相似之照片及影像」 | 圖片向量嵌入 + 向量搜尋 | ✅ 需要 |
| D | EXIF 完整性比對 | §10.(4)「確認為原始未經修改」 | EXIF 欄位擷取 + 交叉驗證 | ❌ 不需要 |

---

## 決策

### A. 地點分類 — GPS 空間查詢（Phase 1）

完全不涉及影像辨識，用 EXIF GPS 就能做。

```
照片 EXIF GPS (25.041, 121.529)
    ↓
Haversine 距離匹配系統內路燈/分電箱座標
    ↓
自動歸類：區域=中正區、路段=重慶南路、關聯設備=#A12345
```

**實作要點**：
- 上傳時用 `metadata-extractor` 擷取 GPS（詳見 KB-001）
- 與 `devices` 表座標做距離匹配，關聯到最近資產
- 搭配行政區多邊形（PostGIS `ST_Contains`）做區域歸類
- 無 GPS 的照片標記 `noGps=true`，不做地點分類

---

### B. 重複照片偵測 — 感知雜湊 pHash（Phase 1）

**場景**：施工人員重複上傳同張照片、或稍微裁切/壓縮後再傳。

**原理**：把圖片壓縮成 64-bit 指紋，兩張圖的 Hamming 距離 ≤ 10 視為重複。

```
原始照片                    裁切後的同張照片
    ↓                              ↓
縮小 32×32 → DCT → 取低頻       同樣流程
    ↓                              ↓
  1011001010...64bit             1011001011...64bit
              ↓
        Hamming Distance = 1  →  判定為重複
```

**特性**：
- 抗縮放、抗輕微裁切、抗 JPEG 壓縮品質變化
- 純 CPU 計算，一張圖 < 5ms，**不需要 GPU**
- JDK `ImageIO` 可實作，或用 `JImageHash` 函式庫

**DB 設計**：
```sql
ALTER TABLE photo_files ADD COLUMN phash BIT(64);
CREATE INDEX idx_photo_phash ON photo_files (phash);

-- 查詢重複照片（PostgreSQL bit_count + XOR）
SELECT * FROM photo_files
WHERE bit_count(phash # :inputHash) <= 10;
```

**上傳流程整合**：
```
上傳 → ImageIO.read() → 計算 pHash → 查 DB 是否有 distance ≤ 10
    → 有：標記 duplicateSuspect=true + 關聯原始 photoId
    → 無：正常存入
```

---

### C. 相似照片搜尋 — CLIP + pgvector（Phase 2）

**場景**：
- 找出所有「燈桿傾斜」的照片
- 某路口不同時間拍的施工前後對比
- 搜尋「跟這張差不多的」

這是**真正的 AI 影像辨識**。

#### 技術選型

| 方案 | 模型 | 部署方式 | 授權 | 評估 |
|------|------|---------|------|------|
| **✅ CLIP + ONNX Runtime** | OpenAI CLIP (ViT-B/32) | JVM 內嵌 JAR | **MIT** | 推薦：合法、自架、無雲端依賴 |
| Python sidecar | sentence-transformers/clip | 獨立 Python 服務 | Apache 2.0 | 可行，但增加維運複雜度 |
| 雲端 API | Azure/Google Vision | SaaS | 付費 | 政府標案可能不允許資料上雲 |

**選 CLIP 的理由**：
1. **MIT 授權** → 滿足需求「可合法使用之 AI 影像辨識引擎」
2. **可文字搜圖片**：輸入「燈桿傾斜」→ 找出視覺上相似的照片
3. **ONNX 格式**可在 JVM 直接跑（`onnxruntime` Java binding），不需 Python runtime
4. **模型檔 ~350MB**，嵌入推論一張圖 ~50ms (CPU)
5. **自架部署**，照片不離開機關網路

#### 架構

```
                    ┌──────────────────┐
   照片上傳 ───────→│ CLIP 模型 (ONNX) │──→ 512-dim float[] embedding
                    │ JVM 內執行        │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ PostgreSQL       │
                    │ + pgvector 擴充  │
                    │                  │
                    │ photo_files 表    │
                    │ embedding vector(512) │
                    └──────────────────┘
                              │
                              ▼
        -- 以圖搜圖（cosine distance 最近 20 張）
        SELECT * FROM photo_files
        ORDER BY embedding <=> :queryEmbedding
        LIMIT 20;

        -- 以文字搜圖（先將文字轉 embedding 再搜）
        -- 輸入「燈桿傾斜」→ CLIP text encoder → embedding → 同上查詢
```

**pgvector 部署**：
```sql
CREATE EXTENSION vector;
ALTER TABLE photo_files ADD COLUMN embedding vector(512);
CREATE INDEX idx_photo_embedding ON photo_files
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
```

#### 為什麼 pgvector 足夠（不需要獨立向量資料庫）

**量級估算**：

| 參數 | 估算 | 說明 |
|------|------|------|
| 台北市路燈數 | ~16 萬盞 | 公開資料 |
| 每盞歷年照片 | ~10~30 張 | 換裝前後、維修前中後、巡查 |
| 總照片量 | 100~500 萬張 | 含歷年累積 |
| 每張 embedding | 512 維 × 4 bytes = 2KB | CLIP ViT-B/32 |
| 全部 embedding | 500 萬 × 2KB ≈ 10GB | 輕鬆放進記憶體 |

**pgvector 效能實測參考**：

| 資料量 | 索引類型 | 查詢延遲 (Top-20) | 召回率 |
|--------|---------|-------------------|--------|
| 100 萬 | IVFFlat (lists=100) | 5~15ms | ~95% |
| 500 萬 | HNSW (m=16, ef=64) | 3~10ms | ~98% |
| 1000 萬 | HNSW (m=16, ef=64) | 10~30ms | ~96% |

需求要求「應用程式回應時間不超過 1 秒」— pgvector 遠低於門檻。

**索引選擇：HNSW**（本系統持續有新照片上傳，HNSW 插入即時生效，不需定期 REINDEX）。

**pgvector 最大優勢：同庫 JOIN**

```sql
-- 找「中正區、最近 30 天、跟這張最像的 20 張施工照」
SELECT pm.*, fs.original_name, fs.file_path
FROM photo_metadata pm
JOIN file_storage fs ON pm.file_id = fs.id
WHERE pm.tenant_id = :tenantId
  AND pm.created_at > NOW() - INTERVAL '30 days'
  AND pm.latitude BETWEEN 25.03 AND 25.05
ORDER BY pm.embedding <=> :queryEmbedding
LIMIT 20;
```

獨立向量資料庫（Milvus/Qdrant）需分兩次查詢再應用層合併，複雜度高且失去事務保證。
本系統照片量 10 年內不會超過 1000 萬張，pgvector 完全足夠，**不需額外維運向量資料庫**。

> **何時該換**：若未來照片量超過 1000 萬張且查詢延遲不可接受，再評估 Milvus/Qdrant。

#### CLIP 能力邊界

| 能做 | 不能做 |
|------|--------|
| 找視覺上相似的照片 | 辨認特定路燈編號（OCR 問題） |
| 文字搜圖（語義搜尋） | 判斷施工品質是否合格 |
| 場景分類（室內/室外/道路/公園） | 精確計算物件尺寸 |
| 偵測明顯異常（傾斜、損壞） | 分辨兩種相似型號燈具 |

---

### D. EXIF 完整性比對（Phase 1）

需求 §10.(4)：非 APP 拍攝的照片匯入時，需比對 EXIF 確認「原始未經修改」。

**比對邏輯**：

| 檢查項目 | 方法 | 判定 |
|---------|------|------|
| EXIF 是否存在 | `metadata-extractor` 讀取 | 無 EXIF → 警告「可能非原始照片」 |
| GPS 是否存在 | 讀 GPSDirectory | 無 GPS → 警告「無法驗證拍攝地點」 |
| GPS vs 工單座標 | Haversine 距離 | > 200m → 警告「位置不符」 |
| 拍攝時間 vs 上傳時間 | DateTimeOriginal vs server time | 差距 > 7 天 → 警告「非近期拍攝」 |
| 軟體修改痕跡 | 檢查 EXIF `Software` tag | 含 Photoshop/GIMP → 警告「可能經修改」 |
| 圖片尺寸一致性 | EXIF 記錄 vs 實際像素 | 不一致 → 警告「可能經裁切」 |

**設計原則**：所有檢查只產生**警告**，不硬性阻擋上傳。最終由機關人員審核決定是否接受。

---

## 實作階段

### Phase 1 — 零 AI 依賴（與檔案上傳模組同步）

| 項目 | 依賴 | 備註 |
|------|------|------|
| EXIF GPS 擷取 + 地點分類 | `metadata-extractor` (~200KB) | 純 Java |
| pHash 重複偵測 | `JImageHash` 或手寫 DCT | 純 CPU |
| EXIF 完整性比對 | `metadata-extractor` | 純 Java |
| photo_metadata 表 | Flyway migration | 存 GPS/時間/pHash |

**photo_metadata 表設計**：
```sql
CREATE TABLE photo_metadata (
    id           BIGSERIAL PRIMARY KEY,
    file_id      BIGINT NOT NULL REFERENCES file_storage(id),
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    altitude     DOUBLE PRECISION,
    taken_at     TIMESTAMPTZ,
    device_make  VARCHAR(100),
    device_model VARCHAR(100),
    software     VARCHAR(200),
    orientation  SMALLINT,
    phash        BIT(64),
    -- EXIF 完整性
    has_exif           BOOLEAN NOT NULL DEFAULT FALSE,
    has_gps            BOOLEAN NOT NULL DEFAULT FALSE,
    exif_modified_suspect BOOLEAN NOT NULL DEFAULT FALSE,
    -- 地點驗證（關聯工單時填入）
    matched_device_id  BIGINT,
    distance_from_target DOUBLE PRECISION,
    location_warning   BOOLEAN NOT NULL DEFAULT FALSE,
    -- 重複偵測
    duplicate_suspect  BOOLEAN NOT NULL DEFAULT FALSE,
    duplicate_of_id    BIGINT,
    -- 審計
    tenant_id    VARCHAR(50),
    created_by   VARCHAR(50),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### Phase 2 — 引入 AI 模型

| 項目 | 依賴 | 備註 |
|------|------|------|
| pgvector 擴充安裝 | `CREATE EXTENSION vector` | PostgreSQL 15+ 支援 |
| CLIP ONNX 模型部署 | `onnxruntime` Java binding (~50MB) + 模型檔 (~350MB) | JVM 內嵌 |
| 上傳時自動產生 embedding | 非同步計算，不阻塞上傳 | ~50ms/張 (CPU) |
| 以圖搜圖 API | `GET /v1/auth/photos/{id}/similar` | cosine distance |
| 以文字搜圖 API | `GET /v1/auth/photos/search?q=燈桿傾斜` | CLIP text encoder |
| embedding 欄位 | `ALTER TABLE photo_metadata ADD COLUMN embedding vector(512)` | IVFFlat 索引 |

---

## 上傳處理完整流程

```
APP/Web 上傳照片
    │
    ├─ 1. 安全檢查（KB-001）
    │     副檔名白名單 → Magic bytes → 檔案大小
    │
    ├─ 2. EXIF 擷取（Phase 1）
    │     GPS、拍攝時間、設備、Software tag
    │     → 存入 photo_metadata
    │
    ├─ 3. EXIF 完整性比對（Phase 1）
    │     GPS vs 工單座標 → distance + warning
    │     拍攝時間 vs 上傳時間 → 合理性檢查
    │     Software tag → 修改痕跡檢查
    │
    ├─ 4. pHash 計算 + 重複偵測（Phase 1）
    │     計算 64-bit pHash → 查 DB Hamming ≤ 10
    │     → duplicateSuspect + duplicateOfId
    │
    ├─ 5. 圖片消毒 + 壓縮（KB-001）
    │     ImageIO 重新編碼 → EXIF 全丟 → 乾淨像素
    │     （注意：步驟 2 必須在步驟 5 之前）
    │
    ├─ 6. UUID 重命名 + 存檔
    │
    ├─ 7. [Phase 2] 非同步產生 CLIP embedding
    │     → 存入 photo_metadata.embedding
    │
    └─ 8. 回傳 fileId + metadata（含 GPS、距離、警告）
```

---

## 授權合規

| 元件 | 授權 | 合規性 |
|------|------|--------|
| metadata-extractor | Apache 2.0 | ✅ 商用自由 |
| JImageHash | MIT | ✅ 商用自由 |
| OpenAI CLIP (ViT-B/32) | MIT | ✅ 「可合法使用之 AI 引擎」 |
| ONNX Runtime | MIT | ✅ 商用自由 |
| pgvector | BSD | ✅ 商用自由 |
| PostgreSQL | PostgreSQL License (BSD-like) | ✅ 商用自由 |

所有元件皆為**開源、自架部署、資料不離開機關網路**。

---

## 風險與限制

| 風險 | 影響 | 緩解 |
|------|------|------|
| GPS 可被偽造 | 施工地點驗證失準 | 多重交叉驗證（EXIF GPS × APP 即時定位 × 工單座標） |
| pHash 對大幅修改的照片失效 | 漏報重複 | Phase 2 的 CLIP embedding 補強 |
| CLIP 模型 350MB 佔磁碟 | 部署體積增加 | 可接受；比 Python 環境輕量 |
| CLIP CPU 推論 ~50ms/張 | 大量上傳時延遲 | 非同步計算（@Async），不阻塞上傳回應 |
| 無 EXIF 的照片（截圖/通訊軟體轉傳） | 無法驗證地點和時間 | 允許上傳，標記警告，由人工審核 |
