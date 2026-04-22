# 錯誤碼中央註冊表

| 項目 | 內容 |
|------|------|
| 文件版本 | v1.1 |
| 撰寫者 | Peter（產品經理） |
| 撰寫日期 | 2026-04-22 |
| 狀態 | **LOCKED（正式鎖定）** |
| 變更依據 | Jamie 仲裁 D4（2026-04-22）：廢除 `5101 TWSE_UNAVAILABLE`，採後端 ErrorCode.java 體系；新增 `5013`、`5014` |

---

## 重要聲明

> **本表為唯一事實源。**
>
> - Bruno 的 `backend/stock-common/.../constant/ErrorCode.java` 必須與本表完全一致
> - Felix 的 `frontend/src/constants/errorCodes.ts` 必須與本表完全一致
> - **新增 / 修改 / 廢除任何錯誤碼，必須先更新本表並通知 Jamie**，再同步更新 Java enum 與 TypeScript const
> - 違反此流程的 PR 不得 merge（Brian / Fiona 有責任在 Review 時比對本表）

---

## 分段總覽

| 範圍 | 用途 | 前端建議行為 |
|------|------|-------------|
| `0` | 成功 | 取 `data` 欄位 |
| `1xxx` | 參數校驗錯誤 | 顯示欄位錯誤（取 `errors[]`） |
| `2xxx` | 業務邏輯錯誤 | 顯示對應 toast / modal |
| `3xxx` | 權限相關錯誤 | 3001/3003 導回登入頁；3002 觸發 refresh token 流程；3004 顯示「權限不足」；3010/3011 顯示帳號狀態說明 |
| `4xxx` | 資源相關錯誤 | 顯示「查無資料」空狀態 |
| `5xxx` | 第三方服務錯誤 | 提示「資料來源暫時無法使用，請稍後再試」，可自動重試 |
| `9xxx` | 系統錯誤 | 提示「系統繁忙，請稍後再試」或顯示維護告示 |

---

## 完整錯誤碼清單

### 0 — 成功

| Code | Constant | Message (zh-TW) | Message (en) | 段別 | 用途 | 前端建議行為 |
|------|----------|-----------------|--------------|------|------|-------------|
| `0` | `SUCCESS` | success | success | 成功 | 業務成功 | 取 `data` 欄位 |

---

### 1xxx — 參數校驗錯誤

| Code | Constant | Message (zh-TW) | Message (en) | 段別 | 用途 | 前端建議行為 |
|------|----------|-----------------|--------------|------|------|-------------|
| `1001` | `PARAM_REQUIRED` | 必填參數缺失 | Required parameter is missing | 參數 | 必填欄位未帶 | 欄位必填提示（搭配 `errors[]`） |
| `1002` | `PARAM_FORMAT_INVALID` | 參數格式錯誤 | Invalid parameter format | 參數 | 格式不符（日期、email 等） | 欄位格式錯誤提示（搭配 `errors[]`） |
| `1003` | `PARAM_OUT_OF_RANGE` | 參數值超出允許範圍 | Parameter value out of range | 參數 | 數值超界、陣列過長 | 欄位範圍錯誤提示 |
| `1004` | `PARAM_TOO_LONG` | 參數長度超過上限 | Parameter too long | 參數 | 字串超長 | 欄位長度錯誤提示 |

---

### 2xxx — 業務邏輯錯誤

| Code | Constant | Message (zh-TW) | Message (en) | 段別 | 用途 | 前端建議行為 |
|------|----------|-----------------|--------------|------|------|-------------|
| `2010` | `EMAIL_ALREADY_REGISTERED` | Email 已被註冊 | Email already registered | 業務 | 重複註冊 | 註冊頁顯示錯誤提示 |
| `2011` | `EMAIL_OR_PASSWORD_INCORRECT` | Email 或密碼錯誤 | Incorrect email or password | 業務 | 登入失敗（刻意不揭露是 email 或密碼哪個錯） | 登入失敗 toast |
| `2012` | `EMAIL_NOT_VERIFIED` | Email 尚未驗證 | Email not verified | 業務 | 未驗證帳號嘗試操作 | 引導至「重發驗證信」頁 |
| `2013` | `VERIFY_TOKEN_EXPIRED` | 驗證連結已過期 | Verification link has expired | 業務 | 驗證信連結逾時 | 引導重發驗證信 |
| `2014` | `VERIFY_TOKEN_INVALID` | 驗證連結無效 | Invalid verification link | 業務 | 連結格式錯誤 / 已使用 | 顯示「連結無效」提示 |
| `2015` | `VERIFY_RESEND_LIMIT` | 24 小時內驗證信已達 5 次上限 | Verification email send limit reached | 業務 | 重發次數超限 | 顯示限制說明與等待時間 |
| `2016` | `OLD_PASSWORD_INCORRECT` | 舊密碼錯誤 | Current password is incorrect | 業務 | 修改密碼舊密碼不符 | 修改密碼頁顯示錯誤 |
| `2020` | `WATCHLIST_ALREADY_EXISTS` | 該股票已在自選股清單 | Stock already in watchlist | 業務 | 重複加入自選股 | toast 提示 |
| `2021` | `WATCHLIST_LIMIT_REACHED` | 自選股已達上限 50 檔 | Watchlist limit (50) reached | 業務 | 自選股超出 50 檔 | toast 提示，引導管理清單 |
| `2022` | `WATCHLIST_GROUP_LIMIT` | 自訂分組已達上限 10 個 | Watchlist group limit (10) reached | 業務 | 分組超出 10 個 | toast 提示 |
| `2030` | `ALERT_DUPLICATE` | 已存在相同提醒條件 | Duplicate alert condition | 業務 | 重複設定相同提醒 | toast 提示 |
| `2031` | `ALERT_DELETED` | 提醒條件已被刪除 | Alert condition has been deleted | 業務 | 操作已刪除的提醒 | 重新載入提醒列表 |
| `2040` | `ACCOUNT_CLOSED` | 該帳號已被註銷 | Account has been closed | 業務 | 已註銷帳號嘗試登入 | 阻擋登入並顯示說明 |
| `2041` | `ACCOUNT_CLOSE_EXPIRED` | 註銷申請已超過 30 日，無法復原 | Account close period expired | 業務 | 超過復原期限 | 顯示無法復原說明 |

---

### 3xxx — 權限相關錯誤

| Code | Constant | Message (zh-TW) | Message (en) | 段別 | 用途 | 前端建議行為 |
|------|----------|-----------------|--------------|------|------|-------------|
| `3001` | `UNAUTHORIZED` | 未登入，請先登入 | Not authenticated | 權限 | 未帶 Authorization header 或 token 完全不存在 | 導回登入頁（埋點：unauthenticated） |
| `3002` | `TOKEN_EXPIRED` | 登入憑證已過期 | Access token has expired | 權限 | Access token 超過有效期 | 觸發 refresh token 流程；失敗才導回登入（埋點：token_expired） |
| `3003` | `TOKEN_INVALID` | 登入憑證無效或已被撤銷 | Token is invalid or revoked | 權限 | 簽章不符 / 格式錯誤 / 已加入黑名單 | 直接導回登入（埋點：tampered_token） |
| `3004` | `FORBIDDEN` | 無權限執行此操作 | Forbidden | 權限 | 已登入但無此功能權限（Wave 2+ RBAC 上線後使用） | 顯示「權限不足」提示，不導回登入 |
| `3010` | `ACCOUNT_SUSPENDED` | 帳號已被停權 | Account is suspended | 權限 | 被管理員停權的帳號 | 顯示停權說明與聯絡方式 |
| `3011` | `ACCOUNT_DELETED` | 帳號已被刪除 | Account has been deleted | 權限 | 軟刪除後仍嘗試登入 | 顯示帳號已刪除說明 |

> **埋點注意**：`3001` / `3002` / `3003` 三者前端最終雖都可能導回登入頁，但埋點事件名稱必須區分，便於後續分析是安全事件還是正常過期。

---

### 4xxx — 資源相關錯誤

| Code | Constant | Message (zh-TW) | Message (en) | 段別 | 用途 | 前端建議行為 |
|------|----------|-----------------|--------------|------|------|-------------|
| `4001` | `STOCK_NOT_FOUND` | 查無此股票 | Stock not found | 資源 | DB 無此股票代號且外部來源亦查無 | 顯示「查無股票代號 XXX」空狀態 + 返回按鈕 |
| `4002` | `USER_NOT_FOUND` | 查無此使用者 | User not found | 資源 | 系統內部使用，正常情境不應觸發 | 顯示一般錯誤，回報問題 |
| `4003` | `ALERT_NOT_FOUND` | 查無此提醒條件 | Alert condition not found | 資源 | 操作不存在的提醒 | 重新載入列表 |
| `4004` | `NOTIFY_RECORD_NOT_FOUND` | 查無此推播紀錄 | Notification record not found | 資源 | 查詢不存在的推播紀錄 | 重新載入列表 |
| `4010` | `STOCK_DELISTED` | 該股票已下市 | Stock has been delisted | 資源 | 股票已下市/下櫃 | 顯示下市提示，建議從自選股移除 |

---

### 5xxx — 第三方服務錯誤

| Code | Constant | Message (zh-TW) | Message (en) | 段別 | 用途 | 前端建議行為 |
|------|----------|-----------------|--------------|------|------|-------------|
| `5001` | `EMAIL_SERVICE_ERROR` | Email 寄送服務暫時異常 | Email service is temporarily unavailable | 第三方 | SendGrid / SES 等 Email 寄送失敗 | 提示稍後再試 |
| `5002` | `WEB_PUSH_SERVICE_ERROR` | Web Push 服務暫時異常 | Web push service is temporarily unavailable | 第三方 | Web Push 推播失敗 | 提示稍後再試 |
| `5010` | `TWSE_DATA_SOURCE_ERROR` | TWSE 資料來源暫時無法存取 | TWSE data source is temporarily unavailable | 第三方 | TWSE 外部 API 回傳錯誤 / 逾時 | 顯示「行情資料暫時無法載入」，可顯示最後 fallback 資料（isStale=true） |
| `5011` | `MOPS_DATA_SOURCE_ERROR` | MOPS 資料來源暫時無法存取 | MOPS data source is temporarily unavailable | 第三方 | MOPS 外部 API 回傳錯誤 / 逾時 | 顯示「基本面資料暫時無法載入」 |
| `5012` | `CHIP_DATA_SOURCE_ERROR` | 籌碼資料來源暫時無法存取 | Chip data source is temporarily unavailable | 第三方 | 籌碼外部 API（TWSE/OTC）回傳錯誤 | 顯示「籌碼資料暫時無法載入」 |
| `5013` | `OTC_DATA_SOURCE_ERROR` | OTC 資料來源暫時無法存取 | OTC data source is temporarily unavailable | 第三方 | OTC 外部 API 回傳錯誤 / 逾時 | 同 5010 處理邏輯，顯示 fallback 資料 |
| `5014` | `MOPS_FORMAT_CHANGED` | MOPS 資料格式異動，請聯絡系統管理員 | MOPS data format has changed unexpectedly | 第三方 | MOPS 回應解析失敗（欄位格式異動） | 顯示「資料解析失敗」，不提供重試；需後端工程師介入修復 |

> **廢除記錄**：`5101 TWSE_UNAVAILABLE`（Felix 前端原始定義）已正式廢除，統一改用 `5010 TWSE_DATA_SOURCE_ERROR`。Felix 的 `frontend/src/constants/errorCodes.ts` 必須移除 `TWSE_UNAVAILABLE: 5101` 並改為 `TWSE_DATA_SOURCE_ERROR: 5010`。

---

### 9xxx — 系統錯誤

| Code | Constant | Message (zh-TW) | Message (en) | 段別 | 用途 | 前端建議行為 |
|------|----------|-----------------|--------------|------|------|-------------|
| `9001` | `SYSTEM_BUSY` | 系統繁忙，請稍後再試 | System is busy, please try again later | 系統 | 一般性系統過載 / 未預期錯誤 | toast 提示稍後再試 |
| `9002` | `SYSTEM_MAINTENANCE` | 系統維護中，請稍後再試 | System is under maintenance | 系統 | 排程維護停機 | 顯示維護告示頁 |
| `9003` | `FEATURE_NOT_AVAILABLE` | 功能尚未開放，敬請期待 | This feature is not yet available | 系統 | Placeholder API 回傳（後續 Wave 才實作的功能） | 顯示「即將推出」提示，不顯示錯誤 toast |
| `9999` | `UNKNOWN_ERROR` | 未知錯誤，請稍後再試或回報問題 | Unknown error occurred | 系統 | 所有未分類的例外 | toast 提示並附上 traceId 供回報 |

---

## Wave B 新增 / 修改對照

| 動作 | Code | 說明 |
|------|------|------|
| **新增** | `5013 OTC_DATA_SOURCE_ERROR` | OTC 獨立資料來源錯誤，與 TWSE (5010) 分開（Brian M-BE-W2-09） |
| **新增** | `5014 MOPS_FORMAT_CHANGED` | MOPS 解析失敗專用，需人工介入修復（Brian M-BE-W2-08） |
| **廢除** | `5101 TWSE_UNAVAILABLE` | Felix 前端原始定義，與後端 5010 語意重疊，統一廢除 |

---

## 前後端對應關係

### Java ErrorCode.java（Bruno 維護）

```
ErrorCode.SUCCESS                   = 0
ErrorCode.PARAM_REQUIRED            = 1001
ErrorCode.PARAM_FORMAT_INVALID      = 1002
ErrorCode.PARAM_OUT_OF_RANGE        = 1003
ErrorCode.PARAM_TOO_LONG            = 1004
ErrorCode.EMAIL_ALREADY_REGISTERED  = 2010
ErrorCode.EMAIL_OR_PASSWORD_INCORRECT = 2011
ErrorCode.EMAIL_NOT_VERIFIED        = 2012
ErrorCode.VERIFY_TOKEN_EXPIRED      = 2013
ErrorCode.VERIFY_TOKEN_INVALID      = 2014
ErrorCode.VERIFY_RESEND_LIMIT       = 2015
ErrorCode.OLD_PASSWORD_INCORRECT    = 2016
ErrorCode.WATCHLIST_ALREADY_EXISTS  = 2020
ErrorCode.WATCHLIST_LIMIT_REACHED   = 2021
ErrorCode.WATCHLIST_GROUP_LIMIT     = 2022
ErrorCode.ALERT_DUPLICATE           = 2030
ErrorCode.ALERT_DELETED             = 2031
ErrorCode.ACCOUNT_CLOSED            = 2040
ErrorCode.ACCOUNT_CLOSE_EXPIRED     = 2041
ErrorCode.UNAUTHORIZED              = 3001
ErrorCode.TOKEN_EXPIRED             = 3002
ErrorCode.TOKEN_INVALID             = 3003
ErrorCode.FORBIDDEN                 = 3004
ErrorCode.ACCOUNT_SUSPENDED         = 3010
ErrorCode.ACCOUNT_DELETED           = 3011
ErrorCode.STOCK_NOT_FOUND           = 4001
ErrorCode.USER_NOT_FOUND            = 4002
ErrorCode.ALERT_NOT_FOUND           = 4003
ErrorCode.NOTIFY_RECORD_NOT_FOUND   = 4004
ErrorCode.STOCK_DELISTED            = 4010
ErrorCode.EMAIL_SERVICE_ERROR       = 5001
ErrorCode.WEB_PUSH_SERVICE_ERROR    = 5002
ErrorCode.TWSE_DATA_SOURCE_ERROR    = 5010
ErrorCode.MOPS_DATA_SOURCE_ERROR    = 5011
ErrorCode.CHIP_DATA_SOURCE_ERROR    = 5012
ErrorCode.OTC_DATA_SOURCE_ERROR     = 5013   // Wave B 新增
ErrorCode.MOPS_FORMAT_CHANGED       = 5014   // Wave B 新增
ErrorCode.SYSTEM_BUSY               = 9001
ErrorCode.SYSTEM_MAINTENANCE        = 9002
ErrorCode.FEATURE_NOT_AVAILABLE     = 9003
ErrorCode.UNKNOWN_ERROR             = 9999
```

### TypeScript ErrorCode（Felix 維護）

```typescript
export const ErrorCode = {
  SUCCESS: 0,

  // 參數校驗
  PARAM_REQUIRED: 1001,
  PARAM_FORMAT_INVALID: 1002,
  PARAM_OUT_OF_RANGE: 1003,
  PARAM_TOO_LONG: 1004,

  // 業務邏輯
  EMAIL_ALREADY_REGISTERED: 2010,
  EMAIL_OR_PASSWORD_INCORRECT: 2011,
  EMAIL_NOT_VERIFIED: 2012,
  VERIFY_TOKEN_EXPIRED: 2013,
  VERIFY_TOKEN_INVALID: 2014,
  VERIFY_RESEND_LIMIT: 2015,
  OLD_PASSWORD_INCORRECT: 2016,
  WATCHLIST_ALREADY_EXISTS: 2020,
  WATCHLIST_LIMIT_REACHED: 2021,
  WATCHLIST_GROUP_LIMIT: 2022,
  ALERT_DUPLICATE: 2030,
  ALERT_DELETED: 2031,
  ACCOUNT_CLOSED: 2040,
  ACCOUNT_CLOSE_EXPIRED: 2041,

  // 權限
  UNAUTHORIZED: 3001,
  TOKEN_EXPIRED: 3002,
  TOKEN_INVALID: 3003,
  FORBIDDEN: 3004,
  ACCOUNT_SUSPENDED: 3010,
  ACCOUNT_DELETED: 3011,

  // 資源
  STOCK_NOT_FOUND: 4001,
  USER_NOT_FOUND: 4002,
  ALERT_NOT_FOUND: 4003,
  NOTIFY_RECORD_NOT_FOUND: 4004,
  STOCK_DELISTED: 4010,

  // 第三方
  EMAIL_SERVICE_ERROR: 5001,
  WEB_PUSH_SERVICE_ERROR: 5002,
  TWSE_DATA_SOURCE_ERROR: 5010,    // 注意：廢除原 TWSE_UNAVAILABLE: 5101
  MOPS_DATA_SOURCE_ERROR: 5011,
  CHIP_DATA_SOURCE_ERROR: 5012,
  OTC_DATA_SOURCE_ERROR: 5013,     // Wave B 新增
  MOPS_FORMAT_CHANGED: 5014,       // Wave B 新增

  // 系統
  SYSTEM_BUSY: 9001,
  SYSTEM_MAINTENANCE: 9002,
  FEATURE_NOT_AVAILABLE: 9003,
  UNKNOWN_ERROR: 9999,
} as const;
```

---

## 變更控管

> 本表為 **errorCode 唯一事實源**。
>
> 新增 / 修改 / 廢除錯誤碼的流程：
>
> 1. **必須先更新本表**（`20260422_errorCodes_central.md`）並通知 Jamie
> 2. PR 標題加 `[SRS-CHANGE][errorCode]` 前綴
> 3. Bruno 更新 `ErrorCode.java`、Felix 更新 `errorCodes.ts` 必須在**同一 PR** 提交
> 4. Brian + Fiona 在同一 review cycle 比對本表確認一致性
> 5. 廢除的錯誤碼必須在本表保留「廢除記錄」區段，不可直接刪除（方便追溯）
