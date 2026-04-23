# Wave 2 API 對外手冊

| 項目 | 內容 |
|------|------|
| **版本** | v1.0 |
| **日期** | 2026-04-23 |
| **適用 API** | M-QUOTE、M-FUND、M-CHIP（Wave 2） |
| **基準文件** | [schema-lock_stock-detail-apis.md](../03_spec/20260422_schema-lock_stock-detail-apis.md)、[errorCodes_central.md](../03_spec/20260422_errorCodes_central.md)、[wave2_spec-clarifications.md](../03_spec/20260423_wave2_spec-clarifications.md) |

---

## 通用規範

### HTTP 標準

| 項目 | 規格 |
|------|------|
| **HTTP 方法** | POST（所有端點） |
| **HTTP 狀態碼** | 200（永遠回傳 200，業務錯誤透過 `code` 欄位區分） |
| **Content-Type** | `application/json; charset=UTF-8` |
| **時間格式** | ISO 8601：`YYYY-MM-DDTHH:mm:ss.sss+08:00`；日期：`YYYY-MM-DD` |
| **時區** | GMT+8（台灣時區，固定） |
| **字元編碼** | UTF-8 |

### 認證

所有端點均需在 HTTP Header 帶入：

```
Authorization: Bearer {access_token}
```

`access_token` 為 JWT 格式，有效期 15 分鐘；過期需透過 refresh token 取得新 token（Wave 2 暫不涉及）。

### 統一回應格式（Envelope Pattern）

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-04-23T10:30:45.123+08:00",
  "traceId": "unique-request-id"
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `code` | number | 業務錯誤碼（0 = 成功，其他見錯誤碼表） |
| `message` | string | 錯誤描述（英文，client 需自行 i18n） |
| `data` | object / array | 業務資料（成功時才填） |
| `timestamp` | string | 伺服器回應時間（ISO 8601 + GMT+8） |
| `traceId` | string | 追蹤 ID（用於 debug） |

### BigDecimal 序列化

金額、比率等數值型欄位**序列化為字串**（`string`），避免 JavaScript 浮點誤差：

```json
{
  "price": "1050.00",
  "eps": "43.50",
  "per": "24.14",
  "change": "+19.00"
}
```

整數型欄位（成交量、股數）仍序列化為 `number`。

---

## 錯誤碼對照表

### Wave 2 核心錯誤碼

| Code | Constant | Message (zh-TW) | Message (en) | 原因 | 前端行為 |
|------|----------|-----------------|--------------|------|---------|
| 0 | SUCCESS | success | success | 成功 | 取 `data` 欄位 |
| 1001 | PARAM_REQUIRED | 必填參數缺失 | Required parameter is missing | 請求缺少必填欄位 | 欄位驗證提示 |
| 1002 | PARAM_FORMAT_INVALID | 參數格式錯誤 | Invalid parameter format | 日期格式不符、email 無效等 | 欄位格式錯誤提示 |
| 1003 | PARAM_OUT_OF_RANGE | 參數值超出允許範圍 | Parameter value out of range | 查詢日期超過 5 年、陣列 > 50 筆等 | 欄位範圍錯誤提示 |
| 3001 | UNAUTHORIZED | 未登入 | Not authenticated | 無 token 或 token 完全缺失 | 導回登入頁 |
| 3002 | TOKEN_EXPIRED | 登入憑證已過期 | Access token has expired | JWT 超過有效期 | 觸發 refresh token 流程 |
| 3003 | TOKEN_INVALID | 登入憑證無效或已被撤銷 | Token is invalid or revoked | JWT 簽章不符、被加入黑名單 | 直接導回登入頁 |
| 4001 | STOCK_NOT_FOUND | 查無此股票 | Stock not found | DB + 外部資料均無此股票代號 | 顯示「查無股票 XXX」空狀態 |
| 5010 | TWSE_DATA_SOURCE_ERROR | TWSE 資料來源暫時無法存取 | TWSE data source is temporarily unavailable | TWSE API 回傳錯誤或逾時 | 顯示「行情資料暫時無法載入」；可顯示 fallback 資料（`isStale=true`） |
| 5011 | MOPS_DATA_SOURCE_ERROR | MOPS 資料來源暫時無法存取 | MOPS data source is temporarily unavailable | MOPS API 回傳錯誤或逾時 | 顯示「基本面資料暫時無法載入」 |
| 5012 | CHIP_DATA_SOURCE_ERROR | 籌碼資料來源暫時無法存取 | Chip data source is temporarily unavailable | TWSE/OTC 籌碼 API 不可用 | 顯示「籌碼資料暫時無法載入」 |
| 5013 | OTC_DATA_SOURCE_ERROR | OTC 資料來源暫時無法存取 | OTC data source is temporarily unavailable | OTC 外部 API 回傳錯誤或逾時 | 同 5010 邏輯，顯示 fallback 資料 |
| 5014 | MOPS_FORMAT_CHANGED | MOPS 資料格式異動，請聯絡系統管理員 | MOPS data format has changed; please contact system administrator | MOPS 回應解析失敗，欄位格式異動 | 顯示「資料解析失敗」；**不應自動重試**（需工程師修復） |
| 9001 | SYSTEM_BUSY | 系統繁忙 | System is busy | 伺服器過載或未預期錯誤 | 顯示「系統繁忙」toast，可重試 |
| 9999 | UNKNOWN_ERROR | 未知錯誤 | Unknown error occurred | 所有未分類異常 | 顯示「發生未知錯誤」toast + traceId |

### 關鍵說明

**5010 / 5013 vs. 5014**：
- 5010 / 5013：暫時性問題，使用者可稍後重試
- 5014：結構性錯誤（parser 失敗），重試無用，需工程師介入修復

---

## API 清單

### 1. POST /api/v1/quote/get

查詢單一股票最新行情。

#### Request

```json
{
  "stockId": "2330"
}
```

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `stockId` | string | Y | 股票代號（如 `"2330"`） |

#### Response（成功）

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stockId": "2330",
    "stockName": "台積電",
    "market": "TWSE",
    "price": "1050.00",
    "previousClose": "1031.00",
    "change": "+19.00",
    "changePercent": "+1.84",
    "open": "1035.00",
    "high": "1055.00",
    "low": "1030.00",
    "volume": 28450000,
    "quoteDate": "2026-04-22",
    "updatedAt": "2026-04-22T13:30:00.000+08:00",
    "isStale": false,
    "source": "TWSE"
  },
  "timestamp": "2026-04-22T14:00:00.000+08:00",
  "traceId": "abc-123-def"
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `stockId` | string | 股票代號 |
| `stockName` | string | 股票名稱 |
| `market` | string | 交易所別（`TWSE` / `OTC`） |
| `price` | string | 當日收盤 / 最新成交價（BigDecimal，以字串表示） |
| `previousClose` | string | 前一交易日收盤價（BigDecimal） |
| `change` | string | 漲跌額（正值帶 `+`） |
| `changePercent` | string | 漲跌幅百分比（正值帶 `+`，不含 `%` 符號） |
| `open` | string | 開盤價（BigDecimal） |
| `high` | string | 當日最高價（BigDecimal） |
| `low` | string | 當日最低價（BigDecimal） |
| `volume` | number | 成交量（股數，JavaScript safe integer） |
| `quoteDate` | string | 報價日期（`YYYY-MM-DD`） |
| `updatedAt` | string | 資料最後更新時間（ISO 8601 + GMT+8） |
| `isStale` | boolean | 是否為陳舊資料（週末 / 假日 / 資料來源失敗時 fallback） |
| `source` | string | 資料來源（`TWSE` 或 `OTC`，與 `market` 一致） |

#### 重要設計說明

**`isStale=true` 時的語意**：
- `isStale=true` 表示該筆資料**非今日最新**（可能因週末 / 假日 / 外部資料不可用而 fallback）
- `quoteDate` 為 fallback DB 那筆資料的**實際交易日**（例：周一呼叫 API，TWSE 不可用，fallback 到周五資料，則 `quoteDate: "2026-04-19"`）
- 前端應顯示：「資料延遲，最後更新：{quoteDate}」
- 此時可選擇顯示黃色警示，但不應阻擋頁面

#### cURL 範例

```bash
curl -X POST https://api.stockplatform.tw/api/v1/quote/get \
  -H "Authorization: Bearer {your-token}" \
  -H "Content-Type: application/json" \
  -d '{"stockId":"2330"}'
```

#### 錯誤案例

**股票不存在**（code 4001）：
```json
{
  "code": 4001,
  "message": "Stock not found",
  "data": null,
  "timestamp": "2026-04-23T10:30:45.123+08:00",
  "traceId": "xyz-456-def"
}
```

**TWSE 資料不可用**（code 5010）：
```json
{
  "code": 5010,
  "message": "TWSE data source is temporarily unavailable",
  "data": null,
  "timestamp": "2026-04-23T10:30:45.123+08:00",
  "traceId": "xyz-457-def"
}
```

---

### 2. POST /api/v1/quote/list

批次查詢多檔股票行情。

#### Request

```json
{
  "stockIds": ["2330", "2317", "2454"],
  "market": "ALL"
}
```

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `stockIds` | string[] | Y | 股票代號陣列，最多 50 筆 |
| `market` | string | N | 市場篩選（`"TWSE"` / `"OTC"` / `"ALL"`），預設 `"ALL"` |

#### Response（成功）

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "stockId": "2330",
      "stockName": "台積電",
      "market": "TWSE",
      "price": "1050.00",
      ...
    },
    {
      "stockId": "2317",
      "stockName": "鴻海",
      "market": "TWSE",
      "price": "180.00",
      ...
    }
  ],
  "timestamp": "2026-04-22T14:00:00.000+08:00",
  "traceId": "abc-123-def"
}
```

回傳陣列，各元素結構與 `/quote/get` response 的 `data` 完全相同。

#### 業務規則

- 若某股票查無資料，**直接省略該筆**（不填 null）
- 若所有股票均查無，回傳空陣列 `[]`，`code` 仍為 `0`
- 若超過 50 筆，回傳 code 1003（PARAM_OUT_OF_RANGE）

---

### 3. POST /api/v1/quote/history

查詢 K 線歷史資料。

#### Request

```json
{
  "stockId": "2330",
  "period": "daily",
  "startDate": "2026-01-01",
  "endDate": "2026-04-22"
}
```

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `stockId` | string | Y | 股票代號 |
| `period` | string | Y | 資料週期（`"daily"` / `"weekly"` / `"monthly"`） |
| `startDate` | string | N | 起始日期（`YYYY-MM-DD`），預設今日前 90 天 |
| `endDate` | string | N | 結束日期（`YYYY-MM-DD`），預設今日 |

#### Response（成功）

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stockId": "2330",
    "period": "daily",
    "items": [
      {
        "date": "2026-04-01",
        "open": "1020.00",
        "high": "1035.00",
        "low": "1015.00",
        "close": "1031.00",
        "volume": 25000000
      },
      {
        "date": "2026-04-02",
        "open": "1033.00",
        "high": "1058.00",
        "low": "1029.00",
        "close": "1050.00",
        "volume": 28450000
      }
    ]
  },
  "timestamp": "2026-04-22T14:00:00.000+08:00",
  "traceId": "abc-123-def"
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `items[].date` | string | 交易日期（`YYYY-MM-DD`） |
| `items[].open` | string | 開盤價（BigDecimal） |
| `items[].high` | string | 最高價（BigDecimal） |
| `items[].low` | string | 最低價（BigDecimal） |
| `items[].close` | string | 收盤價（BigDecimal） |
| `items[].volume` | number | 成交量（股數） |

#### 聚合規則（後端自動執行）

- **weekly**：以自然週（週一～週五）為單位
  - open = 週首個交易日 open
  - high = 週內最高 high
  - low = 週內最低 low
  - close = 週末個交易日 close
  - volume = 週加總

- **monthly**：以自然月為單位（同上邏輯）

#### 查詢限制

| 週期 | 最長範圍 | 超出回傳 |
|------|----------|----------|
| daily | 5 年 | code 1003 |
| weekly | 5 年 | code 1003 |
| monthly | 10 年 | code 1003 |

---

### 4. POST /api/v1/fundamental/get

查詢個股基本面指標。

#### Request

```json
{
  "stockId": "2330"
}
```

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `stockId` | string | Y | 股票代號 |

#### Response（成功）

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stockId": "2330",
    "stockName": "台積電",
    "eps": "43.50",
    "per": "24.14",
    "pbr": "8.72",
    "roe": "23.5",
    "reportYear": 2025,
    "reportQuarter": 4,
    "updatedAt": "2026-04-22T08:00:00.000+08:00",
    "source": "MOPS"
  },
  "timestamp": "2026-04-22T14:00:00.000+08:00",
  "traceId": "abc-123-def"
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `stockId` | string | 股票代號 |
| `stockName` | string | 股票名稱 |
| `eps` | string | 近四季 EPS 滾動加總（BigDecimal） |
| `per` | string | 本益比（BigDecimal；**注意：欄位名為 `per` 不是 `perRatio`**） |
| `pbr` | string | 股價淨值比（BigDecimal；**注意：欄位名為 `pbr` 不是 `pbrRatio`**） |
| `roe` | string | 股東權益報酬率（百分比，例 `"23.5"` 表示 23.5%，不含 `%` 符號） |
| `reportYear` | number | 最新季報年份（例 `2025`） |
| `reportQuarter` | number | 最新季報季別（`1` ~ `4`） |
| `updatedAt` | string | 資料最後更新時間（ISO 8601 + GMT+8） |
| `source` | string | 資料來源（固定 `"MOPS"`） |

#### 重要設計說明

- `eps` 為近四季滾動加總（非單季），反映年度獲利能力
- `per` / `pbr` 為 **BigDecimal 序列化為字串**，避免浮點誤差
- `reportYear` / `reportQuarter` 為最新一筆季報期別

---

### 5. POST /api/v1/chip/get

查詢個股最新三大法人買賣超。

#### Request

```json
{
  "stockId": "2330"
}
```

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `stockId` | string | Y | 股票代號 |

#### Response（成功）

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stockId": "2330",
    "date": "2026-04-22",
    "institutions": [
      {
        "name": "外資",
        "buy": 18500000,
        "sell": 12000000,
        "netBuySell": 6500000
      },
      {
        "name": "投信",
        "buy": 3200000,
        "sell": 1500000,
        "netBuySell": 1700000
      },
      {
        "name": "自營商",
        "buy": 2100000,
        "sell": 3800000,
        "netBuySell": -1700000
      }
    ],
    "totalNetBuySell": 6500000,
    "source": "TWSE"
  },
  "timestamp": "2026-04-22T14:00:00.000+08:00",
  "traceId": "abc-123-def"
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `stockId` | string | 股票代號 |
| `date` | string | 資料日期（`YYYY-MM-DD`，最新交易日） |
| `institutions[]` | array | 三大法人陣列（固定 3 筆，順序：外資、投信、自營商） |
| `institutions[].name` | string | 法人名稱（固定值：`"外資"` / `"投信"` / `"自營商"`） |
| `institutions[].buy` | number | 買進股數 |
| `institutions[].sell` | number | 賣出股數 |
| `institutions[].netBuySell` | number | 買賣超（= buy - sell；正為買超，負為賣超） |
| `totalNetBuySell` | number | 三大法人合計買賣超 |
| `source` | string | 資料來源（`"TWSE"` 或 `"OTC"`） |

#### 重要設計說明

- `institutions` 陣列順序**固定不變**：外資 → 投信 → 自營商
- 前端應**依此順序渲染**，不可動態排序
- `buy` / `sell` 由 TWSE API 限制，目前暫為 0（Wave 3 評估擴充）
- `totalNetBuySell` 由後端計算，前端**不應自行加總**

---

## 常見使用模式

### 模式 1：載入個股詳情頁

```javascript
// TypeScript / React 示意

const StockDetail = ({ stockId }: { stockId: string }) => {
  const [quote, setQuote] = useState<StockQuote | null>(null);
  const [fundamental, setFundamental] = useState<StockFundamental | null>(null);
  const [chip, setChip] = useState<StockChip | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // 並行呼叫 3 個 API
    Promise.allSettled([
      api.post('/quote/get', { stockId }),
      api.post('/fundamental/get', { stockId }),
      api.post('/chip/get', { stockId }),
    ])
      .then(([quoteRes, fundRes, chipRes]) => {
        if (quoteRes.status === 'fulfilled') setQuote(quoteRes.value.data);
        if (fundRes.status === 'fulfilled') setFundamental(fundRes.value.data);
        if (chipRes.status === 'fulfilled') setChip(chipRes.value.data);
      })
      .catch(err => setError(err.message));
  }, [stockId]);

  if (error) return <ErrorAlert message={error} />;
  return (
    <>
      {quote && <PriceHeader quote={quote} />}
      {fundamental && <FundamentalCard fundamental={fundamental} />}
      {chip && <ChipCard chip={chip} />}
    </>
  );
};
```

### 模式 2：K 線圖顯示

```javascript
const KLineChart = ({ stockId, period }: { stockId: string; period: 'daily' | 'weekly' | 'monthly' }) => {
  const [data, setData] = useState<KLineHistory | null>(null);

  useEffect(() => {
    api.post('/quote/history', {
      stockId,
      period,
      startDate: dayjs().subtract(90, 'days').format('YYYY-MM-DD'),
      endDate: dayjs().format('YYYY-MM-DD'),
    })
      .then(res => setData(res.data))
      .catch(err => console.error(err));
  }, [stockId, period]);

  if (!data) return <Skeleton />;
  return <FinancialChart data={data.items} />;
};
```

### 模式 3：自選股批次查詢

```javascript
const WatchlistGrid = ({ stockIds }: { stockIds: string[] }) => {
  const [quotes, setQuotes] = useState<StockQuote[]>([]);

  useEffect(() => {
    api.post('/quote/list', {
      stockIds: stockIds.slice(0, 50), // 最多 50 筆
      market: 'ALL',
    })
      .then(res => setQuotes(res.data))
      .catch(err => console.error(err));
  }, [stockIds]);

  return (
    <Table
      columns={[
        { title: '代號', dataIndex: 'stockId' },
        { title: '名稱', dataIndex: 'stockName' },
        { title: '價格', dataIndex: 'price' },
        { title: '漲跌', dataIndex: 'changePercent' },
      ]}
      dataSource={quotes}
    />
  );
};
```

---

## 效能優化建議

### 1. 並行請求 vs. 依序請求

**推薦：並行請求**（Promise.allSettled）
- 若 quote / fundamental / chip 無相依性，應並行呼叫
- 單個 API 若超時，不影響其他 API（使用 allSettled）

### 2. 快取策略

- **行情（quote）**：每 30 秒更新一次（配合市場更新頻率）
- **基本面（fundamental）**：每日更新一次（季報穩定）
- **籌碼（chip）**：每日更新一次

使用 React Query 或 SWR 可自動管理快取：

```typescript
import { useQuery } from '@tanstack/react-query';

const { data: quote } = useQuery({
  queryKey: ['quote', stockId],
  queryFn: () => api.post('/quote/get', { stockId }),
  staleTime: 30_000, // 30 秒後重新驗証
  cacheTime: 5 * 60_000, // 快取 5 分鐘
});
```

### 3. 錯誤重試策略

**5010-5013（暫時性錯誤）**：可自動重試，建議指數退避：
```typescript
const { data, isLoading, error } = useQuery({
  queryKey: ['quote', stockId],
  queryFn: () => api.post('/quote/get', { stockId }),
  retry: (failureCount, error) => {
    // 5014 不重試，其他錯誤重試 3 次
    return failureCount < 3 && error.code !== 5014;
  },
  retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30_000),
});
```

**5014（結構性錯誤）**：不重試，直接顯示「聯絡工程師」

---

## 範例回應

### 成功：完整個股資訊

Request:
```bash
curl -X POST https://api.stockplatform.tw/api/v1/quote/get \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{"stockId":"2330"}'
```

Response:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stockId": "2330",
    "stockName": "台積電",
    "market": "TWSE",
    "price": "1050.00",
    "previousClose": "1031.00",
    "change": "+19.00",
    "changePercent": "+1.84",
    "open": "1035.00",
    "high": "1055.00",
    "low": "1030.00",
    "volume": 28450000,
    "quoteDate": "2026-04-22",
    "updatedAt": "2026-04-22T13:30:00.000+08:00",
    "isStale": false,
    "source": "TWSE"
  },
  "timestamp": "2026-04-23T10:30:45.123+08:00",
  "traceId": "req-2330-001"
}
```

### 成功但 Stale：週末查詢週五資料

Request:（週一上午，TWSE 尚未開盤）
```bash
curl -X POST https://api.stockplatform.tw/api/v1/quote/get \
  -d '{"stockId":"2330"}'
```

Response:（fallback 至週五資料）
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stockId": "2330",
    "stockName": "台積電",
    "market": "TWSE",
    "price": "1050.00",
    "previousClose": "1031.00",
    "change": "+19.00",
    "changePercent": "+1.84",
    "open": "1035.00",
    "high": "1055.00",
    "low": "1030.00",
    "volume": 28450000,
    "quoteDate": "2026-04-19",         // ← 週五
    "updatedAt": "2026-04-22T13:30:00.000+08:00",
    "isStale": true,                    // ← 標記陳舊
    "source": "TWSE"
  },
  "timestamp": "2026-04-23T09:00:00.000+08:00",
  "traceId": "req-2330-002"
}
```

### 失敗：MOPS 格式異動

Request:
```bash
curl -X POST https://api.stockplatform.tw/api/v1/fundamental/get \
  -d '{"stockId":"2330"}'
```

Response（code 5014）：
```json
{
  "code": 5014,
  "message": "MOPS data format has changed; parser needs update",
  "data": null,
  "timestamp": "2026-04-23T10:30:45.123+08:00",
  "traceId": "req-fund-5014-003"
}
```

前端應顯示：「資料解析失敗，請聯絡系統管理員（traceId: req-fund-5014-003）」

---

## 版本與變更

| 版本 | 日期 | 重要變更 |
|------|------|---------|
| v1.0 | 2026-04-23 | Wave 2 Release；5 個 endpoint 正式發布 |

---

**文件最後更新**：2026-04-23  
**API 基準版本**：schema-lock v1.0（含 spec-clarifications）
