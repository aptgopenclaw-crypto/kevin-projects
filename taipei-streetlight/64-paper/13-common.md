# 13. Common 共用模組

## 1. 模組概述

`com.taipei.iot.common` 是整個台北市路燈管理系統的基礎共用模組，提供所有業務模組共享的基礎設施元件，包括：

- **BaseEntity**：JPA 實體基礎超類別（自動 timestamp）
- **ErrorCode**：全域統一錯誤碼枚舉
- **BusinessException**：業務異常
- **BaseResponse**：統一 API 回應格式
- **RateLimit / RateLimitInterceptor**：基於 Redis 的 IP 速率限制
- **FileStorageService / FileValidationService / ImageSanitizer**：檔案上傳安全鏈
- **VirusScanService**：病毒掃描（ClamAV / NoOp）
- **SecurityContextUtils**：從 Spring Security 取得當前使用者資訊
- **SecurityLogger / SecurityEvent**：安全事件結構化日誌
- **UserInfo DTO**：跨模組傳遞使用者上下文

## 2. 資料表結構

本模組不直接擁有資料表，但 `BaseEntity` 為所有實體提供共用欄位：

| 欄位 | 型別 | 說明 |
|------|------|------|
| `created_at` | `TIMESTAMP` | 建立時間，`NOT NULL`、`updatable=false`，由 `AuditingEntityListener` 自動填入 |
| `updated_at` | `TIMESTAMP` | 最後更新時間，由 `AuditingEntityListener` 自動填入 |

## 3. 元件關聯/架構

```
BaseEntity ←── 所有業務實體繼承
    └── @MappedSuperclass + AuditingEntityListener

ErrorCode ←── BusinessException / GlobalExceptionHandler / BaseResponse
    └── 統一 code + httpStatus + message

UserInfo DTO
    └── SecurityContextUtils → 從 JWT Authentication 解析
    └── 被 dept/announcement/tenant 等模組使用

檔案上傳安全鏈：
    Controller → FileValidationService (副檔名+Magic bytes+大小)
              → ImageSanitizer (圖片 re-encode 消毒)
              → VirusScanService (ClamAV 掃描)
              → LocalFileStorageService (磁碟寫入)

速率限制：
    WebMvcConfig → RateLimitInterceptor → @RateLimit 註解
                                        → Redis INCR + EXPIRE
```

## 4. API 端點

本模組無直接 API，但提供基礎設施給所有 Controller 使用。

`@RateLimit` 註解可標註在任意 Controller 方法上：
```java
@RateLimit(key = "login", limit = 10, period = 60)
```

## 5. 業務邏輯/機制說明

### 5.1 BaseEntity 自動審計
- 使用 Spring Data JPA 的 `@CreatedDate` / `@LastModifiedDate`
- 不提供 Setter，防止應用層意外覆寫
- 子類需自行實作 `equals()` / `hashCode()`（基於業務主鍵）

### 5.2 速率限制（RateLimitInterceptor）
- **Redis Key 格式**：`rate_limit:{key}:{clientIp}`
- 首次請求 INCR 建立 key 值為 1，設定 TTL = period 秒
- 超過 limit → 回傳 HTTP 429 + `Retry-After` header
- **安全修復**：使用 `request.getRemoteAddr()`（TCP 層 IP），不信任 `X-Forwarded-For` 等可偽造 header
- Redis 不可用時放行（fail-open），避免阻斷服務

### 5.3 檔案上傳安全鏈
1. **副檔名白名單**：jpg/jpeg/png/gif/bmp/webp/pdf/xlsx/docx/csv/mp4/wav/mp3
2. **Magic Bytes 驗證**：Apache Tika 偵測實際 MIME type，與副檔名交叉比對
3. **檔案大小限制**：預設 10MB（`file.validation.max-size`）
4. **圖片消毒**（ImageSanitizer）：透過 `ImageIO.read()` → `ImageIO.write()` re-encode，消除 EXIF metadata 及 Polyglot payload
5. **病毒掃描**：ClamAV INSTREAM 協定（正式環境）/ NoOp（開發環境）

### 5.4 檔案儲存（LocalFileStorageService）
- 路徑穿越防護：`sanitizeFileName()` 移除 `/`、`\`、`..`、null bytes
- 所有 `resolve()` 後檢查 `startsWith(rootLocation)` 防止目錄逃逸
- 檔案名稱加 UUID 前綴避免覆蓋

### 5.5 UserInfo DTO
封裝當前使用者上下文，跨模組傳遞：

| 欄位 | 說明 |
|------|------|
| `userId` | 使用者 ID |
| `username` | 帳號 |
| `tenantId` | 當前租戶 ID |
| `deptId` | 所屬部門 ID |
| `dataScope` | 資料權限範圍（ALL / THIS_LEVEL / THIS_LEVEL_AND_BELOW） |

## 6. 資料流

### 速率限制流程
```
HTTP Request
  → WebMvcConfig 註冊 RateLimitInterceptor
    → 檢查 handler 是否有 @RateLimit 註解
      → Redis INCR rate_limit:{key}:{ip}
        → count ≤ limit → 放行
        → count > limit → 429 + SecurityLogger.warn(RATE_LIMITED)
```

### 檔案上傳流程
```
MultipartFile
  → FileValidationService.validate()
    → validateSize() → FILE_SIZE_EXCEEDED
    → validateExtension() → FILE_EXTENSION_NOT_ALLOWED
    → validateMagicBytes() → FILE_TYPE_MISMATCH
  → ImageSanitizer.sanitize() (圖片類型)
  → VirusScanService.scan()
    → INFECTED → FILE_VIRUS_DETECTED
    → ERROR → 依策略處理
  → LocalFileStorageService.store()
    → sanitizeFileName() + UUID + 路徑檢查
    → 回傳相對路徑存入 DB
```

## 7. ErrorCode / Enum 定義

### ErrorCode 完整列表

| 代碼區段 | 範圍 | 用途 |
|---------|------|------|
| 00xxx | 通用 | SUCCESS(00000), VALIDATION_ERROR(00001) |
| 10xxx | 認證授權 | ACCESS_TOKEN_INVALID(10001) ~ TENANT_SELECTION_REQUIRED(10022) |
| 10030 | 速率限制 | RATE_LIMIT_EXCEEDED(10030) → HTTP 429 |
| 20xxx | 使用者管理 | USER_NOT_FOUND(20005) ~ MAPPING_NOT_FOUND(20018) |
| 30xxx | RBAC | ROLE_CODE_DUPLICATE(30001) ~ MENU_HAS_CHILDREN(30006) |
| 40xxx | 部門 | DEPT_NOT_FOUND(40001) ~ DEPT_HAS_USERS(40004) |
| 50xxx | 公告 | ANNOUNCEMENT_NOT_FOUND(50001) |
| 55xxx | 通知 | NOTIFICATION_NOT_FOUND(55001) |
| 60xxx | 資產管理 | DEVICE_NOT_FOUND(60001) ~ CONTRACT_NOT_FOUND(60020) |
| 70xxx | 報修維護 | REPAIR_TICKET_NOT_FOUND(70001) ~ INSPECTION_RECORD_NOT_FOUND(70031) |
| 80xxx | 換裝維護 | REPLACEMENT_ORDER_NOT_FOUND(80001) ~ POLE_NUMBER_NOT_FOUND(80021) |
| 85xxx | 材料管理 | MATERIAL_SPEC_NOT_FOUND(85001) ~ MATERIAL_NOT_AVAILABLE(85011) |
| 90xxx | 簽核引擎 | WORKFLOW_INSTANCE_NOT_FOUND(90001) ~ DELEGATE_END_DATE_REQUIRED(90012) |
| 99xxx | 系統 | UNKNOWN_ERROR(99999) → HTTP 500 |

### SecurityEvent 枚舉

| 值 | 說明 |
|----|------|
| `LOGIN_FAILED` | 登入失敗 |
| `CAPTCHA_FAILED` | 驗證碼錯誤 |
| `RATE_LIMITED` | 觸發速率限制 |
| `JWT_INVALID` | JWT 驗證失敗 |
| `ACCESS_DENIED` | 存取被拒 |
| `PASSWORD_RESET_REQUEST` | 密碼重設請求 |
| `SUSPICIOUS_INPUT` | 可疑輸入 |

### VirusScanService.ScanResult 枚舉

| 值 | 說明 |
|----|------|
| `CLEAN` | 檔案安全 |
| `INFECTED` | 偵測到惡意內容 |
| `ERROR` | 掃描服務不可用 |
