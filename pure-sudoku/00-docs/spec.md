

---

# 🧩 PureSudoku (純淨數獨) - Flutter AI 開發規格書 (Spec)

## 1. 專案概述 (Project Overview)
開發一款名為 "PureSudoku" 的本地端數獨遊戲 APP。核心宗旨：**零廣告、零聯網、極致流暢**。提供完整的數獨體驗，包含難度選擇、筆記模式 (Pencil Marks)、智能高亮提示、計時器以及自動儲存進度功能。

## 2. 推薦技術棧 (Tech Stack)
*   **框架**: Flutter (最新穩定版, 確保啟用 Null Safety)
*   **語言**: Dart
*   **目標平台**: **Android Only**
*   **狀態管理**: `flutter_riverpod` (推薦，AI 生成穩定且程式碼簡潔，適合處理複雜的棋盤狀態更新)
*   **本地儲存**: `shared_preferences` (用於輕量級的遊戲進度自動儲存)
*   **UI 輔助**: 內建 `GridView`, `CustomPaint` (如需複雜繪圖), `TextStyle`
*   **UI 工具套件**: `flutter_launcher_icons` (App 圖示生成), `flutter_native_splash` (啟動畫面)

## 3. 核心功能需求 (Functional Requirements)

### FR1: 數獨核心引擎 (Sudoku Engine)
*   **棋盤生成**: 使用回溯法 (Backtracking) 生成一個完整且**唯一解**的 9x9 數獨棋盤。
*   **難度挖空**: 根據難度 (簡單: 挖空 30-40 格, 中等: 40-50 格, 困難: 50-60 格) 隨機移除數字，確保剩餘提示數仍能推導出唯一解。
*   **驗證邏輯**: 即時驗證填入的數字是否符合數獨規則 (同行、同列、同 3x3 宮格不重複)。

### FR2: 棋盤 UI 與互動 (Board UI & Interaction)
*   **視覺呈現**: 9x9 網格，**必須透過動態計算 Border，加粗 3x3 宮格的邊界線**（這是數獨視覺辨識的關鍵）。
*   **智能高亮 (Smart Highlight)**: 當使用者點選某個格子時：
    1. 高亮該格子本身 (深色背景)。
    2. 高亮該格子所在的整列與整行 (淡色背景)。
    3. 高亮棋盤上所有**數值相同**的格子。
*   **狀態標記**: 區分「系統給定的數字 (固定, 粗體深色)」與「玩家填入的數字 (可修改, 藍色)」。若開啟「即時錯誤檢查」，填錯的數字顯示為紅色。

### FR3: 輸入與輔助工具 (Input & Tools)
*   **數字鍵盤**: 螢幕下方提供 1-9 的數字按鈕，以及「橡皮擦 (清除)」按鈕。
*   **筆記模式 (Pencil Marks)**: 一個切換開關。開啟後，點選數字不會直接填入答案，而是以**小字型**標記在該格子的角落（一個格子內以 3x3 微型網格顯示候選數字）。
*   **提示功能 (Hint)**: 隨機填滿一個目前為空的正確格子。

### FR4: 遊戲流程與持久化 (Game Flow & Persistence)
*   **計時器**: 遊戲開始後顯示 `MM:SS`。
*   **自動儲存**: 每次棋盤狀態改變時，自動將當前進度序列化並儲存至本地。下次打開 APP 時自動恢復。

### FR5: Android 專屬需求 (Android-Specific Requirements)
*   **螢幕方向**: 鎖定直向 (Portrait)。在 `android/app/src/main/AndroidManifest.xml` 的 `<activity>` 中設定 `android:screenOrientation="portrait"`。
*   **網路權限**: 嚴格不加入 `INTERNET` permission，確保零聯網承諾在 Manifest 層級落實。
*   **返回鍵行為**: 使用 `PopScope` 攔截 Android 返回鍵，顯示二次確認對話框（「確定要離開遊戲嗎？」），防止誤觸退出。
*   **觸覺回饋**: 點擊數字鍵盤時呼叫 `HapticFeedback.lightImpact()`，提升操作回饋感。
*   **自適應棋盤尺寸**: 棋盤邊長使用 `MediaQuery.of(context).size.width` 動態計算，不寫死尺寸，相容手機與平板。

## 4. 資料結構設計 (Data Models)
請 AI 嚴格遵守此 Dart 資料結構，這是狀態管理的基礎：

```dart
// 單一格子狀態
class Cell {
  final int? value;           // 當前顯示的數字 (1-9 或 null)
  final bool isFixed;         // 是否為系統初始給定的數字 (不可修改)
  final List<int> notes;      // 筆記模式下的候選數字陣列，例如 [1, 5, 9]
  final bool isError;         // 是否為錯誤填入 (用於紅色標記)

  Cell({
    this.value,
    this.isFixed = false,
    this.notes = const [],
    this.isError = false,
  });

  // 複製並修改部分屬性的方法 (Immutable update)
  Cell copyWith({
    int? value,
    bool? isFixed,
    List<int>? notes,
    bool? isError,
  }) {
    return Cell(
      value: value ?? this.value,
      isFixed: isFixed ?? this.isFixed,
      notes: notes ?? this.notes,
      isError: isError ?? this.isError,
    );
  }
}

// 遊戲難度列舉
enum Difficulty { easy, medium, hard }
```

---

## 5. 檔案結構 (File Structure)

```
lib/
├── main.dart
├── models/
│   └── cell.dart                  # Cell class & Difficulty enum（見第 4 節）
├── services/
│   └── sudoku_generator.dart      # 棋盤生成與驗證演算法
├── providers/
│   └── game_provider.dart         # Riverpod GameState & GameNotifier
└── widgets/
    ├── sudoku_board.dart           # 9x9 棋盤 UI
    └── game_controls.dart         # 數字鍵盤、計時器、工具列
android/
├── app/
│   ├── build.gradle               # minSdk 21, targetSdk 35, compileSdk 35
│   └── src/main/AndroidManifest.xml  # portrait 鎖定, 無 INTERNET permission
pubspec.yaml
```

---

## 6. AI Agent 開發指令 (Agent Prompt)

> **你是一位資深的 Flutter / Dart 開發專家，擅長演算法、Riverpod 狀態管理與複雜 UI 佈局。**
> **請依照本規格書，一次性自主完成 "PureSudoku" Android APP 的完整實作，無需等待確認。**
> 按照以下順序建立所有檔案，確保每個模組在建立時即可正確編譯。

### Step 1: 專案初始化
執行以下命令建立專案：
```bash
flutter create --platforms=android pure_sudoku
cd pure_sudoku
```

在 `pubspec.yaml` 的 `dependencies` 加入：
```yaml
dependencies:
  flutter_riverpod: ^2.5.1
  shared_preferences: ^2.2.3

dev_dependencies:
  flutter_launcher_icons: ^0.14.1
  flutter_native_splash: ^2.4.1
```

執行 `flutter pub get`。

**`android/app/build.gradle`** — 設定：
- `minSdkVersion 21`
- `targetSdkVersion 35`
- `compileSdkVersion 35`

**`android/app/src/main/AndroidManifest.xml`** — 要求：
- `<activity>` 加入 `android:screenOrientation="portrait"`
- **不得**包含 `<uses-permission android:name="android.permission.INTERNET"/>` 或任何網路權限

**`lib/main.dart`** — 要求：
- `WidgetsFlutterBinding.ensureInitialized()` 後呼叫 `SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp])`
- 根 Widget 包裹 `ProviderScope`

### Step 2: 數獨引擎 — `lib/services/sudoku_generator.dart`
實作以下三個函式，加上 DartDoc 註解：
1. `bool isValidMove(List<List<int?>> board, int row, int col, int num)` — 驗證同行、同列、同 3x3 宮格不重複
2. `bool solveSudoku(List<List<int?>> board)` — 回溯法求解，回傳是否有解
3. `Map<String, dynamic> generateSudoku(Difficulty difficulty)` — 先生成完整解，再按難度挖空（Easy: 30-40, Medium: 40-50, Hard: 50-60），驗證剩餘提示仍為唯一解，回傳 `{'board': List<List<Cell>>, 'solution': List<List<int>>}`

演算法要求：挖空時隨機打亂順序；每次移除一格後驗證唯一性，若破壞唯一性則還原並跳過。

### Step 3: 狀態管理 — `lib/providers/game_provider.dart`
定義 `GameState`（freezed 或手寫 copyWith 均可）：
```dart
class GameState {
  final List<List<Cell>> board;
  final List<List<int>> solution;
  final Difficulty difficulty;
  final int? selectedRow;
  final int? selectedCol;
  final int timerSeconds;
  final bool isNoteMode;
  final bool isPlaying;
  final bool isCompleted;
}
```

建立 `GameNotifier extends Notifier<GameState>`，實作：
- `startNewGame(Difficulty d)` — 呼叫 generator，重設計時器，觸發 `saveGame()`
- `selectCell(int row, int col)` — 僅更新 selectedRow/Col
- `inputNumber(int? num)` — 若 isNoteMode 為 true 則寫入 notes；否則填入 value，比對 solution 設定 isError，填完後檢查 isCompleted，觸發 `saveGame()`
- `toggleNoteMode()` — 切換 isNoteMode，觸發 `saveGame()`
- `useHint()` — 隨機選取一個空格填入正確答案，觸發 `saveGame()`
- `tick()` — timerSeconds++（由 UI 層的 Timer 呼叫）
- `saveGame()` / `loadGame()` — 使用 `shared_preferences` 序列化/還原完整 GameState 為 JSON

所有棋盤更新必須使用 `Cell.copyWith` 做 immutable 更新。

### Step 4: 棋盤 UI — `lib/widgets/sudoku_board.dart`
- 棋盤邊長 = `MediaQuery.of(context).size.width`（不寫死像素）
- 使用 `CustomPaint` 繪製整個 9x9 網格及粗細邊框（**優先使用 CustomPaint，而非在每個 GridView item 上單獨畫 Border**，避免邊框重疊問題）：
  - 細線（0.5px, `#BDBDBD`）：所有格線
  - 粗線（2.0px, `#333333`）：每 3 格的宮格邊界
- 在 `CustomPaint` 上疊加 `GridView.builder`（81 格）實作點擊與顯示
- **智能高亮**背景色邏輯（優先順序高到低）：
  1. 選中格：`#90CAF9`（深藍）
  2. 同行/同列/同宮格：`#E3F2FD`（淡藍）
  3. 相同數值：`#C8E6C9`（淡綠）
  4. 預設：`#FFFFFF`（白）
- **文字顯示**：
  - 固定數字：粗體黑色 `#212121`
  - 玩家數字：藍色 `#1565C0`
  - 錯誤數字：紅色 `#C62828`
  - 筆記模式：格子內用 3x3 `GridView` 顯示 1-9，`notes` 中有的數字顯示小字（灰色，font size ~10），沒有的留空

### Step 5: 控制區 UI — `lib/widgets/game_controls.dart`
佈局（由上至下）：
1. **頂部列**：左側難度 `SegmentedButton<Difficulty>`（Easy/Medium/Hard），右側 `MM:SS` 計時器文字
2. **工具列**：「筆記模式」`ToggleButton`（啟用時高亮顯示）、「提示」`IconButton`
3. **數字鍵盤**：3x3 + 橡皮擦的 10 鍵網格（數字 1-9 + 橡皮擦），點擊時：
   - 呼叫 `ref.read(gameProvider.notifier).inputNumber(num)`
   - 呼叫 `HapticFeedback.lightImpact()`

在遊戲主頁面的根層級包裹 `PopScope`：
```dart
PopScope(
  canPop: false,
  onPopInvokedWithResult: (didPop, result) async {
    // 彈出 AlertDialog 詢問「確定要離開遊戲嗎？」
    // 使用者確認後才 Navigator.of(context).pop()
  },
)
```

### Step 6: 整合與計時器
在主遊戲頁面的 `initState` 中啟動 `Timer.periodic(Duration(seconds: 1), ...)`，每秒呼叫 `notifier.tick()`。在 `dispose` 中取消計時器。Provider 初始化時自動呼叫 `loadGame()`。

### Step 7: Android 收尾
在 `pubspec.yaml` 加入 `flutter_launcher_icons` 與 `flutter_native_splash` 設定區塊（使用佔位圖示路徑，讓 Agent 在有圖示素材時替換）：
```yaml
flutter_launcher_icons:
  android: true
  ios: false
  image_path: "assets/icon/icon.png"

flutter_native_splash:
  color: "#FFFFFF"
  image: "assets/icon/icon.png"
  android: true
  ios: false
```

執行：
```bash
dart run flutter_launcher_icons
dart run flutter_native_splash:create
```

---

## 7. 全域程式碼規範 (Coding Standards)

- **Null Safety**：全面啟用，禁止使用 `!` 強制解包，除非邏輯上已確保非 null
- **Immutability**：棋盤狀態所有更新透過 `Cell.copyWith` + list `map` 重建，不直接 mutate
- **UI 尺寸**：禁止寫死像素值，一律使用 `MediaQuery` 或 `LayoutBuilder`
- **網路隔離**：不引入任何會發起網路請求的套件或程式碼
- **色彩系統**：白底 `#FFFFFF`、深灰線條 `#333333`、選中藍 `#90CAF9`、高亮藍 `#E3F2FD`、高亮綠 `#C8E6C9`、錯誤紅 `#C62828`

