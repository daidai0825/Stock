# 業務錯誤碼對照表（Backend ↔ Frontend）

> **版本**：Wave 2 Round 1（2026-04-22）
> **依據**：SRS §9 業務碼分段、`api-design.md`
> **維護者**：Bruno（Backend）
> **對接**：Felix（Frontend）
> **檔案來源**：`backend/stock-common/src/main/java/tw/com/stockplatform/common/constant/ErrorCode.java`

---

## 設計原則

1. 所有 REST API 一律 **HTTP 200**（除非網路層錯誤）
2. 業務狀態以 `code` 表達：成功 = `0`，錯誤 = 對應業務碼
3. 回應使用 Envelope Pattern：`{ code, message, data, errors?, timestamp, traceId }`
4. 前端依 `code` 分流，不依 HTTP status

---

## 分段總覽

| 範圍 | 用途 | 前端建議行為 |
|------|------|--------------|
| `0` | 成功 | 取 `data` |
| `1xxx` | 參數校驗錯誤 | 顯示欄位錯誤（取 `errors[]`） |
| `2xxx` | 業務邏輯錯誤 | 顯示對應 toast / modal |
| `3xxx` | 權限相關錯誤 | 3001/3002/3003 導回登入；3002 觸發 refresh；3004 顯示權限不足 |
| `4xxx` | 資源相關錯誤 | 顯示「查無資料」之類提示 |
| `5xxx` | 第三方服務錯誤 | 提示稍後再試，可重試 |
| `9xxx` | 系統錯誤 | 提示稍後再試，回報問題 |

---

## 完整錯誤碼清單

### 0 — 成功

| Code | Constant | Message | 說明 |
|------|----------|---------|------|
| `0` | `SUCCESS` | success | 業務成功 |

---

### 1xxx — 參數校驗錯誤

| Code | Constant | Message | 前端建議 |
|------|----------|---------|----------|
| `1001` | `PARAM_REQUIRED` | 必填參數缺失 | 欄位必填提示 |
| `1002` | `PARAM_FORMAT_INVALID` | 參數格式錯誤 | 欄位格式錯誤提示（會帶 `errors[]`） |
| `1003` | `PARAM_OUT_OF_RANGE` | 參數值超出範圍 | 欄位範圍錯誤 |
| `1004` | `PARAM_TOO_LONG` | 參數長度超過上限 | 欄位長度錯誤 |

---

### 2xxx — 業務邏輯錯誤

| Code | Constant | Message | 前端建議 |
|------|----------|---------|----------|
| `2010` | `EMAIL_ALREADY_REGISTERED` | Email 已被註冊 | 註冊頁顯示錯誤 |
| `2011` | `EMAIL_OR_PASSWORD_INCORRECT` | Email 或密碼錯誤 | 登入失敗提示（不揭露是 email 還密碼） |
| `2012` | `EMAIL_NOT_VERIFIED` | Email 尚未驗證 | 引導重發驗證信 |
| `2013` | `VERIFY_TOKEN_EXPIRED` | 驗證 Token 已過期 | 引導重發驗證信 |
| `2014` | `VERIFY_TOKEN_INVALID` | 驗證 Token 無效 | 顯示連結無效 |
| `2015` | `VERIFY_RESEND_LIMIT` | 24 小時內驗證信寄送已達 5 次上限 | 顯示限制 |
| `2016` | `OLD_PASSWORD_INCORRECT` | 舊密碼錯誤 | 修改密碼頁顯示錯誤 |
| `2020` | `WATCHLIST_ALREADY_EXISTS` | 該股票已在自選股清單 | toast 提示 |
| `2021` | `WATCHLIST_LIMIT_REACHED` | 自選股已達上限 50 檔 | toast 提示 |
| `2022` | `WATCHLIST_GROUP_LIMIT` | 自訂分組已達上限 10 個 | toast 提示 |
| `2030` | `ALERT_DUPLICATE` | 已存在相同提醒條件 | toast 提示 |
| `2031` | `ALERT_DELETED` | 提醒條件已被刪除 | 重新載入列表 |
| `2040` | `ACCOUNT_CLOSED` | 該帳號已被註銷 | 阻擋登入並提示 |
| `2041` | `ACCOUNT_CLOSE_EXPIRED` | 註銷申請已超過 30 日，無法復原 | 提示無法復原 |

---

### 3xxx — 權限錯誤（重要）

| Code | Constant | Message | 前端建議 |
|------|----------|---------|----------|
| `3001` | `UNAUTHORIZED` | 未登入 | 完全沒帶 Authorization header → 導回登入 |
| `3002` | `TOKEN_EXPIRED` | Token 已過期 | **觸發 refresh token 流程**；失敗才導回登入 |
| `3003` | `TOKEN_INVALID` | Token 無效或已被撤銷 | 直接導回登入（簽章不符 / 格式錯誤） |
| `3004` | `FORBIDDEN` | 無權限執行此操作 | 顯示「權限不足」提示；**不**導回登入（身份有效） |
| `3010` | `ACCOUNT_SUSPENDED` | 帳號已被停權 | 顯示停權說明 |
| `3011` | `ACCOUNT_DELETED` | 帳號已註銷 | 顯示註銷說明 |

> **追蹤埋點區分**：`3001` / `3002` / `3003` 三者前端最終雖都可能導回登入頁，但**埋點需區分**便於後續分析。
>
> **3004 FORBIDDEN**：Wave 2 起 RBAC（Role-Based Access Control）上線後實際使用。目前（Wave 1）尚無任何 Controller 拋出此碼；前端 `errorCodes.ts` 已預先對齊，後端 enum 同步定義以保持介面一致。

---

### 4xxx — 資源錯誤

| Code | Constant | Message | 前端建議 |
|------|----------|---------|----------|
| `4001` | `STOCK_NOT_FOUND` | 查無此股票 | 「找不到股票」空狀態 |
| `4002` | `USER_NOT_FOUND` | 查無此使用者 | 一般不該發生，回報問題 |
| `4003` | `ALERT_NOT_FOUND` | 查無此提醒條件 | 重新載入列表 |
| `4004` | `NOTIFY_RECORD_NOT_FOUND` | 查無此推播紀錄 | 重新載入列表 |
| `4010` | `STOCK_DELISTED` | 該股票已下市 | 顯示下市提示 |

---

### 5xxx — 第三方服務錯誤

| Code | Constant | Message | 前端建議 |
|------|----------|---------|----------|
| `5001` | `EMAIL_SERVICE_ERROR` | Email 寄送服務異常 | 提示稍後再試 |
| `5002` | `WEB_PUSH_SERVICE_ERROR` | Web Push 服務異常 | 提示稍後再試 |
| `5010` | `TWSE_DATA_SOURCE_ERROR` | TWSE 資料源暫時無法存取 | 顯示資料來源異常（僅 TWSE） |
| `5011` | `MOPS_DATA_SOURCE_ERROR` | MOPS 資料源暫時無法存取 | 顯示資料來源異常 |
| `5012` | `CHIP_DATA_SOURCE_ERROR` | 籌碼資料源暫時無法存取 | 顯示資料來源異常（保留，未來 CHIP 特定錯誤） |
| `5013` | `OTC_DATA_SOURCE_ERROR` | OTC 資料源暫時無法存取 | 顯示資料來源異常（僅 OTC/TPEX） |
| `5014` | `MOPS_FORMAT_CHANGED` | MOPS API 格式異動，請通知維運團隊 | 立即通知維運；屬系統型告警，不應頻繁出現 |

---

### 9xxx — 系統錯誤

| Code | Constant | Message | 前端建議 |
|------|----------|---------|----------|
| `9001` | `SYSTEM_BUSY` | 系統繁忙，請稍後再試 | 提示稍後再試（一般性系統忙線）|
| `9002` | `SYSTEM_MAINTENANCE` | 系統維護中 | 顯示維護告示 |
| `9003` | `FEATURE_NOT_AVAILABLE` | 功能尚未開放，敬請期待 | **placeholder API 專用**（Wave 2+ 上線前的預設回應） |
| `9999` | `UNKNOWN_ERROR` | 未知錯誤 | 提示稍後再試，回報問題 |

> **注意**：原本 placeholder controllers 一律回 `9001`，現已改為 `9003`，與真實「系統繁忙」區分。

---

## 跨團隊提醒（給 Felix）

### 1. JWT 例外分流（BE-02 已修復）

| 情境 | 後端回應 | 前端建議 |
|------|----------|----------|
| 沒帶 Authorization header | `3001 UNAUTHORIZED` | 導回登入頁（埋點：unauthenticated） |
| Token 過期 | `3002 TOKEN_EXPIRED` | 觸發 refresh token 流程；失敗才導回登入 |
| 簽章不符 / 格式錯誤 | `3003 TOKEN_INVALID` | 直接導回登入（埋點：tampered token） |

> Wave 1 階段 refresh token 尚未實作（TD-1），收到 3002 暫時也是導回登入，但前端應先寫好分支。

### 2. Placeholder API（BE-M02 已修復）

呼叫 9 個未實作模組的 `/_status` 端點會收到：

```json
{
  "code": 9003,
  "message": "功能尚未開放，敬請期待",
  "timestamp": "2026-04-22T10:30:00.000+08:00",
  "traceId": "..."
}
```

請與真正的 `9001 系統繁忙` 區分：`9003` 表示「該模組 Wave 1 尚未實作」，不是錯誤。

### 3. 參數校驗回應結構

`1002 PARAM_FORMAT_INVALID` 會額外帶 `errors[]`：

```json
{
  "code": 1002,
  "message": "參數格式錯誤",
  "errors": [
    { "field": "email", "message": "格式不正確" },
    { "field": "password", "message": "長度不足 8 字元" }
  ],
  "timestamp": "...",
  "traceId": "..."
}
```

### 4. trace 對齊

所有回應都會帶 `traceId`，前端在錯誤回報時請一併附上 traceId 方便後端定位。
