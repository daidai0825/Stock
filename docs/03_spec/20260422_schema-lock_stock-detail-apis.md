# Schema Lock：個股詳情頁 API Contract（Wave B / Wave 2）

| 項目 | 內容 |
|------|------|
| 文件版本 | v1.0 |
| 撰寫者 | Peter（產品經理） |
| 撰寫日期 | 2026-04-22 |
| 狀態 | **LOCKED（正式鎖定）** |
| 仲裁依據 | Jamie 決策 D1~D6（2026-04-22） |
| 適用範圍 | Wave B 個股詳情頁所涉 4 支端點 + errorCode |
| 下游使用者 | Bruno（後端 DTO 實作）、Felix（前端 types/service）、Brian（後端 Review）、Fiona（前端 Review）、Quincy/Quinn（contract test 基準） |

---

## 0. 全域規範

| 項目 | 規範 |
|------|------|
| HTTP 方法 | POST |
| HTTP 狀態碼 | 200（業務錯誤透過 `code` 區分） |
| Content-Type | `application/json; charset=UTF-8` |
| 認證 | `Authorization: Bearer {access_token}`（所有端點均需） |
| 數值精度 | 金額 / 比率類：後端 `BigDecimal` 序列化為 `string`（保留精度、避免 JS 浮點誤差） |
| 整數類 | 成交量、買賣超股數：序列化為 `number`（JavaScript safe integer 範圍內） |
| 時間格式 | ISO 8601 datetime：`YYYY-MM-DDTHH:mm:ss.sss+08:00`；純日期：`YYYY-MM-DD` |
| 時區 | 固定 GMT+8 |
| Envelope 格式 | `{ "code": 0, "message": "success", "data": {...}, "timestamp": "...", "traceId": "..." }` |

---

## 1. `POST /api/v1/quote/get`

### 1.1 用途

查詢單一股票最新行情（含漲跌計算）。

**仲裁記錄**：D2（前端結構為基準）+ D4（errorCode 以後端為準）+ D5（端點路徑確認）

### 1.2 Request Body

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號，例 `"2330"` |

```json
{
  "stockId": "2330"
}
```

### 1.3 Response.data Schema

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號，例 `"2330"` |
| stockName | string | Y | 股票名稱，例 `"台積電"` |
| market | `"TWSE"` \| `"OTC"` | Y | 交易所別（上市 / 上櫃） |
| price | string | Y | 當日收盤 / 最新成交價（BigDecimal → string，例 `"1050.00"`） |
| previousClose | string | Y | 前一交易日收盤價（BigDecimal → string） |
| change | string | Y | 漲跌額 = price − previousClose（後端計算，例 `"+19.00"` / `"-5.00"`，正值帶 `+`） |
| changePercent | string | Y | 漲跌幅（後端計算，例 `"+1.84"` 表示 +1.84%，正值帶 `+`，不含 `%` 符號） |
| open | string | Y | 開盤價（BigDecimal → string） |
| high | string | Y | 當日最高價（BigDecimal → string） |
| low | string | Y | 當日最低價（BigDecimal → string） |
| volume | number | Y | 成交量（股數，JavaScript safe integer） |
| quoteDate | string | Y | 報價日期（`YYYY-MM-DD`，最新交易日日期） |
| updatedAt | string | Y | 資料最後更新時間（ISO 8601 含時區，例 `"2026-04-22T13:30:00.000+08:00"`） |
| isStale | boolean | Y | 是否為陳舊資料（週末 / 假日 / 外部來源失敗時 fallback 回 DB 最新一筆並標記 `true`） |
| source | `"TWSE"` \| `"OTC"` | Y | 資料來源，與 market 保持一致 |

> **設計說明**：
> - `change` 與 `changePercent` 必須由**後端計算**後回傳，前端不得自行計算（業務邏輯應集中在後端）。
> - `isStale: true` 時，前端應顯示「資料延遲，最後更新：{quoteDate}」之類的提示文案。
> - `previousClose` 保留原始精度，不可四捨五入。

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

### 1.4 業務錯誤碼

| code | Constant | 觸發條件 |
|------|----------|----------|
| 4001 | STOCK_NOT_FOUND | DB 無此股票 + 外部來源亦查無 |
| 5010 | TWSE_DATA_SOURCE_ERROR | TWSE 外部 API 不可用 |
| 5013 | OTC_DATA_SOURCE_ERROR | OTC 外部 API 不可用 |

---

## 2. `POST /api/v1/quote/list`

### 2.1 用途

批次查詢多檔股票最新行情（用於自選股清單頁批次顯示）。

### 2.2 Request Body

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockIds | string[] | Y | 股票代號陣列，例 `["2330", "2317", "2454"]`；最多 50 筆（對應自選股上限） |
| market | `"TWSE"` \| `"OTC"` \| `"ALL"` | N | 市場別篩選，預設 `"ALL"` |

```json
{
  "stockIds": ["2330", "2317", "2454"],
  "market": "ALL"
}
```

### 2.3 Response.data Schema

`StockQuote[]`，陣列元素 schema 與第 1 節 `/api/v1/quote/get` 的 `Response.data` 完全一致。

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
    }
  ],
  "timestamp": "2026-04-22T14:00:00.000+08:00",
  "traceId": "abc-123-def"
}
```

### 2.4 業務規則

- 若某股票代號查無資料，**不中斷整批回傳**，該筆於陣列中直接省略（不填 null）。
- 若所有代號均查無，回傳空陣列 `[]`，`code` 仍為 `0`。
- `market` 篩選條件在後端執行（`QuoteListRequest.market` 欄位須實作，Brian review M-BE-W2-05 已指出）。

### 2.5 業務錯誤碼

| code | Constant | 觸發條件 |
|------|----------|----------|
| 1001 | PARAM_REQUIRED | `stockIds` 為空陣列或未帶 |
| 1003 | PARAM_OUT_OF_RANGE | `stockIds` 超過 50 筆 |
| 5010 | TWSE_DATA_SOURCE_ERROR | TWSE 外部 API 不可用 |
| 5013 | OTC_DATA_SOURCE_ERROR | OTC 外部 API 不可用 |

---

## 3. `POST /api/v1/quote/history`

### 3.1 用途

查詢個股歷史 K 線資料（日 / 週 / 月），用於 K 線圖渲染。

**仲裁記錄**：D3（`period: daily/weekly/monthly` 廢除 `month: LocalDate` 語意）+ D5（路徑去掉 `/get`）

### 3.2 Request Body

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號 |
| period | `"daily"` \| `"weekly"` \| `"monthly"` | Y | 資料週期 |
| startDate | string | N | 起始日期（`YYYY-MM-DD`），預設今日 90 天前 |
| endDate | string | N | 結束日期（`YYYY-MM-DD`），預設今日 |

```json
{
  "stockId": "2330",
  "period": "daily",
  "startDate": "2026-01-01",
  "endDate": "2026-04-22"
}
```

### 3.3 Response.data Schema

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號 |
| period | `"daily"` \| `"weekly"` \| `"monthly"` | Y | 請求的週期（echo back） |
| items | HistoryItem[] | Y | K 線資料陣列，依 `date` 升冪排序 |

**HistoryItem**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| date | string | Y | 日期（`YYYY-MM-DD`）；weekly 取該週第一個交易日；monthly 取該月第一個交易日 |
| open | string | Y | 開盤價（BigDecimal → string）；weekly/monthly 取週/月首日 open |
| high | string | Y | 最高價（BigDecimal → string）；weekly/monthly 取週/月最高 high |
| low | string | Y | 最低價（BigDecimal → string）；weekly/monthly 取週/月最低 low |
| close | string | Y | 收盤價（BigDecimal → string）；weekly/monthly 取週/月末日 close |
| volume | number | Y | 成交量（股數）；weekly/monthly 取週/月加總 |

> **週/月聚合規則（後端實作）**：
> - `weekly`：以自然週（週一至週五）為單位聚合，open = 週首個交易日 open、high = 週最高 high、low = 週最低 low、close = 週最後一個交易日 close、volume = 週成交量加總。
> - `monthly`：以自然月為單位聚合，open = 月第一個交易日 open、high = 月最高 high、low = 月最低 low、close = 月最後一個交易日 close、volume = 月成交量加總。
> - 聚合由後端完成，前端不得自行聚合。

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

### 3.4 業務規則

| 規則 | 值 |
|------|-----|
| `startDate` 預設 | 請求日往前 90 個自然日 |
| `endDate` 預設 | 請求當天日期 |
| `daily` 最長範圍 | 5 年（1825 天） |
| `weekly` 最長範圍 | 5 年 |
| `monthly` 最長範圍 | 10 年 |
| 超出範圍 | 回 1003 PARAM_OUT_OF_RANGE |
| `startDate > endDate` | 回 1002 PARAM_FORMAT_INVALID |
| 週末 / 假日 startDate | 自動對齊至下一個交易日（後端處理） |

### 3.5 業務錯誤碼

| code | Constant | 觸發條件 |
|------|----------|----------|
| 1001 | PARAM_REQUIRED | `stockId` 或 `period` 未帶 |
| 1002 | PARAM_FORMAT_INVALID | `startDate > endDate`；日期格式不正確 |
| 1003 | PARAM_OUT_OF_RANGE | 查詢範圍超出最長限制 |
| 4001 | STOCK_NOT_FOUND | 查無此股票代號 |
| 5010 | TWSE_DATA_SOURCE_ERROR | 歷史資料外部來源不可用 |

---

## 4. `POST /api/v1/fundamental/get`

### 4.1 用途

查詢個股基本面指標（EPS / PER / PBR / ROE），用於基本面卡片顯示。

**仲裁記錄**：D2（前端結構為基準，`per` / `pbr` 欄位名去掉 `Ratio` 字尾）+ `source` 欄位補齊 + `updatedAt` 改為 datetime 格式

### 4.2 Request Body

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號 |

```json
{
  "stockId": "2330"
}
```

### 4.3 Response.data Schema

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號 |
| stockName | string | Y | 股票名稱 |
| eps | string | Y | 近四季 EPS 滾動加總（BigDecimal → string，例 `"43.50"`） |
| per | string | Y | 本益比（BigDecimal → string，例 `"24.14"`）；欄位名稱固定為 `per`，禁止用 `perRatio` |
| pbr | string | Y | 股價淨值比（BigDecimal → string）；欄位名稱固定為 `pbr`，禁止用 `pbrRatio` |
| roe | string | Y | 股東權益報酬率（百分比，BigDecimal → string，例 `"23.5"` 表示 23.5%，不含 `%` 符號） |
| reportYear | number | Y | 最新季報年份（例 `2025`） |
| reportQuarter | number | Y | 最新季報季別（`1` ~ `4`） |
| updatedAt | string | Y | 資料最後更新時間（ISO 8601 含時區，例 `"2026-04-22T08:00:00.000+08:00"`） |
| source | `"MOPS"` | Y | 資料來源（目前固定為 `"MOPS"`） |

> **設計說明**：
> - `per` / `pbr` 欄位名不帶 `Ratio` 字尾，與前端 TypeScript 型別定義一致。
> - `eps` 為近四季滾動加總（非單季），後端需確認正確取最新四季資料並加總。
> - `reportYear` / `reportQuarter` 指最新一筆季報期別，需先排序再取（避免 B-BE-W2-03 bug）。

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

### 4.4 業務錯誤碼

| code | Constant | 觸發條件 |
|------|----------|----------|
| 4001 | STOCK_NOT_FOUND | 查無此股票代號 |
| 5011 | MOPS_DATA_SOURCE_ERROR | MOPS 外部 API 不可用 |
| 5014 | MOPS_FORMAT_CHANGED | MOPS 回應解析失敗（格式異動）；詳見 errorCode 中央註冊表 |

---

## 5. `POST /api/v1/chip/get`

### 5.1 用途

查詢個股最新三大法人買賣超資料，用於籌碼卡片顯示。

**仲裁記錄**：D2（前端 array 結構為基準，後端從 flat 欄位改為 array 並補 `buy` / `sell` 欄位）

### 5.2 Request Body

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號 |

```json
{
  "stockId": "2330"
}
```

### 5.3 Response.data Schema

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| stockId | string | Y | 股票代號 |
| date | string | Y | 資料日期（`YYYY-MM-DD`，最新交易日） |
| institutions | InstitutionItem[] | Y | 三大法人陣列，固定 3 筆，順序：外資、投信、自營商 |
| totalNetBuySell | number | Y | 三大法人合計買賣超（股數），`institutions[*].netBuySell` 加總 |
| source | `"TWSE"` \| `"OTC"` | Y | 資料來源 |

**InstitutionItem**：

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| name | `"外資"` \| `"投信"` \| `"自營商"` | Y | 法人名稱（繁體中文固定值） |
| buy | number | Y | 買進股數 |
| sell | number | Y | 賣出股數 |
| netBuySell | number | Y | 買賣超（= buy − sell），正值為買超，負值為賣超 |

> **設計說明**：
> - 後端 ChipDTO 原為 flat 欄位結構（`foreignNetShares` / `investmentTrustNetShares` / `dealerNetShares`），**必須改為 array 結構**並補充 `buy` / `sell` 欄位（Convertor 層轉換）。
> - `institutions` 陣列順序固定：`[外資, 投信, 自營商]`，前端依此順序渲染，不得依名稱動態排序。
> - `totalNetBuySell` 由後端計算（等於三筆 netBuySell 之和），前端不得自行加總。

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

### 5.4 業務錯誤碼

| code | Constant | 觸發條件 |
|------|----------|----------|
| 4001 | STOCK_NOT_FOUND | 查無此股票代號 |
| 5012 | CHIP_DATA_SOURCE_ERROR | 籌碼外部資料來源不可用 |

---

## 6. 前端 TypeScript 型別定義（單一事實源）

以下型別定義為 Felix 應採用的**唯一標準**，不得與本文件不一致：

```typescript
// ---- 行情 ----

export interface StockQuote {
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'OTC';
  price: string;
  previousClose: string;
  change: string;
  changePercent: string;
  open: string;
  high: string;
  low: string;
  volume: number;
  quoteDate: string;           // YYYY-MM-DD
  updatedAt: string;           // ISO 8601 + GMT+8
  isStale: boolean;
  source: 'TWSE' | 'OTC';
}

// ---- 歷史 K 線 ----

export type KLinePeriod = 'daily' | 'weekly' | 'monthly';

export interface KLineItem {
  date: string;                // YYYY-MM-DD
  open: string;
  high: string;
  low: string;
  close: string;
  volume: number;
}

export interface KLineHistory {
  stockId: string;
  period: KLinePeriod;
  items: KLineItem[];
}

// ---- 基本面 ----

export interface StockFundamental {
  stockId: string;
  stockName: string;
  eps: string;
  per: string;                 // 注意：不是 perRatio
  pbr: string;                 // 注意：不是 pbrRatio
  roe: string;
  reportYear: number;
  reportQuarter: number;       // 1 ~ 4
  updatedAt: string;           // ISO 8601 + GMT+8
  source: 'MOPS';
}

// ---- 籌碼 ----

export type InstitutionName = '外資' | '投信' | '自營商';

export interface InstitutionItem {
  name: InstitutionName;
  buy: number;
  sell: number;
  netBuySell: number;
}

export interface StockChip {
  stockId: string;
  date: string;                // YYYY-MM-DD
  institutions: InstitutionItem[];   // 固定順序：外資、投信、自營商
  totalNetBuySell: number;
  source: 'TWSE' | 'OTC';
}
```

---

## 7. 後端 DTO 對照（Bruno 必須對齊的欄位）

### 7.1 QuoteDTO 必須新增的欄位

以下欄位在 Wave 2 原始實作中**缺漏**，Bruno 必須在 DTO + Convertor 補齊：

| 欄位 | 型別（Java） | 序列化型別（JSON） | 說明 |
|------|------------|-------------------|------|
| `previousClose` | `BigDecimal` | `string` | 前日收盤，需從 DB 查前一交易日記錄 |
| `change` | `BigDecimal` | `string` | `price − previousClose`，帶正負號，Convertor 計算 |
| `changePercent` | `BigDecimal` | `string` | `change / previousClose * 100`，帶正負號 |
| `open` | `BigDecimal` | `string` | 原欄位名 `openPrice`，**重命名為 `open`** |
| `high` | `BigDecimal` | `string` | 原欄位名 `highPrice`，**重命名為 `high`** |
| `low` | `BigDecimal` | `string` | 原欄位名 `lowPrice`，**重命名為 `low`** |
| `price` | `BigDecimal` | `string` | 最新成交價（close = price for completed day），**新增欄位** |
| `quoteDate` | `LocalDate` | `string`（YYYY-MM-DD） | 已有，維持 |
| `updatedAt` | `LocalDateTime` | `string`（ISO 8601 + +08:00） | **新增欄位**，記錄資料最後 upsert 時間 |
| `isStale` | `boolean` | `boolean` | **新增欄位**，fallback 時設 `true` |
| `market` | `enum Market` | `string`（"TWSE"\|"OTC"） | 已有，維持 |
| `source` | `enum DataSource` | `string`（"TWSE"\|"OTC"） | **新增欄位**，與 `market` 相同值 |

### 7.2 FundamentalDTO 必須修改的欄位

| 原欄位名 | 新欄位名 | 說明 |
|---------|---------|------|
| `perRatio` | `per` | 禁止使用 `perRatio` |
| `pbrRatio` | `pbr` | 禁止使用 `pbrRatio` |
| （無） | `updatedAt` | 新增，類型 `LocalDateTime`，序列化為 ISO 8601 + +08:00 |
| （無） | `source` | 新增，固定值 `"MOPS"` |

### 7.3 ChipDTO 必須重構的結構

| 原結構 | 新結構 |
|--------|--------|
| Flat 欄位：`foreignNetShares`、`investmentTrustNetShares`、`dealerNetShares`、`totalInstitutionalNet` | Array 欄位：`institutions: List<InstitutionItem>`（含 `name`、`buy`、`sell`、`netBuySell`） |
| `tradeDate`（LocalDate） | `date`（LocalDate）**重命名** |
| （無） | `source`（新增，值 `"TWSE"` 或 `"OTC"`） |
| （無） | `totalNetBuySell`（新增，等於三筆 netBuySell 之和，即原 `totalInstitutionalNet`）|

---

## 8. 端點 / 欄位對照差異摘要

此表為 Fiona、Brian 在第二輪 Review 時的核對基準。

| 端點 | 差異項目 | Felix 原始 | Bruno 原始 | Schema Lock 採用 |
|------|---------|-----------|-----------|-----------------|
| `/quote/get` | `price` 欄位 | 有 | 無（只有 openPrice/highPrice 等） | **有**（必填） |
| `/quote/get` | `change` / `changePercent` | 有 | 無 | **有**（後端計算） |
| `/quote/get` | `updatedAt` | 有（ISO 8601） | `quoteDate`（LocalDate） | **有 `updatedAt`（datetime）+ `quoteDate`（date）雙欄位** |
| `/quote/get` | `isStale` | 無 | 無 | **新增**（fallback 標記） |
| `/quote/history` | 請求 `period` | `daily/weekly/monthly` | `month: LocalDate` | **採前端語意，廢除 `month`** |
| `/quote/history` | 回傳欄位名 | `open/high/low/close` | `openPrice/highPrice/lowPrice/closePrice` | **採 `open/high/low/close`（去掉 Price 字尾）** |
| `/fundamental/get` | `per` / `pbr` | `per`、`pbr` | `perRatio`、`pbrRatio` | **採 `per`、`pbr`** |
| `/fundamental/get` | `updatedAt` | 有（datetime） | 無 | **有（datetime）** |
| `/fundamental/get` | `source` | 有 | 無 | **有（`"MOPS"`）** |
| `/chip/get` | 結構 | Array（含 buy/sell） | Flat 4 欄位 | **採 Array 結構** |
| `/chip/get` | `date` | `date` | `tradeDate` | **採 `date`** |
| `/chip/get` | `source` | 有 | 無 | **有** |

---

## 9. 變更控管

> 本文件為 **Wave B 個股詳情頁 API 唯一事實源**。
> 任何 schema 欄位變更（新增 / 修改 / 刪除）必須遵循以下流程：
>
> 1. PR 標題加 `[SRS-CHANGE]` 前綴，例：`[SRS-CHANGE][schema-lock] 新增 quote.sector 欄位`
> 2. 召喚 Peter 更新本文件（`20260422_schema-lock_stock-detail-apis.md`）及 SRS 對應章節
> 3. Bruno（後端 DTO）+ Felix（前端 types/service/handlers）**必須在同一 PR 中同步更新**，不可分批提交
> 4. Reviewer Brian + Fiona **必須在同一 review cycle 確認**，不得各自分批 approve
> 5. 更新 errorCode 中央註冊表（`20260422_errorCodes_central.md`）
