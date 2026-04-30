# ADR-004: 行動 APP 技術選型 — 採用 Flutter

> **狀態**: Accepted
> **日期**: 2026-04-24
> **決策者**: Kevin
> **關聯需求**: §10 行動作業應用程式 APP

---

## 背景

需求 §10 要求提供行動 APP，支援資產普查、維護履歷記錄、巡查記錄、GPS 照片管控（50m 電子圍籬 + EXIF 驗證）、離線續傳等功能。需同時支援 Android 與 iOS。

## 決策

**採用 Flutter 作為行動 APP 開發框架。**

## 考量方案

| 方案 | 語言 | 跨平台 | 離線支援 | GPS/Camera | 學習成本 | 社群 |
|------|------|:------:|:--------:|:----------:|:--------:|:----:|
| **Flutter** | Dart | ✅ Android + iOS | SQLite 原生 | 成熟 | 中 | 活躍 |
| React Native | JavaScript | ✅ Android + iOS | AsyncStorage / SQLite | 成熟 | 低（已用 JS） | 活躍 |
| PWA | TypeScript | ✅ 瀏覽器 | Service Worker + IndexedDB | 受限 | 最低（複用前端） | — |
| 原生雙開發 | Kotlin + Swift | ✅ 各自原生 | 原生最佳 | 最佳 | 高（兩套程式碼） | — |

### 排除理由

| 方案 | 排除原因 |
|------|---------|
| **PWA** | iOS Safari 對 Service Worker、背景 GPS、相機 EXIF 存取支援不完整；無法上架 App Store；離線續傳不可靠 |
| **React Native** | 可行，但 SQLite 需第三方橋接（expo-sqlite）；EXIF 處理生態不如 Flutter 成熟；效能略遜 |
| **原生雙開發** | 兩套程式碼維護成本翻倍，人力不足以支撐 |

## Flutter 選型理由

### 1. 一套程式碼，雙平台原生執行

Dart 程式碼編譯為 Android APK 和 iOS IPA，兩個平台都是原生執行（AOT 編譯），不是 WebView 包裝。

### 2. 需求功能套件成熟度

| 需求功能 | Flutter 套件 | 授權 | 成熟度 |
|---------|-------------|------|:------:|
| GPS 定位 | `geolocator` | MIT | ⭐⭐⭐ |
| 50m 電子圍籬 | `geolocator` 距離計算 | MIT | ⭐⭐⭐ |
| 相機拍照 | `camera` / `image_picker` | BSD | ⭐⭐⭐ |
| EXIF 讀取 | `exif` | MIT | ⭐⭐⭐ |
| 照片浮水印 | `image` (Dart) | BSD | ⭐⭐⭐ |
| QR Code 掃描 | `mobile_scanner` | BSD | ⭐⭐⭐ |
| SQLite 離線 | `sqflite` | BSD | ⭐⭐⭐ |
| 地圖 | `flutter_map` (Leaflet binding) | BSD | ⭐⭐⭐ |
| 推播通知 | `firebase_messaging` (FCM/APNs) | BSD | ⭐⭐⭐ |
| HTTP 通訊 | `dio` | MIT | ⭐⭐⭐ |
| 分段上傳 | `dio` chunked upload | MIT | ⭐⭐⭐ |
| JWT 認證 | `flutter_secure_storage` | BSD | ⭐⭐⭐ |

所有套件皆開源免費，授權無商用限制。

### 3. 離線續傳架構

```
┌──────────────────────────┐
│      Flutter APP          │
│                          │
│  拍照/巡查/普查           │
│       │                  │
│       ▼                  │
│  SQLite 本地暫存          │  ← 斷網時資料存這裡
│       │                  │
│       ▼                  │
│  SyncManager             │  ← 偵測網路恢復
│       │                  │
│       ▼                  │
│  Dio chunked upload      │  ← 分段上傳（≤2MB/chunk）
│  (自動重試 + 斷點續傳)    │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│  Spring Boot REST API     │
│  (既有後端，共用 JWT)      │
└──────────────────────────┘
```

### 4. 與既有後端整合

- **共用 JWT 認證**: APP 登入後取得同一組 access/refresh token，呼叫同一套 REST API
- **不需額外後端**: 所有 API 已在 Spring Boot 中，APP 只是另一個 client
- **推播通知**: FCM (Android) / APNs (iOS)，後端透過 Firebase Admin SDK 發送

## 平台發佈費用

| 項目 | 費用 | 頻率 | 說明 |
|------|------|------|------|
| Apple Developer Program | USD $99/年 | 年繳 | iOS App Store 上架必要 |
| Google Play Console | USD $25 | 一次性 | Android Play Store 上架必要 |

**這筆費用可以接受，已確認。**

## 開發環境需求

| 項目 | 說明 |
|------|------|
| Flutter SDK | 3.x stable |
| Dart | 隨 Flutter SDK 內建 |
| Android Studio | Android 模擬器 + 編譯 |
| Xcode (Mac) | iOS 編譯 + 模擬器（**需要一台 Mac**） |
| VS Code | Flutter 開發 IDE（也可用 Android Studio） |

> ⚠️ iOS 編譯必須在 macOS 上進行，這是 Apple 的限制。如果團隊沒有 Mac，需準備一台（或使用 CI 雲端 Mac，如 GitHub Actions macOS runner）。

## 後果

### 正面
- 一套程式碼同時產出 Android + iOS，開發維護成本減半
- Dart AOT 編譯，效能接近原生
- 離線 + SQLite + 分段上傳有成熟方案
- 所有需求功能套件齊全，無需自行開發底層
- 與既有 Spring Boot 後端無縫整合（共用 JWT + REST API）

### 負面
- 團隊需學習 Dart 語言（學習曲線約 2–3 週）
- iOS 編譯需 Mac 環境
- Flutter 版本升級偶有 breaking changes（但 stable channel 穩定）
- APP 體積較原生略大（約 +10–15MB）
