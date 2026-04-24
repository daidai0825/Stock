# Wave 2 前端開發者指南

| 項目 | 內容 |
|------|------|
| **版本** | v1.0 |
| **日期** | 2026-04-23 |
| **對象** | 前端開發工程師 |
| **涉及技術** | React 18+、TypeScript、Vite、Ant Design |
| **對應規格** | [schema-lock_stock-detail-apis.md](../03_spec/20260422_schema-lock_stock-detail-apis.md)、[errorCodes_central.md](../03_spec/20260422_errorCodes_central.md) |

---

## 快速開始

### 本機開發環境設定

#### 1. 前置需求

```bash
node --version  # 需 20+ LTS
npm --version   # 需 10+
```

#### 2. 安裝與啟動

```bash
# clone 專案
git clone https://github.com/your-org/stock-platform.git
cd Stock/frontend

# 安裝依賴
npm ci  # 使用 package-lock.json（比 npm install 穩定）

# 啟動開發伺服器
npm run dev
# → 預設 http://localhost:5173

# 構建生產版本
npm run build

# 執行單元測試
npm run test

# 執行 E2E 測試（需 Playwright 環境）
npm run test:e2e
```

#### 3. IDE 設定

**推薦：VS Code**

必要外掛：
- ESLint
- Prettier
- TypeScript Vue Plugin（若有 Vue 元件）
- Vitest（測試執行）

`.vscode/settings.json` 範本：

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "[typescript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  }
}
```

---

## 專案結構

```
frontend/
├── src/
│   ├── components/
│   │   ├── common/              # Ant Design 封裝元件
│   │   │   ├── Loading.tsx
│   │   │   └── ErrorBoundary.tsx
│   │   └── business/            # 業務元件
│   │       ├── StockDetail/
│   │       │   ├── index.tsx
│   │       │   ├── components/
│   │       │   │   ├── PriceHeader.tsx
│   │       │   │   ├── KLineChart.tsx
│   │       │   │   ├── FundamentalCard.tsx
│   │       │   │   └── ChipCard.tsx
│   │       │   ├── hooks/
│   │       │   │   ├── useStockHistory.ts
│   │       │   │   └── useQuoteErrorNotification.ts
│   │       │   └── StockDetail.test.tsx
│   │       └── ...
│   ├── pages/
│   │   ├── StockDetail/
│   │   ├── Watchlist/
│   │   └── ...
│   ├── hooks/                   # 共用 hooks
│   │   ├── useQuery/
│   │   └── ...
│   ├── services/                # API 呼叫層
│   │   ├── http.ts              # Axios 設定 + 攔截器
│   │   ├── quoteService.ts
│   │   ├── fundamentalService.ts
│   │   ├── chipService.ts
│   │   └── ...
│   ├── types/                   # TypeScript 型別定義
│   │   ├── api.ts               # API response 型別
│   │   ├── domain.ts            # 業務域型別
│   │   └── ...
│   ├── constants/               # 常數
│   │   ├── errorCodes.ts        # 錯誤碼常數
│   │   └── ...
│   ├── utils/                   # 工具函式
│   │   ├── formatters.ts        # 數字格式化
│   │   └── ...
│   ├── i18n/
│   │   ├── index.ts             # i18n 初始化
│   │   ├── zh-TW.json           # 中文翻譯
│   │   └── en.json              # 英文翻譯
│   ├── mocks/
│   │   ├── handlers.ts          # MSW handlers
│   │   └── server.ts            # MSW server 配置
│   ├── App.tsx
│   └── main.tsx
├── vite.config.ts
├── tsconfig.json
├── vitest.config.ts
├── .prettierrc
├── .eslintrc.cjs
└── package.json
```

---

## StockDetail 頁面深入說明

### 頁面架構

```
StockDetail/
├── 搜尋欄（輸入股票代號）
├── PriceHeader
│   └── 行情卡（價格、漲跌、成交量、更新時間 + isStale 標記）
├── KLineChart
│   └── K 線圖（日 / 週 / 月切換，90 天範圍）
├── FundamentalCard
│   └── 基本面（EPS / PE / PB / ROE）
└── ChipCard
    └── 籌碼（三大法人陣列，合計買賣超）
```

### 主要元件

#### PriceHeader（行情卡）

```typescript
// PriceHeader.tsx
import { type FC } from 'react';
import { Card, Statistic, Alert, Space } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { type StockQuote } from '@/types/api';

interface PriceHeaderProps {
  quote: StockQuote;
}

export const PriceHeader: FC<PriceHeaderProps> = ({ quote }) => {
  const isPositive = !quote.change.startsWith('-');

  return (
    <Card title={`${quote.stockName} (${quote.stockId})`}>
      {quote.isStale && (
        <Alert
          type="warning"
          message={`資料延遲，最後更新：${quote.quoteDate}`}
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Statistic
          title="現價"
          value={quote.price}
          precision={2}
          prefix="$"
          valueStyle={{ color: isPositive ? '#52c41a' : '#ff4d4f' }}
          suffix={
            isPositive ? (
              <ArrowUpOutlined style={{ color: '#52c41a' }} />
            ) : (
              <ArrowDownOutlined style={{ color: '#ff4d4f' }} />
            )
          }
        />
        <Statistic
          title="漲跌"
          value={`${quote.change} (${quote.changePercent}%)`}
          valueStyle={{ color: isPositive ? '#52c41a' : '#ff4d4f' }}
        />
        <Statistic
          title="成交量"
          value={quote.volume.toLocaleString()}
          suffix="股"
        />
      </Space>
    </Card>
  );
};
```

#### KLineChart（K 線圖）

```typescript
// KLineChart.tsx
import { type FC, useState } from 'react';
import { Card, Radio, Skeleton } from 'antd';
import { useStockHistory } from '../hooks/useStockHistory';
import { FinancialChart } from '@/components/common/FinancialChart';
import { type KLinePeriod } from '@/types/api';

interface KLineChartProps {
  stockId: string;
}

export const KLineChart: FC<KLineChartProps> = ({ stockId }) => {
  const [period, setPeriod] = useState<KLinePeriod>('daily');
  const { data, isLoading, error } = useStockHistory(stockId, period);

  if (isLoading) return <Skeleton />;

  return (
    <Card title="K 線圖" style={{ marginTop: 16 }}>
      <Radio.Group value={period} onChange={(e) => setPeriod(e.target.value)}>
        <Radio.Button value="daily">日線</Radio.Button>
        <Radio.Button value="weekly">週線</Radio.Button>
        <Radio.Button value="monthly">月線</Radio.Button>
      </Radio.Group>
      {error ? (
        <div style={{ color: 'red', marginTop: 16 }}>{error.message}</div>
      ) : data ? (
        <FinancialChart data={data.items} />
      ) : null}
    </Card>
  );
};
```

#### FundamentalCard（基本面卡）

```typescript
// FundamentalCard.tsx
import { type FC } from 'react';
import { Card, Row, Col, Statistic, Empty } from 'antd';
import { type StockFundamental } from '@/types/api';

interface FundamentalCardProps {
  fundamental: StockFundamental | null;
  error?: Error | null;
}

export const FundamentalCard: FC<FundamentalCardProps> = ({
  fundamental,
  error,
}) => {
  if (error) {
    return (
      <Card title="基本面">
        <Empty description="資料暫時無法載入" />
      </Card>
    );
  }

  if (!fundamental) {
    return <Card title="基本面"><Skeleton /></Card>;
  }

  return (
    <Card
      title={`基本面 (${fundamental.reportYear}年第${fundamental.reportQuarter}季)`}
    >
      <Row gutter={16}>
        <Col span={6}>
          <Statistic title="EPS" value={fundamental.eps} precision={2} />
        </Col>
        <Col span={6}>
          <Statistic title="PE" value={fundamental.per} precision={2} />
        </Col>
        <Col span={6}>
          <Statistic title="PB" value={fundamental.pbr} precision={2} />
        </Col>
        <Col span={6}>
          <Statistic title="ROE" value={`${fundamental.roe}%`} />
        </Col>
      </Row>
    </Card>
  );
};
```

#### ChipCard（籌碼卡）

```typescript
// ChipCard.tsx
import { type FC } from 'react';
import { Card, Table, Empty } from 'antd';
import { type StockChip } from '@/types/api';

interface ChipCardProps {
  chip: StockChip | null;
  error?: Error | null;
}

export const ChipCard: FC<ChipCardProps> = ({ chip, error }) => {
  if (error) {
    return (
      <Card title="籌碼">
        <Empty description="籌碼資料暫時無法載入" />
      </Card>
    );
  }

  if (!chip) return null;

  const columns = [
    { title: '法人', dataIndex: 'name', key: 'name' },
    {
      title: '買進',
      dataIndex: 'buy',
      key: 'buy',
      render: (val: number) => val.toLocaleString(),
    },
    {
      title: '賣出',
      dataIndex: 'sell',
      key: 'sell',
      render: (val: number) => val.toLocaleString(),
    },
    {
      title: '買賣超',
      dataIndex: 'netBuySell',
      key: 'netBuySell',
      render: (val: number) => (
        <span style={{ color: val >= 0 ? '#52c41a' : '#ff4d4f' }}>
          {val >= 0 ? '+' : ''}{val.toLocaleString()}
        </span>
      ),
    },
  ];

  return (
    <Card title={`籌碼 (${chip.date})`}>
      <Table
        columns={columns}
        dataSource={chip.institutions}
        rowKey="name"
        pagination={false}
      />
      <div style={{ marginTop: 16, fontSize: 16, fontWeight: 'bold' }}>
        三大法人合計：
        <span
          style={{
            color: chip.totalNetBuySell >= 0 ? '#52c41a' : '#ff4d4f',
            marginLeft: 8,
          }}
        >
          {chip.totalNetBuySell >= 0 ? '+' : ''}
          {chip.totalNetBuySell.toLocaleString()} 股
        </span>
      </div>
    </Card>
  );
};
```

---

## Hook 與服務層

### useStockHistory：K 線資料 Hook

```typescript
// hooks/useStockHistory.ts
import { useQuery } from '@tanstack/react-query';
import { quoteService } from '@/services/quoteService';
import { type KLinePeriod, type KLineHistory } from '@/types/api';

export const useStockHistory = (stockId: string, period: KLinePeriod) => {
  return useQuery({
    queryKey: ['stockHistory', stockId, period],
    queryFn: async (): Promise<KLineHistory> => {
      const res = await quoteService.getHistory(stockId, period);
      return res.data;
    },
    staleTime: 60_000, // 60 秒後重新驗証
    cacheTime: 10 * 60_000, // 快取 10 分鐘
    retry: (failureCount, error) => {
      // 5014 不重試，其他錯誤重試 2 次
      return failureCount < 2 && 'code' in error && error.code !== 5014;
    },
  });
};
```

### API Service 層

```typescript
// services/quoteService.ts
import { api } from './http';
import { type StockQuote, type KLineHistory } from '@/types/api';

export const quoteService = {
  async getQuote(stockId: string): Promise<{ data: StockQuote }> {
    return api.post('/api/v1/quote/get', { stockId });
  },

  async getQuoteList(
    stockIds: string[],
    market?: 'TWSE' | 'OTC' | 'ALL'
  ): Promise<{ data: StockQuote[] }> {
    return api.post('/api/v1/quote/list', {
      stockIds,
      market: market ?? 'ALL',
    });
  },

  async getHistory(
    stockId: string,
    period: 'daily' | 'weekly' | 'monthly',
    startDate?: string,
    endDate?: string
  ): Promise<{ data: KLineHistory }> {
    return api.post('/api/v1/quote/history', {
      stockId,
      period,
      startDate,
      endDate,
    });
  },
};
```

### HTTP 攔截器與錯誤處理

```typescript
// services/http.ts
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { BusinessError } from '@/utils/errors';
import { ErrorCode } from '@/constants/errorCodes';

export const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 請求攔截器：注入 token
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 回應攔截器：統一處理 Envelope + 錯誤
api.interceptors.response.use(
  (response) => {
    const { code, message, data, traceId } = response.data;

    // code !== 0 表示業務錯誤
    if (code !== 0) {
      throw new BusinessError(code, message, traceId);
    }

    return { ...response, data };
  },
  (error) => {
    // 網路層錯誤（無 response）
    if (!error.response) {
      throw new Error('Network error, please check your connection');
    }

    // 非 200 的 HTTP 狀態碼（原則上不應發生，API 統一 200）
    if (error.response.status === 401) {
      // token 過期或無效，導回登入
      window.location.href = '/login';
    }

    throw error;
  }
);
```

---

## 多語言（i18n）

### 初始化

```typescript
// i18n/index.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import zhTW from './zh-TW.json';
import en from './en.json';

const resolveInitialLang = (): 'zh-TW' | 'en' => {
  // 優先級：query param > localStorage > 預設
  const params = new URLSearchParams(window.location.search);
  const queryLang = params.get('lang');
  if (queryLang === 'en' || queryLang === 'zh-TW') return queryLang;

  const stored = localStorage.getItem('i18nextLng');
  if (stored === 'en' || stored === 'zh-TW') return stored;

  return 'zh-TW';
};

void i18n.use(initReactI18next).init({
  resources: {
    'zh-TW': { translation: zhTW },
    en: { translation: en },
  },
  lng: resolveInitialLang(),
  fallbackLng: 'zh-TW',
  interpolation: { escapeValue: false },
  returnNull: false,
});
```

### 翻譯檔案結構

```json
// i18n/zh-TW.json
{
  "common": {
    "loading": "載入中...",
    "retry": "重試"
  },
  "errors": {
    "5010": "TWSE 資料來源暫時無法存取",
    "5014": "MOPS 資料格式異動，請聯絡系統管理員"
  },
  "stockDetail": {
    "priceHeader": "行情",
    "klineChart": "K 線圖"
  }
}
```

### 使用 i18n

```typescript
import { useTranslation } from 'react-i18next';

const MyComponent = () => {
  const { t, i18n } = useTranslation();

  return (
    <div>
      <p>{t('common.loading')}</p>
      <button onClick={() => i18n.changeLanguage('en')}>English</button>
    </div>
  );
};
```

---

## 錯誤處理

### BusinessError 類別

```typescript
// utils/errors.ts
export class BusinessError extends Error {
  constructor(
    public code: number,
    public message: string,
    public traceId?: string
  ) {
    super(message);
    this.name = 'BusinessError';
  }
}
```

### 錯誤通知 Hook

```typescript
// hooks/useQueryErrorNotification.ts
import { useEffect } from 'react';
import { notification } from 'antd';
import { useTranslation } from 'react-i18next';
import { isFatalDataSourceError } from '@/constants/errorCodes';
import type { BusinessError } from '@/utils/errors';

export const useQueryErrorNotification = (
  error: Error | null,
  context: string
): void => {
  const { t } = useTranslation();

  useEffect(() => {
    if (error === null) return;

    const isBusiness = error instanceof BusinessError;
    const code = isBusiness ? error.code : undefined;
    const traceId = isBusiness ? error.traceId : undefined;

    // 獲取 i18n 翻譯的錯誤訊息
    const errorMessage = isBusiness
      ? (t(`errors.${code}`, { defaultValue: error.message }) as string)
      : error.message;

    // 區分 fatal（5014）vs. recoverable（5010-5013） 錯誤
    let description = '';
    if (code && isFatalDataSourceError(code)) {
      description = `此錯誤需後端工程師處理，無法透過重試解決。traceId：${traceId}`;
    } else {
      description = traceId ? `traceId：${traceId}` : '';
    }

    notification.error({
      message: `${context}：${errorMessage}`,
      description,
      duration: 5,
    });
  }, [error, context, t]);
};
```

### 錯誤碼常數

```typescript
// constants/errorCodes.ts
export const ErrorCode = {
  SUCCESS: 0,
  PARAM_REQUIRED: 1001,
  PARAM_FORMAT_INVALID: 1002,
  PARAM_OUT_OF_RANGE: 1003,
  UNAUTHORIZED: 3001,
  TOKEN_EXPIRED: 3002,
  TOKEN_INVALID: 3003,
  STOCK_NOT_FOUND: 4001,
  TWSE_DATA_SOURCE_ERROR: 5010,
  MOPS_DATA_SOURCE_ERROR: 5011,
  CHIP_DATA_SOURCE_ERROR: 5012,
  OTC_DATA_SOURCE_ERROR: 5013,
  MOPS_FORMAT_CHANGED: 5014,
} as const;

/** 結構性資料來源錯誤（重試無法解決，需工程師介入） */
const FATAL_DATA_SOURCE_ERROR_CODES: ReadonlySet<number> = new Set([
  ErrorCode.MOPS_FORMAT_CHANGED, // 5014
]);

export const isFatalDataSourceError = (code: number): boolean =>
  FATAL_DATA_SOURCE_ERROR_CODES.has(code);
```

---

## MSW Mock 用法（本機與測試）

### 啟用 MSW

```typescript
// main.tsx
import { enableMocks } from '@/mocks';

async function bootstrapApp() {
  if (import.meta.env.DEV) {
    await enableMocks();
  }

  createRoot(document.getElementById('root')!).render(<App />);
}

bootstrapApp();
```

### Mock Handlers

```typescript
// mocks/handlers.ts
import { http, HttpResponse } from 'msw';

export const handlers = [
  http.post('/api/v1/quote/get', async ({ request }) => {
    const { stockId } = await request.json() as { stockId: string };

    if (stockId === '2330') {
      return HttpResponse.json({
        code: 0,
        message: 'success',
        data: {
          stockId: '2330',
          stockName: '台積電',
          market: 'TWSE',
          price: '1050.00',
          previousClose: '1031.00',
          change: '+19.00',
          changePercent: '+1.84',
          // ...
        },
        timestamp: new Date().toISOString(),
        traceId: 'mock-trace',
      });
    }

    return HttpResponse.json(
      {
        code: 4001,
        message: 'Stock not found',
        data: null,
      },
      { status: 200 }
    );
  }),
];
```

---

## 測試

### 元件單元測試

```typescript
// StockDetail.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StockDetail } from './index';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';

const queryClient = new QueryClient();

describe('StockDetail', () => {
  it('should display stock quote after loading', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={i18n}>
          <StockDetail stockId="2330" />
        </I18nextProvider>
      </QueryClientProvider>
    );

    // 驗證初始載入狀態
    expect(screen.getByText(/載入中/i)).toBeInTheDocument();

    // 等待資料載入
    await waitFor(() => {
      expect(screen.getByText('台積電')).toBeInTheDocument();
    });

    // 驗證行情卡內容
    expect(screen.getByText('1050.00')).toBeInTheDocument();
    expect(screen.getByText('+1.84')).toBeInTheDocument();
  });

  it('should handle data source errors gracefully', async () => {
    // 模擬 API 錯誤
    server.use(
      http.post('/api/v1/quote/get', () =>
        HttpResponse.json(
          {
            code: 5010,
            message: 'TWSE data source is temporarily unavailable',
            data: null,
          },
          { status: 200 }
        )
      )
    );

    render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={i18n}>
          <StockDetail stockId="2330" />
        </I18nextProvider>
      </QueryClientProvider>
    );

    // 驗證錯誤提示
    await waitFor(() => {
      expect(
        screen.getByText(/TWSE 資料來源暫時無法存取/i)
      ).toBeInTheDocument();
    });
  });
});
```

---

## 效能優化建議

### 1. Code Splitting

```typescript
// 懶載入 StockDetail 頁面
import { lazy } from 'react';

const StockDetail = lazy(() => import('@/pages/StockDetail'));
```

### 2. 防止過度重新渲染

使用 `React.memo` 封裝元件：

```typescript
export const PriceHeader = React.memo(function PriceHeaderComponent({
  quote,
}: PriceHeaderProps) {
  // ...
});
```

### 3. 快取與預取

```typescript
// 預取相鄰頁面資料
const { prefetchQuery } = useQueryClient();

const handleStockSelect = (stockId: string) => {
  prefetchQuery({
    queryKey: ['quote', stockId],
    queryFn: () => quoteService.getQuote(stockId),
  });
};
```

---

## 常見問題

**Q：為什麼 i18n 語言切換後頁面沒反應？**  
A：確保在 `i18n/index.ts` 中 `lng` 設定**讀取 localStorage**。詳見本文件「多語言」章節的 `resolveInitialLang()` 函式。

**Q：5014 MOPS_FORMAT_CHANGED 錯誤為什麼不應重試？**  
A：5014 是結構性錯誤（parser 解析失敗），重試 100 次也無法恢復。需後端工程師修改 parser 邏輯才能解決。建議前端顯示「聯絡管理員」而非「稍後重試」。

**Q：BigDecimal 為什麼序列化為字串？**  
A：避免 JavaScript 浮點精度問題。例如：`1050.00 * 100 = 105000.00000000001`（浮點誤差）。用字串則精度無損。前端顯示時需手動 `parseFloat()` 轉數字。

---

## 相關文件

- [API 對外手冊](20260423_wave2_api-handbook.md)
- [schema-lock](../03_spec/20260422_schema-lock_stock-detail-apis.md)
- [errorCodes 中央化](../03_spec/20260422_errorCodes_central.md)

---

**文件版本**：v1.0  
**最後更新**：2026-04-23  
**維護人員**：@Felix（資深前端工程師）
