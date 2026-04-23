# Schema Lock：Wave 3 API Contract

| 項目 | 內容 |
|------|------|
| 文件版本 | v1.0 |
| 撰寫者 | Peter（PM） |
| 撰寫日期 | 2026-04-23 |
| 狀態 | **LOCKED（正式鎖定）** |
| 仲裁依據 | Jamie 決策 D-05 / D-06 / D-07 / D-08（2026-04-23） |
| 適用範圍 | Wave 3 全部 17 支端點 + Wave 3 新增 errorCode + 新增資料表 schema |
| 下游使用者 | Bruno（後端 DTO 實作）、Felix（前端 types/service）、Brian（後端 Review）、Fiona（前端 Review）、Quincy/Quinn（contract test 基準） |

---

## 0. 全域規範

| 項目 | 規範 |
|------|------|
| HTTP 方法 | POST |
| HTTP 狀態碼 | 200（業務錯誤透過 `code` 區分；**未授權一律 200 + code 3001，禁止使用 HTTP 401**） |
| Content-Type | `application/json; charset=UTF-8` |
| 認證 | `Authorization: Bearer {access_token}`（公開端點除外） |
| 數值精度 | 金融數值（價格 / 比率）：後端 `BigDecimal` 序列化為 `string`（保留精度） |
| 整數類 | 成交量、搜尋次數、排名：序列化為 `number` |
| 時間格式 | ISO 8601 datetime：`YYYY-MM-DDTHH:mm:ss.sss+08:00`；純日期：`YYYY-MM-DD` |
| 時區 | 固定 GMT+8 |
| Envelope 格式 | `{ "code": 0, "message": "success", "data": {...}, "timestamp": "...", "traceId": "..." }` |
| ID 格式 | UUID（VARCHAR(36)），系統產生 |

---

## §1. 端點路徑與認證要求

| # | 路徑 | 認證 | 模組 |
|---|------|------|------|
| 1 | POST /api/v1/stock/search | 否（公開） | Search |
| 2 | POST /api/v1/stock/hot-search | 否（公開） | Search |
| 3 | POST /api/v1/watchlist/add | 是 | Watchlist |
| 4 | POST /api/v1/watchlist/remove | 是 | Watchlist |
| 5 | POST /api/v1/watchlist/list | 是 | Watchlist |
| 6 | POST /api/v1/alert/create | 是 | Alert |
| 7 | POST /api/v1/alert/update-status | 是 | Alert |
| 8 | POST /api/v1/alert/delete | 是 | Alert |
| 9 | POST /api/v1/alert/list | 是 | Alert |
| 10 | POST /api/v1/user/notification-preference | 是 | User |
| 11 | POST /api/v1/push/subscribe | 是 | Push |
| 12 | POST /api/v1/push/unsubscribe | 是 | Push |
| 13 | POST /api/v1/auth/refresh | 否（公開） | Auth |
| 14 | POST /api/v1/auth/logout | 是 | Auth |
| 15 | POST /api/v1/search/history/list | 是 | SearchHistory |
| 16 | POST /api/v1/search/history/remove | 是 | SearchHistory |
| 17 | POST /api/v1/search/history/clear | 是 | SearchHistory |

---

## §2. 未授權回應 Contract（D-08 拍板）

所有需認證端點收到未授權請求時，**必須**回傳：

```json
{
  "code": 3001,
  "message": "未登入，請先登入",
  "timestamp": "2026-04-23T10:30:45.123+08:00",
  "traceId": "abc-123-def"
}
```

HTTP 狀態碼：**200**（禁止使用 401 / 403）

### §2.1 觸發場景 → code 對照

| 場景 | code | message | 是否寫 audit_log |
|------|------|---------|----------------|
| 未帶 Authorization header | 3001 | 未登入，請先登入 | 否 |
| Token 格式錯誤 / 解析失敗 | 3003 | 登入憑證無效或已被撤銷 | 是 |
| Access token exp 超過 | 3002 | 登入憑證已過期 | 否 |
| Token 在 Redis 黑名單中 | 3003 | 登入憑證無效或已被撤銷 | 是 |

### §2.2 Spring Security 實作要點（Bruno）

- `AuthenticationEntryPoint`：覆蓋預設 401 行為，改回 HTTP 200 + code 3001 envelope
- `AccessDeniedHandler`：覆蓋預設 403 行為，改回 HTTP 200 + code 3004 envelope
- JWT Filter：在 `OncePerRequestFilter` 中解析 token，失效時不 throw，而是設定對應 SecurityContext

### §2.3 前端 axios interceptor 要點（Felix）

| 偵測 code | 行為 |
|----------|------|
| 3001 | 清空 accessToken + refreshToken；存 returnUrl；導向 /login |
| 3002 | 觸發 refresh token 流程；成功後重試原請求；失敗走 3001 |
| 3003 | 清空所有 token；埋點 `tampered_token`；導向 /login |

---

## §3. 搜尋相關 API Contract

### §3.1 POST /api/v1/stock/search

**Request**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| keyword | string | Y | 最小 1 字元，最大 50 字元 |
| limit | integer | N | 預設 10，最大 10 |

**Response.data**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| items | StockSearchItem[] | Y | 最多 10 筆 |
| keyword | string | Y | Echo 回傳 |
| total | integer | Y | 命中總數 |

**StockSearchItem 欄位**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號 |
| stockName | string | Y | 中文名稱 |
| stockNameEn | string \| null | Y | 英文名稱（可 null） |
| market | `"TWSE"` \| `"OTC"` | Y | 市場別 |
| matchType | string | Y | `ID_EXACT` / `ID_PREFIX` / `NAME_EXACT` / `NAME_PREFIX` / `NAME_PARTIAL` / `NAME_EN_PREFIX` |

**排序規則（按優先序）**：
1. 代號完全匹配（ID_EXACT）
2. 代號前綴匹配（ID_PREFIX）
3. 中文名稱完全匹配（NAME_EXACT）
4. 中文名稱前綴匹配（NAME_PREFIX）
5. 中文名稱部分匹配（NAME_PARTIAL）
6. 英文名稱前綴匹配（NAME_EN_PREFIX）
- 同優先序內，依 HOT_SEARCH.search_count 降冪，再依 stock_id 升冪

**業務錯誤碼**：

| code | Constant | 觸發條件 |
|------|----------|---------|
| 1001 | PARAM_REQUIRED | keyword 未帶 |
| 1004 | PARAM_TOO_LONG | keyword 超過 50 字元 |

---

### §3.2 POST /api/v1/stock/hot-search

**Request**：空 JSON `{}`（無必填參數）

**Response.data**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| items | HotSearchItem[] | Y | 最多 10 筆 |
| computedAt | string（ISO 8601） | Y | 最近一次排程彙總時間 |

**HotSearchItem 欄位**：

| 欄位 | 型別 | 說明 |
|------|------|------|
| rank | integer | 排名（1-10） |
| stockId | string | 股票代號 |
| stockName | string | 中文名稱 |
| market | `"TWSE"` \| `"OTC"` | 市場別 |
| searchCount | integer | 過去 24 小時搜尋次數（彙總值，搜尋次數 < 3 不出現） |

---

## §4. Watchlist API Contract

### §4.1 POST /api/v1/watchlist/add

**Request**：`{ "stockId": string }`

**Response.data（成功）**：

| 欄位 | 型別 | 說明 |
|------|------|------|
| itemId | string（UUID） | 自選股記錄 ID |
| stockId | string | 股票代號 |
| stockName | string | 股票中文名稱 |
| market | `"TWSE"` \| `"OTC"` | 市場別 |
| createdAt | string（ISO 8601） | 加入時間 |

**業務錯誤碼**：

| code | Constant | 觸發條件 |
|------|----------|---------|
| 1001 | PARAM_REQUIRED | stockId 未帶 |
| 2020 | WATCHLIST_ALREADY_EXISTS | 重複加入 |
| 2021 | WATCHLIST_LIMIT_REACHED | 已達 50 檔上限 |
| 3001 | UNAUTHORIZED | 未登入 |
| 4001 | STOCK_NOT_FOUND | 股票不存在 |

---

### §4.2 POST /api/v1/watchlist/remove

**Request**：`{ "stockId": string }`

**Response.data（成功）**：`null`

**業務錯誤碼**：

| code | Constant | 觸發條件 |
|------|----------|---------|
| 1001 | PARAM_REQUIRED | stockId 未帶 |
| 2023 | WATCHLIST_NOT_FOUND | 股票不在自選股清單（Wave 3 新增） |
| 3001 | UNAUTHORIZED | 未登入 |

---

### §4.3 POST /api/v1/watchlist/list

**Request**：空 JSON `{}`

**Response.data**：

| 欄位 | 型別 | 說明 |
|------|------|------|
| items | WatchlistListItem[] | 自選股清單 |
| total | integer | 總筆數 |

**WatchlistListItem 欄位**：

| 欄位 | 型別 | 說明 |
|------|------|------|
| itemId | string（UUID） | 記錄 ID |
| stockId | string | 股票代號 |
| stockName | string | 中文名稱 |
| market | `"TWSE"` \| `"OTC"` | 市場別 |
| createdAt | string（ISO 8601） | 加入時間 |
| quote | WatchlistQuoteSnapshot \| null | 最新報價；暫時無法取得時為 null |
| quoteError | string \| null | 若 quote 為 null，填入錯誤碼字串（例 `"5010"`） |

**WatchlistQuoteSnapshot 欄位**（來自 quote/list 批次查詢）：

| 欄位 | 型別 | 說明 |
|------|------|------|
| price | string | 最新成交價（BigDecimal string） |
| change | string | 漲跌額（帶正負號，BigDecimal string） |
| changePercent | string | 漲跌幅（帶正負號，不含 % 符號） |
| volume | number | 成交量（股數） |
| quoteDate | string（YYYY-MM-DD） | 報價日期 |
| isStale | boolean | 是否陳舊資料 |

> **業務規則**：若某股票報價查詢失敗，不中斷整批，該筆 quote = null，quoteError = 錯誤碼字串。

**效能要求**：P95 < 500ms（10 檔自選股）

---

## §5. Alert API Contract

### §5.1 POST /api/v1/alert/create

**Request**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號 |
| alertType | `"PRICE_ABOVE"` \| `"PRICE_BELOW"` \| `"CHANGE_PERCENT"` | Y | 警示類型 |
| threshold | string（BigDecimal） | Y | 觸發門檻值，必須 > 0 |
| changeDirection | `"UP"` \| `"DOWN"` \| `"BOTH"` \| null | 僅 CHANGE_PERCENT 必填 | 漲跌方向 |

**Response.data（成功）**：AlertItem（見 §5.5）

**業務錯誤碼**：

| code | Constant | 觸發條件 |
|------|----------|---------|
| 1001 | PARAM_REQUIRED | 必填欄位缺失 |
| 1002 | PARAM_FORMAT_INVALID | alertType / changeDirection 不合法值 |
| 1003 | PARAM_OUT_OF_RANGE | threshold <= 0 |
| 2030 | ALERT_DUPLICATE | 相同條件已存在 |
| 2032 | ALERT_LIMIT_REACHED | 已達每股 5 條上限（Wave 3 新增） |
| 3001 | UNAUTHORIZED | 未登入 |
| 4001 | STOCK_NOT_FOUND | 股票不存在 |

---

### §5.2 POST /api/v1/alert/update-status

**Request**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| alertId | string（UUID） | Y | 警示 ID |
| status | `"ACTIVE"` \| `"PAUSED"` | Y | 目標狀態（允許 TRIGGERED → ACTIVE） |

**Response.data（成功）**：`null`

**狀態轉換規則**：

| 當前狀態 | 允許轉換至 |
|---------|---------|
| ACTIVE | PAUSED |
| PAUSED | ACTIVE |
| TRIGGERED | ACTIVE（重新啟用） |
| DELETED | 不允許（回 4003） |

---

### §5.3 POST /api/v1/alert/delete

**Request**：`{ "alertId": string }`

**Response.data（成功）**：`null`

**實作**：軟刪除，將 status 設為 `DELETED`。

---

### §5.4 POST /api/v1/alert/list

**Request**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string \| null | N | 不帶則查所有股票 |
| status | `"ACTIVE"` \| `"PAUSED"` \| `"TRIGGERED"` \| null | N | 不帶則回傳 ACTIVE + PAUSED + TRIGGERED |

**Response.data**：`{ "items": AlertItem[], "total": integer }`

---

### §5.5 AlertItem 欄位定義（所有 Alert API 共用）

| 欄位 | 型別 | 說明 |
|------|------|------|
| alertId | string（UUID） | 警示 ID |
| stockId | string | 股票代號 |
| stockName | string | 股票中文名稱 |
| alertType | `"PRICE_ABOVE"` \| `"PRICE_BELOW"` \| `"CHANGE_PERCENT"` | 警示類型 |
| threshold | string（BigDecimal） | 觸發門檻值 |
| changeDirection | `"UP"` \| `"DOWN"` \| `"BOTH"` \| null | 漲跌方向（僅 CHANGE_PERCENT 有值） |
| status | `"ACTIVE"` \| `"PAUSED"` \| `"TRIGGERED"` \| `"DELETED"` | 狀態 |
| triggeredAt | string（ISO 8601） \| null | 最近一次觸發時間 |
| triggerCount | integer | 累計觸發次數 |
| createdAt | string（ISO 8601） | 建立時間 |

---

## §6. 推播相關 API Contract

### §6.1 POST /api/v1/user/notification-preference

**Request**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| action | `"get"` \| `"update"` | Y | 操作類型 |
| webPushEnabled | boolean \| null | N（update 時選填） | 是否啟用 Web Push |
| emailEnabled | boolean \| null | N（update 時選填） | 是否啟用 Email |
| webPushStatus | `"UNKNOWN"` \| `"GRANTED"` \| `"DENIED"` \| `"UNAVAILABLE"` \| null | N | 前端同步瀏覽器授權狀態 |

**Response.data**：

| 欄位 | 型別 | 說明 |
|------|------|------|
| webPushEnabled | boolean | 是否啟用 Web Push |
| emailEnabled | boolean | 是否啟用 Email |
| webPushStatus | `"UNKNOWN"` \| `"GRANTED"` \| `"DENIED"` \| `"UNAVAILABLE"` | 推播授權狀態 |
| updatedAt | string（ISO 8601） \| null | 最後更新時間 |

---

### §6.2 POST /api/v1/push/subscribe

**Request**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| endpoint | string | Y | Web Push endpoint URL |
| p256dh | string | Y | ECDH 公鑰（base64url） |
| auth | string | Y | 驗證密鑰（base64url） |
| userAgent | string \| null | N | 瀏覽器 User-Agent |

**Response.data**：`{ "subscriptionId": string（UUID） }`

**業務規則**：同一 endpoint 重複訂閱視為 upsert（is_active = true，fail_count = 0）。

---

### §6.3 POST /api/v1/push/unsubscribe

**Request**：`{ "endpoint": string }`

**Response.data**：`null`

---

## §7. Auth API Contract

### §7.1 POST /api/v1/auth/refresh

**Request**：`{ "refreshToken": string }`

**Response.data（成功）**：

| 欄位 | 型別 | 說明 |
|------|------|------|
| accessToken | string | 新的 Access Token（RS256 JWT） |
| expiresIn | integer | 有效秒數（固定 900） |
| refreshToken | string | 新的 Refresh Token（舊的已撤銷） |
| refreshExpiresIn | integer | 有效秒數（固定 604800） |

**業務規則**：Refresh Token Rotation — 每次 refresh 回傳全新的 refresh token，舊 token 立即撤銷。

**業務錯誤碼**：

| code | Constant | 觸發條件 |
|------|----------|---------|
| 1001 | PARAM_REQUIRED | refreshToken 未帶 |
| 3002 | TOKEN_EXPIRED | refresh token 已過期 |
| 3003 | TOKEN_INVALID | refresh token 無效 / 已撤銷 |

---

### §7.2 POST /api/v1/auth/logout

**Request**：`{ "refreshToken": string }`

**Response.data（成功）**：`null`

**後端行為**：
1. 將 refresh token 的 token_hash 設為 revoked（REFRESH_TOKEN 表 is_active = false）
2. 從 request Authorization header 取 access token 的 jti，加入 Redis 黑名單（TTL = 剩餘有效期）

---

## §8. 搜尋歷史 API Contract

### §8.1 POST /api/v1/search/history/list

**Request**：空 JSON `{}`

**Response.data**：

| 欄位 | 型別 | 說明 |
|------|------|------|
| items | SearchHistoryItem[] | 最多 10 筆，依 searched_at 降序 |
| total | integer | 總筆數 |

**SearchHistoryItem 欄位**：

| 欄位 | 型別 | 說明 |
|------|------|------|
| stockId | string | 股票代號 |
| stockName | string | 股票中文名稱 |
| market | `"TWSE"` \| `"OTC"` | 市場別 |
| searchedAt | string（ISO 8601） | 最後搜尋時間 |

---

### §8.2 POST /api/v1/search/history/remove

**Request**：`{ "stockId": string }`

**Response.data**：`null`

---

### §8.3 POST /api/v1/search/history/clear

**Request**：空 JSON `{}`

**Response.data**：`null`

---

## §9. 前端 TypeScript 型別定義（單一事實源）

Felix 採用以下型別定義作為唯一標準，不得與本文件不一致：

```typescript
// ---- 搜尋 ----

export type MatchType =
  | 'ID_EXACT'
  | 'ID_PREFIX'
  | 'NAME_EXACT'
  | 'NAME_PREFIX'
  | 'NAME_PARTIAL'
  | 'NAME_EN_PREFIX';

export interface StockSearchItem {
  stockId: string;
  stockName: string;
  stockNameEn: string | null;
  market: 'TWSE' | 'OTC';
  matchType: MatchType;
}

export interface StockSearchResult {
  items: StockSearchItem[];
  keyword: string;
  total: number;
}

export interface HotSearchItem {
  rank: number;
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'OTC';
  searchCount: number;
}

export interface HotSearchResult {
  items: HotSearchItem[];
  computedAt: string;
}

// ---- 搜尋歷史 ----

export interface SearchHistoryItem {
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'OTC';
  searchedAt: string;
}

// ---- Watchlist ----

export interface WatchlistQuoteSnapshot {
  price: string;           // BigDecimal string
  change: string;          // 帶正負號，BigDecimal string
  changePercent: string;   // 帶正負號，不含 % 符號
  volume: number;
  quoteDate: string;       // YYYY-MM-DD
  isStale: boolean;
}

export interface WatchlistItem {
  itemId: string;          // UUID
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'OTC';
  createdAt: string;       // ISO 8601
  quote: WatchlistQuoteSnapshot | null;
  quoteError: string | null;
}

// ---- Price Alert ----

export type AlertType = 'PRICE_ABOVE' | 'PRICE_BELOW' | 'CHANGE_PERCENT';
export type AlertStatus = 'ACTIVE' | 'PAUSED' | 'TRIGGERED' | 'DELETED';
export type ChangeDirection = 'UP' | 'DOWN' | 'BOTH';

export interface AlertItem {
  alertId: string;
  stockId: string;
  stockName: string;
  alertType: AlertType;
  threshold: string;       // BigDecimal string
  changeDirection: ChangeDirection | null;
  status: AlertStatus;
  triggeredAt: string | null;
  triggerCount: number;
  createdAt: string;
}

// ---- Notification Preference ----

export type WebPushStatus = 'UNKNOWN' | 'GRANTED' | 'DENIED' | 'UNAVAILABLE';

export interface NotificationPreference {
  webPushEnabled: boolean;
  emailEnabled: boolean;
  webPushStatus: WebPushStatus;
  updatedAt: string | null;
}

// ---- Auth ----

export interface TokenPair {
  accessToken: string;
  expiresIn: number;           // 900 秒
  refreshToken: string;
  refreshExpiresIn: number;    // 604800 秒
}

// ---- 未登入自選股意圖暫存（sessionStorage）----

export interface PendingWatchlistIntent {
  stockId: string;
  stockName: string;
  expiredAt: number;           // Unix timestamp ms（now + 5 分鐘）
}
```

---

## §10. Wave 3 新增資料表 Schema

以下為 Wave 3 新增的資料表，Sophia / Preston 須依此建立 migration script（Flyway V3.x.x）：

### §10.1 WATCHLIST_ITEM

```sql
CREATE TABLE WATCHLIST_ITEM (
    item_id     VARCHAR(36)   NOT NULL,
    user_id     VARCHAR(36)   NOT NULL,
    stock_id    VARCHAR(20)   NOT NULL,
    sort_order  INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_watchlist_item PRIMARY KEY (item_id),
    CONSTRAINT uk_watchlist_user_stock UNIQUE (user_id, stock_id),
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES user_info(user_id),
    CONSTRAINT fk_watchlist_stock FOREIGN KEY (stock_id) REFERENCES stock_info(stock_id)
);
CREATE INDEX idx_watchlist_user ON WATCHLIST_ITEM (user_id);
```

### §10.2 PRICE_ALERT

```sql
CREATE TABLE PRICE_ALERT (
    alert_id         VARCHAR(36)    NOT NULL,
    user_id          VARCHAR(36)    NOT NULL,
    stock_id         VARCHAR(20)    NOT NULL,
    alert_type       VARCHAR(20)    NOT NULL,
    threshold        NUMERIC(18,4)  NOT NULL,
    change_direction VARCHAR(10),
    status           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    triggered_at     TIMESTAMP,
    trigger_count    INTEGER        NOT NULL DEFAULT 0,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    CONSTRAINT pk_price_alert PRIMARY KEY (alert_id),
    CONSTRAINT ck_price_alert_threshold CHECK (threshold > 0),
    CONSTRAINT ck_price_alert_type CHECK (alert_type IN ('PRICE_ABOVE','PRICE_BELOW','CHANGE_PERCENT')),
    CONSTRAINT ck_price_alert_status CHECK (status IN ('ACTIVE','PAUSED','TRIGGERED','DELETED')),
    CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES user_info(user_id),
    CONSTRAINT fk_alert_stock FOREIGN KEY (stock_id) REFERENCES stock_info(stock_id)
);
CREATE INDEX idx_alert_user ON PRICE_ALERT (user_id);
CREATE INDEX idx_alert_user_stock ON PRICE_ALERT (user_id, stock_id);
CREATE INDEX idx_alert_active ON PRICE_ALERT (status, stock_id) WHERE status = 'ACTIVE';
```

### §10.3 NOTIFICATION_PREFERENCE

```sql
CREATE TABLE NOTIFICATION_PREFERENCE (
    pref_id          VARCHAR(36)  NOT NULL,
    user_id          VARCHAR(36)  NOT NULL,
    web_push_enabled BOOLEAN      NOT NULL DEFAULT true,
    email_enabled    BOOLEAN      NOT NULL DEFAULT true,
    web_push_status  VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    CONSTRAINT pk_notification_preference PRIMARY KEY (pref_id),
    CONSTRAINT uk_notification_pref_user UNIQUE (user_id),
    CONSTRAINT ck_web_push_status CHECK (web_push_status IN ('UNKNOWN','GRANTED','DENIED','UNAVAILABLE')),
    CONSTRAINT fk_notif_pref_user FOREIGN KEY (user_id) REFERENCES user_info(user_id)
);
```

### §10.4 PUSH_SUBSCRIPTION

```sql
CREATE TABLE PUSH_SUBSCRIPTION (
    subscription_id VARCHAR(36)   NOT NULL,
    user_id         VARCHAR(36)   NOT NULL,
    endpoint        TEXT          NOT NULL,
    p256dh          TEXT          NOT NULL,
    auth            VARCHAR(255)  NOT NULL,
    user_agent      VARCHAR(512),
    is_active       BOOLEAN       NOT NULL DEFAULT true,
    fail_count      INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT pk_push_subscription PRIMARY KEY (subscription_id),
    CONSTRAINT uk_push_sub_endpoint UNIQUE (endpoint),
    CONSTRAINT fk_push_sub_user FOREIGN KEY (user_id) REFERENCES user_info(user_id)
);
CREATE INDEX idx_push_sub_user ON PUSH_SUBSCRIPTION (user_id);
```

### §10.5 NOTIFICATION_LOG

```sql
CREATE TABLE NOTIFICATION_LOG (
    log_id          VARCHAR(36)    NOT NULL,
    alert_id        VARCHAR(36)    NOT NULL,
    user_id         VARCHAR(36)    NOT NULL,
    stock_id        VARCHAR(20)    NOT NULL,
    channel         VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    triggered_price NUMERIC(18,4)  NOT NULL,
    sent_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_detail    TEXT,
    CONSTRAINT pk_notification_log PRIMARY KEY (log_id),
    CONSTRAINT ck_notif_channel CHECK (channel IN ('WEB_PUSH','EMAIL')),
    CONSTRAINT ck_notif_status CHECK (status IN ('SENT','FAILED','DELIVERED','SKIPPED')),
    CONSTRAINT fk_notif_log_alert FOREIGN KEY (alert_id) REFERENCES price_alert(alert_id),
    CONSTRAINT fk_notif_log_user FOREIGN KEY (user_id) REFERENCES user_info(user_id)
);
CREATE INDEX idx_notif_log_alert ON NOTIFICATION_LOG (alert_id);
CREATE INDEX idx_notif_log_user ON NOTIFICATION_LOG (user_id, sent_at DESC);
```

### §10.6 HOT_SEARCH

```sql
CREATE TABLE HOT_SEARCH (
    hot_id       VARCHAR(36)  NOT NULL,
    stock_id     VARCHAR(20)  NOT NULL,
    search_count INTEGER      NOT NULL DEFAULT 0,
    rank         INTEGER      NOT NULL,
    computed_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_hot_search PRIMARY KEY (hot_id),
    CONSTRAINT uk_hot_search_stock UNIQUE (stock_id),
    CONSTRAINT fk_hot_search_stock FOREIGN KEY (stock_id) REFERENCES stock_info(stock_id)
);
CREATE INDEX idx_hot_search_rank ON HOT_SEARCH (rank);
```

### §10.7 SEARCH_RAW_LOG

```sql
CREATE TABLE SEARCH_RAW_LOG (
    log_id      VARCHAR(36)  NOT NULL,
    stock_id    VARCHAR(20)  NOT NULL,
    searched_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_search_raw_log PRIMARY KEY (log_id)
    -- 注意：不設 FK 到 user_info（不記錄個人資訊）
);
-- 保留期：48 小時（排程清除）
-- 不建 index（僅由 cron job 全表掃描彙總）
```

### §10.8 SEARCH_HISTORY

```sql
CREATE TABLE SEARCH_HISTORY (
    history_id  VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    stock_id    VARCHAR(20)  NOT NULL,
    searched_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_search_history PRIMARY KEY (history_id),
    CONSTRAINT uk_search_history_user_stock UNIQUE (user_id, stock_id),
    CONSTRAINT fk_search_history_user FOREIGN KEY (user_id) REFERENCES user_info(user_id),
    CONSTRAINT fk_search_history_stock FOREIGN KEY (stock_id) REFERENCES stock_info(stock_id)
);
CREATE INDEX idx_search_history_user_time ON SEARCH_HISTORY (user_id, searched_at DESC);
```

### §10.9 REFRESH_TOKEN

```sql
CREATE TABLE REFRESH_TOKEN (
    token_id    VARCHAR(36)   NOT NULL,
    user_id     VARCHAR(36)   NOT NULL,
    token_hash  VARCHAR(64)   NOT NULL,
    device_info VARCHAR(512),
    issued_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMP     NOT NULL,
    revoked_at  TIMESTAMP,
    is_active   BOOLEAN       NOT NULL DEFAULT true,
    CONSTRAINT pk_refresh_token PRIMARY KEY (token_id),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES user_info(user_id)
);
CREATE INDEX idx_refresh_token_hash ON REFRESH_TOKEN (token_hash);
CREATE INDEX idx_refresh_token_user ON REFRESH_TOKEN (user_id, is_active);
```

---

## §11. Wave 3 新增錯誤碼（需同步至 errorCodes_central.md）

| Code | Constant | Message (zh-TW) | Message (en) | 段別 |
|------|----------|-----------------|--------------|------|
| `2023` | `WATCHLIST_NOT_FOUND` | 該股票不在自選股清單 | Stock not in watchlist | 業務 |
| `2032` | `ALERT_LIMIT_REACHED` | 已達每股警示上限 5 條 | Alert limit (5) per stock reached | 業務 |

**Java ErrorCode.java 補充**（Bruno）：

```java
ErrorCode.WATCHLIST_NOT_FOUND  = 2023   // Wave 3 新增
ErrorCode.ALERT_LIMIT_REACHED  = 2032   // Wave 3 新增
```

**TypeScript errorCodes.ts 補充**（Felix）：

```typescript
WATCHLIST_NOT_FOUND: 2023,     // Wave 3 新增
ALERT_LIMIT_REACHED: 2032,     // Wave 3 新增
```

---

## §12. 端點 / 欄位差異摘要（供 Brian / Fiona Review 用）

| 端點 | 關鍵設計點 | 注意事項 |
|------|---------|---------|
| `/stock/search` | matchType 欄位區分匹配類型 | 前端用於 highlight，不得省略 |
| `/stock/search` | 公開端點但可帶 token | 帶 token 時搜尋結果無差異；歷史寫入由前端點選後另行呼叫 |
| `/watchlist/list` | quote 可能為 null | `quoteError` 欄位說明原因；前端顯示「-」 |
| `/watchlist/list` | 批次查詢防 N+1 | 單次 SQL + 單次 quote/list；P95 < 500ms |
| `/alert/create` | threshold 為 BigDecimal string | `"580.00"` 不是數字 `580` |
| `/alert/create` | changeDirection 僅 CHANGE_PERCENT 有意義 | 其他類型帶入時後端 ignore（或回 1002） |
| `/auth/refresh` | Refresh Token Rotation | 舊 token 立即撤銷，禁止重複使用 |
| `/auth/refresh` | 公開端點 | 不需 Authorization header |
| `/user/notification-preference` | action 區分 get / update | 統一端點避免 GET body 的 RFC 爭議 |
| 所有需認證端點 | 未授權一律 200 + 3001 | 禁止使用 HTTP 401 |

---

## §13. 變更控管

本文件為 **Wave 3 API 唯一事實源**。

任何 schema 欄位變更（新增 / 修改 / 刪除）必須遵循以下流程：

1. PR 標題加 `[SRS-CHANGE]` 前綴，例：`[SRS-CHANGE][schema-lock-w3] 新增 watchlist.sortOrder 欄位`
2. 召喚 Peter 更新本文件（`20260423_wave3_schema-lock.md`）及 SRS 對應章節
3. Bruno（後端 DTO）+ Felix（前端 types / service / handlers）**必須在同一 PR 中同步更新**，不可分批提交
4. Reviewer Brian + Fiona **必須在同一 review cycle 確認**，不得各自分批 approve
5. 若涉及錯誤碼異動，同步更新 `20260422_errorCodes_central.md`
6. 若涉及資料表異動，同步更新 Flyway migration script 版本號
