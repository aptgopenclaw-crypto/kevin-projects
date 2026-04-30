## Q

後端用的是spring-boot3 , 前端是vue3，不曉得有沒有資安的issue?? 會有哪些檢查方式或其他嗎?

---

## 資安分析結果

### 做得好的部分 ✅

| 項目 | 狀態 |
|---|---|
| **SQL Injection** | 所有查詢都用參數化 (`:param`) — 安全 |
| **密碼加密** | BCrypt + 強度驗證 (大小寫/數字/長度8+/歷史5筆/90天過期) |
| **JWT Token** | HMAC-SHA 簽名、Access Token 30分鐘、Refresh Token 7天 |
| **Token 儲存** | Access Token 存記憶體 (Pinia)、Refresh Token 存 HttpOnly Cookie — 防 XSS |
| **XSS 防護** | 前端無 `v-html` 使用，所有內容透過 Vue 安全渲染 |
| **輸入驗證** | `@Valid` + `@NotBlank` / `@Email` / `@Size` 等 annotation 完整 |
| **帳號鎖定** | 登入失敗 5 次自動鎖定 10 分鐘 |
| **CAPTCHA** | Redis-backed、一次性使用、SecureRandom、5分鐘過期 |
| **多租戶隔離** | Hibernate Filter + TenantContext，自動過濾 |
| **稽核日誌** | 敏感欄位 (password/token) 自動遮罩 `***` |
| **權限控制** | URL-level + Method-level (`@PreAuthorize`) + Menu-based 前端守衛 |
| **Token Refresh** | Singleton pattern 避免多次同時刷新 |

---

### 需要修正的問題 🔴

#### 1. 正式環境設定 (最重要)

application.yml 中有多項**預設值必須在正式環境覆蓋**：

| 設定 | 目前值 | 正式環境應為 | 嚴重度 |
|---|---|---|---|
| `spring.datasource.password` | `postgres` | 強密碼 (環境變數) | 🔴 高 |
| `jwt.secret` | ~56字元預設值 | ≥256 bits (環境變數) | 🔴 高 |
| `auth.cookie.secure` | `false` | `true` (HTTPS only) | 🔴 高 |
| `auth.cookie.same-site` | `Lax` | `Strict` | 🟡 中 |
| CORS `allowedOrigins` | `localhost:5173` | 正式域名 | 🟡 中 |

#### 2. 錯誤訊息洩漏

GlobalExceptionHandler.java 的通用 `Exception` handler 回傳 `ex.getMessage()`，正式環境可能洩漏 stack trace 或內部資訊。建議改為固定的 "Internal Server Error"。

#### 3. Seed Data 密碼註解

V1_1__auth__seed_data.sql 裡有 `-- password = Test1234!` 的註解，上線前應移除。

---

### 建議的檢查方式與工具

#### 靜態分析 (SAST)
| 工具 | 用途 | 費用 |
|---|---|---|
| **SonarQube / SonarCloud** | Java + Vue 程式碼品質 + 漏洞掃描 | Community 免費 |
| **SpotBugs + Find Security Bugs** | Java 安全漏洞偵測 | 免費 |
| **ESLint + eslint-plugin-security** | 前端 JS/TS 安全規則 | 免費 |

#### 依賴漏洞掃描 (SCA)
| 工具 | 用途 |
|---|---|
| **OWASP Dependency-Check** | Maven 套件 CVE 掃描 (加入 pom.xml plugin) |
| **`npm audit`** | 前端套件漏洞檢查 (直接執行) |
| **Snyk / Dependabot** | 自動監控依賴漏洞 + PR 修復 |

#### 動態測試 (DAST)
| 工具 | 用途 |
|---|---|
| **OWASP ZAP** | 自動化 Web 應用滲透測試 |
| **Burp Suite** | 手動 + 自動化 API 安全測試 |

#### 快速可執行的檢查

```bash
# 後端 - 依賴漏洞檢查
cd backend && mvn org.owasp:dependency-check-maven:check

# 前端 - 依賴漏洞檢查  
cd frontend && npm audit

# 前端 - 檢查有無 v-html 等危險寫法
grep -r "v-html\|innerHTML" frontend/src/
```

---

### 總結

你的專案在安全架構上**做得相當扎實** — JWT + HttpOnly Cookie 的組合、參數化查詢、輸入驗證、帳號鎖定、多租戶隔離等都有到位。主要風險集中在**正式環境設定**（密碼、JWT secret、Cookie flags）以及通用 Exception handler 可能洩漏資訊這兩點。建議優先處理這些設定問題，再搭配 OWASP Dependency-Check 和 `npm audit` 做依賴掃描。