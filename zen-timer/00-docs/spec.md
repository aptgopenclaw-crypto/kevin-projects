# 🧘‍♀️ ZenTimer - 瑜伽循環計時器 APP 開發規格書

## 1. 專案概述 (Project Overview)

開發一款名為 "ZenTimer" 的瑜伽循環計時器 APP（僅 Android 平台）。

**核心使用情境**：使用者設定一個「間隔時間」（例如 5 分鐘）和「總練習時長」（例如 30 分鐘），APP 會每隔設定的間隔時間播放提示音提醒轉換動作，直到總時長結束。畫面顯示當前倒數與已播放次數。

**關鍵特性**：
- 循環計時：每次間隔到達後播放提示音，自動重置倒數，進入下一輪
- 背景持續運行：無論前景或背景，計時器都必須正常執行
- 設定持久化：關閉 APP 後重新開啟，保留上次的所有設定
- 自定義音效：支援選擇本地音檔（限 15 秒以內）

## 2. 目標平台 (Target Platform)

- **平台**: Android only（不考慮 iOS）
- **最低 Android 版本**: API 24 (Android 7.0)
- **開發框架**: React Native (Expo CLI, SDK 52)
- **建構方式**: EAS Build 或本地 APK 打包

## 3. 技術棧 (Tech Stack)

| 用途 | 套件 |
|------|------|
| 框架 | React Native + Expo CLI |
| 語言 | TypeScript |
| 狀態管理 | React Hooks (`useState`, `useEffect`, `useRef`) |
| 資料持久化 | `@react-native-async-storage/async-storage` |
| 音訊播放 | `expo-av` |
| 檔案選擇 | `expo-document-picker` |
| 防休眠 | `expo-keep-awake` |
| 背景執行 | `expo-task-manager` + `expo-background-fetch` 或 Android Foreground Service (via `expo-notifications` 或自訂 native module) |
| 本地通知 | `expo-notifications`（背景計時結束通知） |
| 樣式 | StyleSheet (內建) |

## 4. 核心功能需求 (Functional Requirements)

### FR1: 循環計時器 (Interval Timer)

- **間隔時間設定**：透過「+ / -」按鈕設定間隔分鐘數
  - 步進值：1 分鐘
  - 最小值：1 分鐘
  - 最大值：60 分鐘
  - 快速設定按鈕：1分、3分、5分、10分
- **總練習時長設定**：設定此次練習的總時長
  - 步進值：1 分鐘
  - 最小值：等於間隔時間（至少播放 1 次）
  - 最大值：180 分鐘（3 小時）
  - 快速設定按鈕：15分、30分、45分、60分
- **自動計算播放次數**：總時長 ÷ 間隔時間 = 預計播放提示音次數
- **控制按鈕**：「開始 (Start)」、「暫停 (Pause)」、「停止 (Stop)」

### FR2: 計時器運行邏輯

1. 使用者按下「開始」
2. 倒數計時從間隔時間開始遞減
3. 倒數歸零時：
   - 播放提示音
   - 已播放次數 +1
   - 自動重置倒數至間隔時間，繼續下一輪
4. 當已播放次數達到預計次數時，整個練習結束
5. 練習結束後顯示完成畫面

### FR3: 提示音設定 (Alert Sound)

- **內建音效**：提供 3 種音效選項（頌缽聲、木魚聲、清脆叮聲）
- **自定義音效**：
  - 使用 `expo-document-picker` 選擇本地 `.mp3` 或 `.wav`
  - **限制：音檔時長必須 ≤ 15 秒**，超過則拒絕並提示使用者
  - 選擇後將檔案複製到 APP 內部儲存，避免原檔被刪除導致錯誤
- **預覽功能**：設定頁面提供「試聽」按鈕
- **音量**：跟隨系統媒體音量

### FR4: 背景執行 (Background Execution)

- APP 在背景時計時器必須持續運作（使用 Android Foreground Service）
- 背景運行時顯示持久通知 (Persistent Notification)，顯示目前倒數狀態
- 提示音在背景時仍需正常播放（使用 `Audio.setAudioModeAsync` 設定適當模式）
- APP 回到前景時，UI 狀態與實際計時同步（基於時間戳計算，非依賴 setInterval）

### FR5: 防休眠 (Keep Awake)

- 計時器運行中（前景時），保持螢幕常亮
- 計時器停止或練習結束時，解除螢幕常亮

### FR6: 資料持久化 (Data Persistence)

- 使用 `AsyncStorage` 儲存以下設定：
  - 間隔時間（分鐘）
  - 總練習時長（分鐘）
  - 音效選擇（內建 ID 或自定義檔案路徑）
- APP 啟動時自動載入上次的設定

## 5. UI/UX 設計規範

### 主題色
- 禪意風格，柔和大地色系
- 主色：鼠尾草綠 `#8A9A5B`
- 背景：米白色 `#F5F5DC`
- 文字：深灰 `#333333`
- 強調色：深綠 `#5C6B3E`

### 畫面佈局（單頁面 APP + Modal 設定）

**主畫面**：
- 頂部：APP 標題「ZenTimer」
- 中上部：顯示「已播放 N / 總 M 次」
- 中部：大字倒數 `MM:SS` + 圓環進度條
- 中下部：間隔時間與總時長設定區（使用 +/- 按鈕 + 快速按鈕）
- 底部：控制按鈕（開始/暫停/停止）+ 音效設定齒輪圖示

**音效設定 Modal**：
- 內建音效列表（Radio 選擇）
- 「選擇本地音檔」按鈕
- 已選音效名稱顯示
- 「試聽」按鈕

### 易用性
- 按鈕面積大（最小 48x48 dp），方便瑜伽墊上操作
- 計時運行中隱藏設定區域，僅顯示倒數與播放次數，避免誤觸

## 6. 資料結構 (Data Structure)

```typescript
interface TimerConfig {
  intervalMinutes: number;       // 間隔時間（分鐘），最小 1，最大 60
  totalMinutes: number;          // 總練習時長（分鐘），最小 = intervalMinutes
}

interface TimerState {
  remainingSeconds: number;      // 當前輪次剩餘秒數
  currentRound: number;          // 目前第幾輪（從 1 開始）
  totalRounds: number;           // 總輪次 = totalMinutes / intervalMinutes
  isRunning: boolean;
  isPaused: boolean;
  isCompleted: boolean;
  startTimestamp: number | null; // 用於背景回來後校準時間
}

interface SoundSetting {
  type: 'builtin' | 'custom';
  builtinId: 'singing_bowl' | 'wooden_fish' | 'chime';
  customUri: string | null;      // APP 內部儲存的檔案路徑
  customFileName: string | null; // 顯示用檔名
}

// AsyncStorage 儲存的設定
interface PersistedSettings {
  intervalMinutes: number;
  totalMinutes: number;
  sound: SoundSetting;
}
```

## 7. 錯誤處理

| 情境 | 處理方式 |
|------|----------|
| 自定義音檔超過 15 秒 | 拒絕選擇，Toast 提示「音效檔案須在 15 秒以內」 |
| 自定義音檔無法播放 | 回退至預設內建音效（頌缽聲），Toast 提示 |
| 背景服務被系統殺掉 | APP 回前景時根據時間戳重新計算狀態 |
| AsyncStorage 讀取失敗 | 使用預設值（間隔 5 分鐘、總時長 30 分鐘、頌缽聲） |

## 8. 非功能需求

- **效能**：計時器精度誤差 ≤ 1 秒（使用時間戳比對，非純 setInterval 累加）
- **電量**：背景運行時盡量降低 CPU 使用（使用 Foreground Service 而非高頻輪詢）
- **APK 大小**：盡量控制在 30MB 以內（內建音效使用小檔案）
