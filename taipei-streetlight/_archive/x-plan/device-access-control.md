# 設備權限設計：View / Operate 雙層存取控制

> 版本：v1.0 ｜日期：2026-04-22
> 解決問題：「看得到」和「能操作」是兩件不同的事

---

## 1. 問題描述

現有 DataScope 機制是「一刀切」——看到就能操作。但實務上有三種情境無法滿足：

| # | 情境 | 現有 DataScope | 缺什麼 |
|---|------|---------------|--------|
| 1 | 設備屬於 A 單位，只有 A 的人能看能操作 | ✅ 可以 | — |
| 2 | 設備屬於 A 單位，但跨科室的承辦人也要能看能操作 | ❌ 承辦人不在 A 部門，看不到 | 個人指派機制 |
| 3 | 設備屬於 B 單位，A 單位上級能看，但不能操作 | ⚠️ 看可以，但擋不住操作 | View ≠ Operate 區分 |

---

## 2. 組織架構（背景）

```
公燈處 (dept=5, 根節點)
├── 第一分隊/北區 (dept=6)
├── 第二分隊/南區 (dept=7)
├── 工程股 (dept=8)
├── 行政股 (dept=9)
└── 智慧路燈管理中心 (dept=10)
```

---

## 3. Use Case 實例

### Use Case 1：基本情境 — 設備歸屬本單位

> 「北區分隊的路燈，只有北區分隊的人能看到和操作」

**角色**：
- 王大明 — 第一分隊/北區（dept=6），ROLE_OPERATOR
- 設備 L-001 — `dept_id=6`（歸屬北區）

**操作**：

```
王大明 → 登入 → 設備列表
  DataScope: THIS_LEVEL → visibleDeptIds = {6}
  L-001.dept_id = 6 ∈ {6} → ✅ 看得到

王大明 → 點擊 L-001 → 報修
  canOperate: L-001.dept_id(6) == 王大明.dept_id(6) → ✅ 能操作

王大明 → 點擊 L-001 → 派工
  canOperate: ✅ → 可以派工
```

**結論**：現有 DataScope 就能處理，不需額外設計。

---

### Use Case 2：跨科室承辦人 — 工程股的人負責北區契約

> 「工程股的張工程師是『北區路燈契約 A』的承辦人，
> 　雖然他不屬於北區分隊，但他要能看到並操作契約 A 下的所有路燈」

**角色**：
- 張工程師 — 工程股（dept=8），ROLE_OPERATOR
- 設備 L-001 ~ L-500 — `dept_id=6`（歸屬北區），`contract_id=契約A`

**如果沒有承辦人指派機制**：

```
張工程師 → 登入 → 設備列表
  DataScope: THIS_LEVEL → visibleDeptIds = {8}
  L-001.dept_id = 6 ∉ {8} → ❌ 看不到

結果：張工程師完全看不到他負責的路燈，無法工作。
```

**有了 device_managers 之後**：

```sql
-- 管理員指派張工程師為契約A的承辦人
INSERT INTO device_managers (tenant_id, scope_type, scope_id, user_id)
VALUES ('TENANT_A', 'CONTRACT', /*契約A的id*/ 1, /*張工程師的id*/ 42);
```

```
張工程師 → 登入 → 設備列表
  DataScope: visibleDeptIds = {8}（工程股）
  + 個人指派: device_managers 找到 CONTRACT=契約A → 展開為 L-001~L-500
  → 聯集結果：工程股設備 ∪ 契約A設備 = 都看得到 ✅

張工程師 → 點擊 L-001 → 報修
  canOperate:
    路徑1: L-001.dept_id(6) == 張工程師.dept_id(8)? → ❌
    路徑2: isAssignedManager? → CONTRACT 命中 → ✅ 能操作

張工程師 → 點擊 L-001 → 派工給廠商
  canOperate: ✅（承辦人身份）→ 可以派工
```

**結論**：透過 `device_managers` 按契約指派，一筆紀錄就覆蓋 500 盞路燈。

---

### Use Case 3：上級只能看，不能操作

> 「公燈處處長（根節點）能看到所有分隊的設備，
> 　但不應該直接對北區路燈報修派工——那是北區分隊的事」

**角色**：
- 李處長 — 公燈處（dept=5），ROLE_ADMIN，DataScope=ALL
- 設備 L-001 — `dept_id=6`（歸屬北區）

```
李處長 → 登入 → 設備列表
  DataScope: ALL → visibleDeptIds = {5,6,7,8,9,10}
  L-001.dept_id = 6 ∈ {5,6,7,8,9,10} → ✅ 看得到

李處長 → 點擊 L-001 → 想報修
  canOperate:
    路徑1: L-001.dept_id(6) == 李處長.dept_id(5)? → ❌ 不同部門
    路徑2: isAssignedManager? → 未指派 → ❌
  → 前端「報修」按鈕不顯示

李處長 → 只能查看設備資訊、歷程、統計報表
```

**結論**：View Scope 寬（DataScope=ALL），Operate Scope 嚴（同部門 or 指派），處長看得到但不亂動。

---

### Use Case 4：SUPER_ADMIN 不受限

> 「系統管理員需要能操作所有設備（緊急狀況）」

**角色**：
- 系統管理員 — `is_super_admin=true`

```
系統管理員 → 任何設備
  canView: SUPER_ADMIN → ✅ 直接通過
  canOperate: SUPER_ADMIN → ✅ 直接通過

// 程式碼
public boolean canOperate(Long deviceId, Long userId) {
    if (SecurityContextUtils.isSuperAdmin()) return true;  // 第一行就短路
    // ... 正常判斷
}
```

---

### Use Case 5：按回路指派 — 一個人負責一條回路

> 「陳技術員負責 C-001 回路的 30 盞路燈，
> 　他屬於智慧路燈管理中心（dept=10），但路燈歸屬不同分隊」

**角色**：
- 陳技術員 — 智慧路燈管理中心（dept=10），ROLE_OPERATOR
- 回路 C-001 下掛 30 盞路燈，分別 `dept_id=6` 和 `dept_id=7`

```sql
-- 指派陳技術員為 C-001 回路承辦人
INSERT INTO device_managers (tenant_id, scope_type, scope_id, user_id)
VALUES ('TENANT_A', 'CIRCUIT', /*C-001的id*/ 5, /*陳技術員*/ 55);
```

```
陳技術員 → 設備列表
  DataScope: visibleDeptIds = {10}
  + 個人指派: CIRCUIT=C-001 → 展開為 30 盞路燈
  → 看到 dept=10 的設備 + C-001 回路下的設備 ✅

陳技術員 → 點擊 L-010 (dept=6, circuit=C-001)
  canOperate:
    路徑1: dept_id(6) == 陳技術員.dept_id(10)? → ❌
    路徑2: isAssignedManager? → CIRCUIT=C-001 命中 → ✅
```

---

### Use Case 6：指派到單一設備 — 特殊路燈由特定人負責

> 「市府前廣場的景觀路燈 L-VIP-001 比較特殊，
> 　指定行政股的林專員負責，雖然設備歸屬北區」

```sql
INSERT INTO device_managers (tenant_id, scope_type, scope_id, user_id)
VALUES ('TENANT_A', 'DEVICE', /*L-VIP-001的id*/ 999, /*林專員*/ 77);
```

```
林專員（dept=9, 行政股） → 設備列表
  → 看到行政股設備 + L-VIP-001 ✅

林專員 → L-VIP-001 → 報修
  canOperate: DEVICE 指派命中 → ✅
```

---

### Use Case 7：同一設備多人負責

> 「L-001 同時由北區分隊的王大明（本部門）和工程股的張工程師（契約承辦）負責」

```
王大明 → L-001
  canOperate: dept_id 命中 → ✅（本部門路徑）

張工程師 → L-001
  canOperate: CONTRACT 指派命中 → ✅（承辦人路徑）

兩人都可以操作，不衝突。
工單流程中的 dispatched_by 記錄「是誰派的工」，簽核歷程明確。
```

---

## 4. 資料模型

### 4-1 新增表：device_managers

```sql
CREATE TABLE device_managers (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    scope_type      VARCHAR(20)     NOT NULL,
    scope_id        BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    created_by      VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, scope_type, scope_id, user_id)
);

CREATE INDEX idx_device_managers_user ON device_managers(tenant_id, user_id);
CREATE INDEX idx_device_managers_scope ON device_managers(scope_type, scope_id);

COMMENT ON TABLE device_managers IS '設備承辦人指派：跨科室人員可看到並操作指定範圍的設備';
COMMENT ON COLUMN device_managers.scope_type IS
  'DEVICE(單一設備) / CONTRACT(整個契約) / CIRCUIT(整條回路)';
```

### 4-2 scope_type 粒度

```
粒度由大到小：

  CONTRACT ──→ 覆蓋契約下所有設備（最常用）
     │          例：「張工程師負責契約A的500盞路燈」
     │          一筆 device_managers 就搞定
     │
  CIRCUIT ───→ 覆蓋回路下所有設備
     │          例：「陳技術員負責C-001回路的30盞」
     │
  DEVICE ────→ 單一設備
               例：「林專員負責VIP景觀燈」
               精細控制，但資料筆數多
```

**判定優先序**：DEVICE → CONTRACT → CIRCUIT（先查最具體的）

---

## 5. 權限判定流程圖

```
使用者請求操作設備 X
         │
         ↓
  ┌─ SUPER_ADMIN? ──→ ✅ 直接通過
  │       │ 否
  │       ↓
  │  ┌─ 設備X.dept_id == 使用者.dept_id? ──→ ✅ 同部門，可操作
  │  │       │ 否
  │  │       ↓
  │  │  ┌─ device_managers 有 DEVICE 指派? ──→ ✅ 個人指派
  │  │  │       │ 否
  │  │  │       ↓
  │  │  │  ┌─ 設備X.contract_id 有 CONTRACT 指派? ──→ ✅ 契約承辦
  │  │  │  │       │ 否
  │  │  │  │       ↓
  │  │  │  │  ┌─ 設備X.circuit_id 有 CIRCUIT 指派? ──→ ✅ 回路承辦
  │  │  │  │  │       │ 否
  │  │  │  │  │       ↓
  │  │  │  │  │      ❌ 無操作權限
  └──┴──┴──┴──┘       （前端不顯示操作按鈕）
```

查詢是否「看得到」的流程類似，但多一個入口：

```
使用者請求查看設備列表
         │
         ↓
  DataScope 部門層級（現有邏輯）
    ∪
  device_managers 展開的設備清單
    =
  最終可見設備集合
```

---

## 6. 後端實作

### 6-1 DeviceAccessService

```java
@Service
@RequiredArgsConstructor
public class DeviceAccessService {

    private final DeviceManagerRepository managerRepo;
    private final DeviceRepository deviceRepo;
    private final DataScopeHelper dataScopeHelper;

    /**
     * 能否操作此設備（報修、維護、簽核）
     */
    public boolean canOperate(Long deviceId, Long userId) {
        // 短路 1：超級管理員
        if (SecurityContextUtils.isSuperAdmin()) return true;

        Device device = deviceRepo.findById(deviceId)
            .orElseThrow(() -> new BusinessException(DEVICE_NOT_FOUND));

        // 短路 2：同部門
        Long userDeptId = SecurityContextUtils.getCurrentUserDeptId();
        if (device.getDeptId() != null && device.getDeptId().equals(userDeptId)) {
            return true;
        }

        // 短路 3：個人指派（DEVICE → CONTRACT → CIRCUIT）
        return isAssignedManager(device, userId);
    }

    /**
     * 查詢列表時的可見設備 ID 集合（DataScope ∪ 個人指派）
     */
    public Set<Long> getViewableDeviceIds(Long userId) {
        // 路徑 1：DataScope 部門層級（現有）
        Set<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();

        // 路徑 2：個人指派展開
        Set<Long> assignedIds = managerRepo.findAccessibleDeviceIds(
            TenantContext.getTenantId(), userId);

        // 聯集（SQL 層面用 UNION，不在 Java 做）
        return assignedIds;  // 傳給 Repository 做 OR 條件
    }

    private boolean isAssignedManager(Device device, Long userId) {
        String tenantId = TenantContext.getTenantId();

        // DEVICE 粒度
        if (managerRepo.existsByScope(tenantId, "DEVICE", device.getId(), userId)) {
            return true;
        }

        // CONTRACT 粒度
        if (device.getContractId() != null &&
            managerRepo.existsByScope(tenantId, "CONTRACT", device.getContractId(), userId)) {
            return true;
        }

        // CIRCUIT 粒度
        if (device.getCircuitId() != null &&
            managerRepo.existsByScope(tenantId, "CIRCUIT", device.getCircuitId(), userId)) {
            return true;
        }

        return false;
    }
}
```

### 6-2 Repository 查詢（View Scope）

```java
@Query(value = """
    SELECT d.* FROM devices d
    WHERE d.tenant_id = :tenantId
      AND (
        d.dept_id IN (:visibleDeptIds)                           -- DataScope
        OR d.id IN (
          SELECT dm.scope_id FROM device_managers dm
          WHERE dm.tenant_id = :tenantId AND dm.user_id = :userId
            AND dm.scope_type = 'DEVICE'
        )
        OR d.contract_id IN (
          SELECT dm.scope_id FROM device_managers dm
          WHERE dm.tenant_id = :tenantId AND dm.user_id = :userId
            AND dm.scope_type = 'CONTRACT'
        )
        OR d.circuit_id IN (
          SELECT dm.scope_id FROM device_managers dm
          WHERE dm.tenant_id = :tenantId AND dm.user_id = :userId
            AND dm.scope_type = 'CIRCUIT'
        )
      )
    """, nativeQuery = true)
Page<Device> findByFiltersWithAccess(
    @Param("tenantId") String tenantId,
    @Param("visibleDeptIds") Set<Long> visibleDeptIds,
    @Param("userId") Long userId,
    Pageable pageable);
```

### 6-3 DeviceResponse 增加 canOperate 欄位

```java
// DeviceService — 列表查詢後批量計算
public Page<DeviceResponse> listDevices(DeviceQueryParams params, Pageable pageable) {
    Long userId = SecurityContextUtils.getCurrentUserId();
    Set<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();

    Page<Device> devices = deviceRepo.findByFiltersWithAccess(
        TenantContext.getTenantId(), visibleDeptIds, userId, pageable);

    // 批量查詢 device_managers（避免 N+1）
    Set<Long> deviceIds = devices.map(Device::getId).toSet();
    Set<Long> operableIds = deviceAccessService.batchCheckOperate(deviceIds, userId);

    return devices.map(d -> {
        DeviceResponse resp = mapper.toResponse(d);
        resp.setCanOperate(operableIds.contains(d.getId()));
        return resp;
    });
}
```

---

## 7. 前端配合

### 7-1 設備列表 — 按鈕控制

```vue
<template>
  <el-table-column label="操作" width="280">
    <template #default="{ row }">
      <!-- 所有人都能看詳情 -->
      <el-button link @click="viewDetail(row)">查看</el-button>

      <!-- 只有 canOperate 才顯示操作按鈕 -->
      <el-button v-if="row.canOperate" link type="primary"
        @click="createFault(row)">報修</el-button>
      <el-button v-if="row.canOperate" link type="warning"
        @click="editDevice(row)">編輯</el-button>

      <!-- 只有 ADMIN / DEPT_ADMIN 才顯示指派按鈕 -->
      <el-button v-if="hasRole('ADMIN', 'DEPT_ADMIN')" link type="info"
        @click="openAssignDialog(row)">指派承辦人</el-button>
    </template>
  </el-table-column>
</template>
```

### 7-2 設備詳情頁

```vue
<template>
  <div class="device-detail">
    <!-- 基本資訊：所有人都能看 -->
    <DeviceInfoPanel :device="device" />

    <!-- 歷程：所有人都能看 -->
    <DeviceEventTimeline :deviceId="device.id" />

    <!-- 操作區：只有 canOperate 才顯示 -->
    <div v-if="device.canOperate" class="action-bar">
      <el-button type="primary" @click="createFaultTicket">
        建立障礙工單
      </el-button>
      <el-button @click="editDevice">
        編輯設備資訊
      </el-button>
    </div>

    <!-- 沒有操作權限時的提示 -->
    <el-alert v-else type="info" :closable="false">
      您僅有查看權限，如需操作請聯繫該設備的所屬單位或承辦人
    </el-alert>

    <!-- 指派承辦人按鈕：只有 ADMIN / DEPT_ADMIN 顯示 -->
    <el-button v-if="hasRole('ADMIN', 'DEPT_ADMIN')" type="info" plain
      @click="openAssignDialog">
      <el-icon><UserPlus /></el-icon> 指派承辦人
    </el-button>
  </div>
</template>
```

### 7-3 指派承辦人 Dialog（ADMIN 專用）

```
┌──────────────────────────────────────────┐
│  指派承辦人                        [✕]   │
├──────────────────────────────────────────┤
│                                          │
│  指派範圍：                               │
│  ◉ 此設備 (L-001)                        │
│  ○ 此設備所屬契約 (契約A — 含500台設備)   │
│  ○ 此設備所屬回路 (C-001 — 含30台設備)   │
│                                          │
│  選擇承辦人：                             │
│  🔍 [搜尋姓名或帳號          ]            │
│                                          │
│  │ ☐ │ 姓名     │ 帳號     │ 部門     │  │
│  │ ☑ │ 張工程師 │ zhang01  │ 工程股   │  │
│  │ ☐ │ 林專員   │ lin02    │ 行政股   │  │
│  │ ☐ │ 陳技術員 │ chen03   │ 管理中心 │  │
│                                          │
│  ⓘ 指派後，該人員可看到並操作指派範圍     │
│    內的所有設備（報修、維護、簽核）        │
│                                          │
│              [取消]  [確認指派]            │
└──────────────────────────────────────────┘
```

**操作流程**：
1. ADMIN 在設備列表或詳情頁點「指派承辦人」
2. Dialog 預設「此設備」，但可切換到契約/回路（若設備有歸屬）
3. 搜尋並勾選人員 → 確認
4. 後端寫入 `device_managers`，即時生效

**契約/回路詳情頁也有此按鈕**：
- 契約詳情頁 → 「指派承辦人」→ scope_type 固定為 CONTRACT
- 回路詳情頁 → 「指派承辦人」→ scope_type 固定為 CIRCUIT
- 不用選範圍，更簡潔

---

## 8. 承辦人自己的視角：「我的負責設備」

### 8-1 入口位置

承辦人登入後，在**側邊欄「資產管理」下新增子選單**：

```
35 資產管理
├── 36 設備管理           DEVICE_VIEW
├── 37 回路管理           CIRCUIT_VIEW
├── 38 障礙工單           FAULT_VIEW
├── 39 契約管理           CONTRACT_VIEW
├── 53 我的負責設備       (登入即可見，無需特殊權限)  ← 新增
```

> menu_id=53。任何被指派的人都看到此選單；未被指派任何設備的人，此選單隱藏（前端判斷）。

### 8-2 頁面 Mockup

```
┌──────────────────────────────────────────────────────────────┐
│  我的負責設備                                                 │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  📊 統計卡片                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ 負責設備  │ │ 待處理工單│ │ 本月結案  │ │ 逾期未結  │       │
│  │   530     │ │    3     │ │   12     │ │    1 ⚠️  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
│                                                               │
├──────────────────────────────────────────────────────────────┤
│  指派來源分組：                                               │
│                                                               │
│  📋 契約A — 北區路燈維護 (500台)                    [展開 ▾]  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ │ 設備編號 │ 設備名稱   │ 類型  │ 狀態  │ 待處理 │ 操作│  │
│  │ │ L-001   │ 忠孝東路01 │ 燈具  │ 使用中│  1    │ [詳情]│  │
│  │ │ L-002   │ 忠孝東路02 │ 燈具  │ 使用中│  —    │ [詳情]│  │
│  │ │ ...     │ (共500台)  │       │       │       │      │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                               │
│  📋 回路 C-001 — 信義區回路 (30台)                  [展開 ▾]  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ │ L-100   │ 信義路01   │ 燈具  │ 使用中│  —    │ [詳情]│  │
│  │ │ ...     │ (共30台)   │       │       │       │      │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                               │
│  📋 直接指派 (2台)                                  [展開 ▾]  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ │ L-VIP-1 │ 市府廣場燈 │ 燈具  │ 使用中│  —    │ [詳情]│  │
│  │ │ L-VIP-2 │ 市府大門燈 │ 燈桿  │ 使用中│  1    │ [詳情]│  │
│  └────────────────────────────────────────────────────────┘  │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

### 8-3 頁面功能說明

| 區塊 | 說明 |
|------|------|
| 統計卡片 | 快速掌握負責範圍的概況 — 多少設備、多少待處理工單、逾期警示 |
| 指派來源分組 | 按 scope_type 分組顯示（CONTRACT → CIRCUIT → DEVICE），一眼知道「為什麼這些設備歸我」 |
| 待處理欄 | 統計該設備有幾張 OPEN / IN_PROGRESS 的工單，讓承辦人優先處理 |
| 詳情按鈕 | 點擊跳轉到設備詳情頁（承辦人有 canOperate 權限，可直接操作） |

### 8-4 API

```
GET /v1/device-managers/my-devices
```

**Response**：
```json
{
  "summary": {
    "totalDevices": 532,
    "pendingTickets": 3,
    "closedThisMonth": 12,
    "overdueTickets": 1
  },
  "groups": [
    {
      "scopeType": "CONTRACT",
      "scopeId": 1,
      "scopeName": "契約A — 北區路燈維護",
      "deviceCount": 500,
      "devices": [
        {
          "id": 1, "deviceCode": "L-001", "deviceName": "忠孝東路01",
          "deviceType": "LUMINAIRE", "status": "ACTIVE",
          "pendingTicketCount": 1, "canOperate": true
        }
      ]
    },
    {
      "scopeType": "CIRCUIT",
      "scopeId": 5,
      "scopeName": "回路 C-001 — 信義區",
      "deviceCount": 30,
      "devices": [...]
    },
    {
      "scopeType": "DEVICE",
      "scopeId": null,
      "scopeName": "直接指派",
      "deviceCount": 2,
      "devices": [...]
    }
  ]
}
```

---

## 9. 管理員視角：查看某人被指派了什麼

### 9-1 入口位置

在**使用者管理頁面**（既有），點擊某使用者 → 使用者詳情 → 新增「負責設備」Tab：

```
┌──────────────────────────────────────────────────┐
│  使用者：張工程師 (zhang01)                       │
├──────────────────────────────────────────────────┤
│  Tab: 基本資料 │ 角色權限 │ 負責設備              │
│                                                   │
│  負責設備 Tab:                                     │
│  ┌────────────────────────────────────────────┐   │
│  │ │ 指派範圍       │ 設備數 │ 指派日期   │操作│   │
│  │ │ 契約A(北區維護) │  500  │ 2026-04-22│[移除]│  │
│  │ │ 回路C-001      │   30  │ 2026-04-20│[移除]│  │
│  │ │ L-VIP-001      │    1  │ 2026-04-15│[移除]│  │
│  │                                             │   │
│  │ [+ 新增指派]                                 │   │
│  └────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
```

**不需要獨立的「承辦人管理」選單** — ADMIN 有兩個自然入口：
1. 從**設備/契約/回路詳情頁** → 「指派承辦人」按鈕（正向：選設備 → 指人）
2. 從**使用者詳情頁** → 「負責設備」Tab（反向：選人 → 看他管什麼）
│  │                                             │   │
---

## 10. API 端點

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/v1/device-managers?scopeType=CONTRACT&scopeId={id}` | DEVICE_MANAGE | 查詢某範圍的承辦人清單 |
| POST | `/v1/device-managers` | DEVICE_MANAGE | 新增承辦人指派（ADMIN/DEPT_ADMIN） |
| DELETE | `/v1/device-managers/{id}` | DEVICE_MANAGE | 移除承辦人指派 |
| GET | `/v1/device-managers/my-devices` | (登入即可) | 承辦人查詢自己負責的設備（分組 + 統計） |
| GET | `/v1/device-managers/by-user/{userId}` | DEVICE_MANAGE | ADMIN 查詢某使用者被指派了什麼 |

---

## 10. 與現有機制的關係

```
┌─────────────────────────────────────────────────────────┐
│                      存取控制全景                         │
│                                                          │
│  Layer 1: JWT 認證                                       │
│    → 誰登入了？屬於哪個 tenant？                         │
│                                                          │
│  Layer 2: RBAC (roles + permissions)                     │
│    → 有沒有 DEVICE_VIEW / DEVICE_MANAGE / FAULT_MANAGE？ │
│    → 沒有 permission → 整個功能模組看不到                 │
│                                                          │
│  Layer 3: DataScope + DeviceManagers (本文件新增)         │
│    → 在有 permission 的前提下：                           │
│    → View: DataScope(部門) ∪ DeviceManagers(指派)        │
│    → Operate: 同部門 ∪ DeviceManagers(指派)              │
│    → SUPER_ADMIN 跳過此層                                │
│                                                          │
│  Layer 4: Workflow (03 簽核引擎)                          │
│    → 在有操作權限的前提下：                               │
│    → 工單流轉需按照流程走（派工→施工→送審→結案）          │
│    → assigned_to 控制「當前這一步誰負責」                 │
│                                                          │
│  總結：                                                   │
│  JWT → 你是誰                                            │
│  RBAC → 你能用什麼功能                                    │
│  DataScope + Managers → 你能看/操作哪些設備               │
│  Workflow → 這張工單現在輪到誰                            │
└─────────────────────────────────────────────────────────┘
```

---

## 11. Flyway Migration

此表放在 **V30**（與 devices / circuits 同批建立），因為它 FK 依賴 devices 和 users 都已存在。

```
V30 建表順序更新：
  1. contracts
  2. devices
  3. circuits
  4. ALTER devices ADD FK → circuits
  5. device_events
  6. device_managers  ← 新增
```
V32 選單新增 menu_id=53「我的負責設備」：

```sql
INSERT INTO menus (menu_id, menu_name, path, parent_menu_id, sort_order, icon, is_active) VALUES
(53, '我的負責設備', '/admin/asset/my-devices', 35, 5, 'UserCheck', true);
-- parent_menu_id=35 (資產管理)，排在契約管理之後
-- 無需特殊 permission，登入且有指派紀錄即可見
```
---

## 12. 決策記錄

| # | 決策 | 理由 |
|---|------|------|
| D-7a | View 和 Operate **拆成兩層** | 上級能看不能動；承辦人跨部門能動 |
| D-7b | 用 `device_managers` 表而非擴充 DataScope | DataScope 是部門維度的通用機制，不適合塞個人指派邏輯；分開維護更清晰 |
| D-7c | scope_type 支援三種粒度 | CONTRACT 最常用（一筆覆蓋整約設備）；CIRCUIT 次之；DEVICE 做精細調整 |
| D-7d | SUPER_ADMIN 不受限 | 緊急狀況需能操作所有設備 |
| D-7e | canOperate 由後端計算、前端只讀 | 避免前端偽造權限；後端是最終防線 |
| D-7f | 「指派承辦人」按鈕只有 ADMIN / DEPT_ADMIN 顯示 | 指派是管理行為，一般操作人員不應自己指派 |
| D-7g | 承辦人視角放在側邊欄「我的負責設備」(menu 53) | 承辦人登入後第一件事就是看自己負責什麼，需要一級入口 |
| D-7h | 不做獨立的「承辦人管理」頁面 | 指派操作語境永遠是「這個設備/契約/回路要指給誰」，放在各自詳情頁最自然；反向查詢放在使用者詳情頁 |
| D-7i | 承辦人起案 → **承辦人的單位主管審核**（非設備所屬單位主管） | 起案人對自己的管理鏈負責；避免跨部門權力衝突（A 主管不認識 B 的人） |
| D-7j | 設備所屬單位收到**非阻塞的知會通知** | 設備歸 A 但由 B 承辦人起案時，A 單位收到副本通知（不阻塞流程、不需審核），保持 A 對自己設備的掌握度 |

---

## 13. 跨部門承辦人簽核路由

### 13-1 核心原則

> **起案人對自己的管理鏈負責，設備所屬單位收到知會。**

### 13-2 情境對比

| 情境 | 起案人 | 設備所屬 | 審核人 | 設備所屬單位 |
|------|--------|----------|--------|-------------|
| A 正常 | A 單位同仁 | A 單位設備 | **A 單位主管** | 同一部門，無需額外通知 |
| B 承辦人 | B 單位承辦人 | A 單位設備 | **B 單位主管** | **A 單位收到知會通知**（不阻塞） |

### 13-3 實作方式

```java
// WorkflowServiceImpl — 建立 workflow_instance 時
WorkflowInstance instance = WorkflowInstance.builder()
    .creatorId(currentUserId)             // 起案人（可能是跨部門承辦人）
    .creatorDeptId(currentUserDeptId)     // 起案人所屬部門
    .ticketType(ticketType)
    .ticketId(ticketId)
    .build();

// 需要審核時，resolver 邏輯：
// 找 creatorDeptId 中擁有 required_role 的人
Long approverId = userService.findByDeptAndRole(
    instance.getCreatorDeptId(),
    step.getRequiredRole()  // e.g., OPERATOR, DEPT_ADMIN
);
```

### 13-4 知會通知

```java
// WorkflowServiceImpl.transition() — 起案時額外通知設備所屬單位
if (!device.getDeptId().equals(creator.getDeptId())) {
    // 起案人 ≠ 設備部門 → 知會設備所屬單位的 DEPT_ADMIN
    Long ownerDeptAdmin = userService.findByDeptAndRole(
        device.getDeptId(), "DEPT_ADMIN");
    notificationService.push(ownerDeptAdmin, "CROSS_DEPT_TICKET_NOTICE",
        String.format("您單位的設備 %s 已由 %s (%s) 建立工單 %s",
            device.getDeviceCode(),
            creator.getDisplayName(),
            creator.getDeptName(),
            ticket.getTicketNo()));
}
```

### 13-5 設備狀態連動

工單流程中，設備狀態自動隨工作流事件推進：

```
ACTIVE（正常）
  │
  │ E1：障礙申告審核通過
  ↓
REPORTED（已報修）── 等待派工
  │
  │ E4：報修派工（dispatch 建立）
  ↓
UNDER_REPAIR（維修中）── 施工進行中
  │
  │ E9/E10：結案審核通過
  ↓
ACTIVE（正常）── 恢復運作
```

> 這三個狀態轉換完全由 EventListener 自動觸發，使用者不可手動設定。
> 詳見 [phase-1-foundation.md](phase-1-foundation.md) §1-2a 設備狀態生命週期。
