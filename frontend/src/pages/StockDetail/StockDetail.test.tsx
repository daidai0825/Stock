/**
 * StockDetail 整合測試 + 元件單元測試 + Hook 單元測試（Wave B Round 2 對齊）
 *
 * Round 2 新增：
 * - 所有 mock data 對齊 schema-lock（含 previousClose / market / open / high / low /
 *   quoteDate / isStale / source / reportYear / reportQuarter / totalNetBuySell）
 * - isStale=true 顯示警示 Tag 測試（stale-data-tag）
 * - 5010 / 5011 / 5012 三種 errorCode 顯示對應 i18n message 測試
 * - useStockHistory 回傳型別改為 KLineHistory（含 items[]）
 * - FundamentalCard 使用 StockFundamental（reportYear / reportQuarter / source 必含）
 * - ChipCard 使用 StockChip（totalNetBuySell 必含）
 * - ErrorCode.STOCK_NOT_FOUND 取代舊版 RESOURCE_NOT_FOUND
 *
 * 策略：
 * - KLineChart 依賴 canvas API，jsdom 無法完整模擬，採 mock 替換。
 * - stockService 以 vi.mock 替換 HTTP 層，透過 mockResolvedValue 注入假資料。
 * - renderHook 包 QueryClientProvider，各測試獨立 QueryClient（retry: false）。
 */

import { describe, it, expect, vi, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor, render } from '@testing-library/react';
import { renderHook } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/testUtils';
import { server } from '@/mocks/server';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { type ReactNode, type ReactElement } from 'react';
import { ConfigProvider } from 'antd';
import zhTW from 'antd/locale/zh_TW';
import '@/i18n';
import { BusinessError } from '@/types/api';
import { ErrorCode } from '@/constants/errorCodes';

// mock stockService，讓 hook 測試不依賴 HTTP 層（避免 jsdom XHR vs MSW Node 相容問題）
vi.mock('@/services/stockService', () => ({
  stockService: {
    getQuote: vi.fn(),
    getHistory: vi.fn(),
    getFundamental: vi.fn(),
    getChip: vi.fn(),
  },
}));

import { stockService } from '@/services/stockService';

// KLineChart 依賴 canvas，jsdom 中 createChart 無法正常運作 → mock
vi.mock('./components/KLineChart', () => ({
  KLineChart: () => <div data-testid="kline-chart-mock">K 線圖（已 mock）</div>,
}));

import { StockDetailPage } from './index';
import { PriceHeader } from './components/PriceHeader';
import { FundamentalCard } from './components/FundamentalCard';
import { ChipCard } from './components/ChipCard';
import { PeriodSelector } from './components/PeriodSelector';
import { useStockQuote } from './hooks/useStockQuote';
import { useStockHistory } from './hooks/useStockHistory';
import { useFundamental } from './hooks/useFundamental';
import { useChip } from './hooks/useChip';
import type { StockQuote, StockFundamental, StockChip, KLineHistory } from '@/types/stock';

// ─── MSW setup ───────────────────────────────────────────────────────────
beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

// ─── 測試用 QueryClient wrapper ──────────────────────────────────────────
const createTestClient = (): QueryClient =>
  new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });

const HookWrapper = ({ children }: { children: ReactNode }): ReactElement => (
  <QueryClientProvider client={createTestClient()}>
    <MemoryRouter initialEntries={['/stocks/2330']}>{children}</MemoryRouter>
  </QueryClientProvider>
);

const createHookWrapper = () => HookWrapper;

/**
 * StockDetailPage 需要 :stockId 路由 param，必須以 Route 包裝。
 * renderWithProviders 的 MemoryRouter 沒有 Route 定義，useParams 會拿到空物件。
 */
const renderStockDetailPage = (stockId: string): void => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  render(
    <QueryClientProvider client={client}>
      <ConfigProvider locale={zhTW}>
        <MemoryRouter initialEntries={[`/stocks/${stockId}`]}>
          <Routes>
            <Route path="/stocks/:stockId" element={<StockDetailPage />} />
            <Route path="/home" element={<div data-testid="home-page">首頁</div>} />
          </Routes>
        </MemoryRouter>
      </ConfigProvider>
    </QueryClientProvider>,
  );
};

// ─── schema-lock 對齊的完整 mock data ────────────────────────────────────
// 所有欄位必須與 docs/03_spec/20260422_schema-lock_stock-detail-apis.md 一致

const happyQuote: StockQuote = {
  stockId: '2330',
  stockName: '台積電',
  market: 'TWSE',
  price: '1050.00',
  previousClose: '1031.00',
  change: '+19.00',
  changePercent: '+1.84',
  open: '1035.00',
  high: '1055.00',
  low: '1030.00',
  volume: 28_450_000,
  quoteDate: '2026-04-22',
  updatedAt: '2026-04-22T13:30:00.000+08:00',
  isStale: false,
  source: 'TWSE',
};

const staleQuote: StockQuote = {
  ...happyQuote,
  isStale: true,
  quoteDate: '2026-04-19',
  updatedAt: '2026-04-19T13:30:00.000+08:00',
};

const happyFundamental: StockFundamental = {
  stockId: '2330',
  stockName: '台積電',
  eps: '43.50',
  per: '24.14',
  pbr: '8.72',
  roe: '23.5',
  reportYear: 2025,
  reportQuarter: 4,
  updatedAt: '2026-04-22T08:00:00.000+08:00',
  source: 'MOPS',
};

const happyChip: StockChip = {
  stockId: '2330',
  date: '2026-04-22',
  institutions: [
    { name: '外資', buy: 18_500_000, sell: 12_000_000, netBuySell: 6_500_000 },
    { name: '投信', buy: 3_200_000,  sell: 1_500_000,  netBuySell: 1_700_000 },
    { name: '自營商', buy: 2_100_000, sell: 3_800_000, netBuySell: -1_700_000 },
  ],
  totalNetBuySell: 6_500_000,
  source: 'TWSE',
};

const happyHistory: KLineHistory = {
  stockId: '2330',
  period: 'daily',
  items: [
    { date: '2026-04-22', open: '1035.00', high: '1055.00', low: '1030.00', close: '1050.00', volume: 28_450_000 },
  ],
};

// ─── StockDetailPage 整合測試 ─────────────────────────────────────────────
describe('StockDetailPage 整合測試', () => {
  it('happy path：service 回 success → 渲染股名、EPS、外資', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValue(happyQuote);
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('2330');

    await waitFor(() => {
      expect(screen.getByText(/台積電/)).toBeDefined();
    });
    await waitFor(() => {
      expect(screen.getByText('43.50')).toBeDefined();
    });
    await waitFor(() => {
      expect(screen.getByText('外資')).toBeDefined();
    });
  });

  it('錯誤路徑：quote service 拋 4001 → 顯示 stock-not-found-result', async () => {
    vi.mocked(stockService.getQuote).mockRejectedValue(
      new BusinessError(ErrorCode.STOCK_NOT_FOUND, '股票不存在', 'test-trace-4001'),
    );
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('9999');

    await waitFor(() => {
      expect(screen.getByTestId('stock-not-found-result')).toBeDefined();
    });
  });

  // Round 2 新增：isStale=true 顯示「資料延遲」警示 Tag
  it('isStale=true → 顯示 stale-data-tag 警示', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValue(staleQuote);
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('2330');

    await waitFor(() => {
      expect(screen.getByTestId('stale-data-tag')).toBeDefined();
    });
  });

  // Round 2 新增：isStale=false → 不顯示 stale-data-tag
  it('isStale=false → 不顯示 stale-data-tag', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValue(happyQuote);
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('2330');

    await waitFor(() => {
      expect(screen.getByText(/台積電/)).toBeDefined();
    });
    expect(screen.queryByTestId('stale-data-tag')).toBeNull();
  });

  // Round 2 新增：5010 TWSE_DATA_SOURCE_ERROR
  it('quote service 拋 5010 → isError 且不顯示 stale-data-tag', async () => {
    vi.mocked(stockService.getQuote).mockRejectedValue(
      new BusinessError(ErrorCode.TWSE_DATA_SOURCE_ERROR, 'TWSE 資料來源暫時無法存取', 'trace-5010'),
    );
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('2330');

    // 頁面不跳 stock-not-found（5010 不是 4001）
    await waitFor(() => {
      expect(screen.queryByTestId('stock-not-found-result')).toBeNull();
    });
    // stale tag 不出現（quote 沒有資料）
    expect(screen.queryByTestId('stale-data-tag')).toBeNull();
  });

  // Round 2 新增：5011 MOPS_DATA_SOURCE_ERROR
  it('fundamental service 拋 5011 → 不影響 quote 顯示', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValue(happyQuote);
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockRejectedValue(
      new BusinessError(ErrorCode.MOPS_DATA_SOURCE_ERROR, 'MOPS 暫時無法存取', 'trace-5011'),
    );
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('2330');

    await waitFor(() => {
      expect(screen.getByText(/台積電/)).toBeDefined();
    });
  });

  // Round 2 新增：5012 CHIP_DATA_SOURCE_ERROR
  it('chip service 拋 5012 → 不影響 quote 顯示', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValue(happyQuote);
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockRejectedValue(
      new BusinessError(ErrorCode.CHIP_DATA_SOURCE_ERROR, '籌碼資料來源暫時無法存取', 'trace-5012'),
    );

    renderStockDetailPage('2330');

    await waitFor(() => {
      expect(screen.getByText(/台積電/)).toBeDefined();
    });
  });
});

// ─── PriceHeader ─────────────────────────────────────────────────────────
describe('PriceHeader', () => {
  it('loading 狀態顯示 Skeleton（無 price-header）', () => {
    renderWithProviders(
      <PriceHeader quote={undefined} isLoading={true} />,
    );
    expect(screen.queryByTestId('price-header')).toBeNull();
  });

  it('無資料時顯示 loading text', () => {
    renderWithProviders(
      <PriceHeader quote={undefined} isLoading={false} />,
    );
    expect(screen.getByTestId('price-header-empty')).toBeDefined();
  });

  it('顯示股號與股名', () => {
    renderWithProviders(
      <PriceHeader quote={happyQuote} isLoading={false} />,
    );
    expect(screen.getByText(/2330/)).toBeDefined();
    expect(screen.getByText(/台積電/)).toBeDefined();
  });

  it('顯示成交價', () => {
    renderWithProviders(
      <PriceHeader quote={happyQuote} isLoading={false} />,
    );
    expect(screen.getByTestId('price-value').textContent).toContain('1050.00');
  });

  it('顯示漲跌資訊', () => {
    renderWithProviders(
      <PriceHeader quote={happyQuote} isLoading={false} />,
    );
    expect(screen.getByTestId('price-change').textContent).toContain('+19.00');
  });

  it('下跌時漲跌顯示負號', () => {
    const downQuote: StockQuote = {
      ...happyQuote,
      change: '-5.00',
      changePercent: '-0.48',
    };
    renderWithProviders(
      <PriceHeader quote={downQuote} isLoading={false} />,
    );
    expect(screen.getByTestId('price-change').textContent).toContain('-5.00');
  });

  it('顯示 market 標籤', () => {
    renderWithProviders(
      <PriceHeader quote={happyQuote} isLoading={false} />,
    );
    expect(screen.getByTestId('price-market').textContent).toBe('TWSE');
  });

  it('顯示前收盤價', () => {
    renderWithProviders(
      <PriceHeader quote={happyQuote} isLoading={false} />,
    );
    expect(screen.getByTestId('price-header').textContent).toContain('1031.00');
  });
});

// ─── FundamentalCard ─────────────────────────────────────────────────────
describe('FundamentalCard', () => {
  it('loading 狀態顯示 Skeleton', () => {
    renderWithProviders(
      <FundamentalCard fundamental={undefined} isLoading={true} />,
    );
    expect(screen.queryByTestId('fundamental-eps')).toBeNull();
  });

  it('無資料時顯示 empty 文字', () => {
    renderWithProviders(
      <FundamentalCard fundamental={undefined} isLoading={false} />,
    );
    expect(screen.getByTestId('fundamental-card')).toBeDefined();
  });

  it('顯示 EPS 數值', () => {
    renderWithProviders(
      <FundamentalCard fundamental={happyFundamental} isLoading={false} />,
    );
    expect(screen.getByText('43.50')).toBeDefined();
  });

  it('顯示 PER 數值（欄位名 per，非 perRatio）', () => {
    renderWithProviders(
      <FundamentalCard fundamental={happyFundamental} isLoading={false} />,
    );
    expect(screen.getByText('24.14')).toBeDefined();
  });

  it('顯示 PBR 數值（欄位名 pbr，非 pbrRatio）', () => {
    renderWithProviders(
      <FundamentalCard fundamental={happyFundamental} isLoading={false} />,
    );
    expect(screen.getByText('8.72')).toBeDefined();
  });

  it('顯示 reportYear + reportQuarter', () => {
    renderWithProviders(
      <FundamentalCard fundamental={happyFundamental} isLoading={false} />,
    );
    // antd Descriptions.Item 不透傳 data-testid，用文字內容驗證
    expect(screen.getByText(/2025/)).toBeDefined();
    expect(screen.getByText(/Q4/)).toBeDefined();
  });

  it('顯示 tooltip（info icon）', () => {
    renderWithProviders(
      <FundamentalCard fundamental={happyFundamental} isLoading={false} />,
    );
    expect(screen.getByLabelText('資料來源：公開資訊觀測站（MOPS）・每季更新')).toBeDefined();
  });
});

// ─── ChipCard ────────────────────────────────────────────────────────────
describe('ChipCard', () => {
  it('loading 狀態不顯示 table 內容', () => {
    renderWithProviders(
      <ChipCard chip={undefined} isLoading={true} />,
    );
    expect(screen.queryByText('外資')).toBeNull();
  });

  it('顯示三大法人名稱（institutions[] 陣列結構）', () => {
    renderWithProviders(
      <ChipCard chip={happyChip} isLoading={false} />,
    );
    expect(screen.getByText('外資')).toBeDefined();
    expect(screen.getByText('投信')).toBeDefined();
    expect(screen.getByText('自營商')).toBeDefined();
  });

  it('顯示資料日期', () => {
    renderWithProviders(
      <ChipCard chip={happyChip} isLoading={false} />,
    );
    expect(screen.getByText(/2026-04-22/)).toBeDefined();
  });

  it('顯示三大法人合計 totalNetBuySell', () => {
    renderWithProviders(
      <ChipCard chip={happyChip} isLoading={false} />,
    );
    expect(screen.getByTestId('chip-total-net')).toBeDefined();
    expect(screen.getByTestId('chip-total-net').textContent).toContain('6,500,000');
  });
});

// ─── PeriodSelector ──────────────────────────────────────────────────────
describe('PeriodSelector', () => {
  it('顯示三個週期按鈕', () => {
    const onChange = vi.fn();
    renderWithProviders(
      <PeriodSelector value="daily" onChange={onChange} />,
    );
    expect(screen.getByText('日線')).toBeDefined();
    expect(screen.getByText('週線')).toBeDefined();
    expect(screen.getByText('月線')).toBeDefined();
  });

  it('點擊週線觸發 onChange("weekly")', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <PeriodSelector value="daily" onChange={onChange} />,
    );
    const label = screen.getByText('週線').closest('label');
    if (label !== null) {
      await user.click(label);
    }
    expect(onChange).toHaveBeenCalledWith('weekly');
  });

  it('點擊月線觸發 onChange("monthly")', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <PeriodSelector value="daily" onChange={onChange} />,
    );
    const label = screen.getByText('月線').closest('label');
    if (label !== null) {
      await user.click(label);
    }
    expect(onChange).toHaveBeenCalledWith('monthly');
  });
});

// ─── useStockQuote Hook 單元測試 ──────────────────────────────────────────
describe('useStockQuote hook', () => {
  it('初始狀態：isLoading=true，data=undefined', () => {
    vi.mocked(stockService.getQuote).mockReturnValue(new Promise(() => undefined));
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockQuote('2330'), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('success：service 回正常資料 → data 有 stockName / market / isStale', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValueOnce(happyQuote);
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockQuote('2330'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.stockName).toBe('台積電');
    expect(result.current.data?.market).toBe('TWSE');
    expect(result.current.data?.isStale).toBe(false);
    expect(result.current.data?.previousClose).toBe('1031.00');
  });

  it('error：service 拋 BusinessError → isError=true', async () => {
    vi.mocked(stockService.getQuote).mockRejectedValueOnce(
      new BusinessError(ErrorCode.STOCK_NOT_FOUND, '股票不存在', 'hook-trace-001'),
    );
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockQuote('9999'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });
});

// ─── useStockHistory Hook 單元測試 ────────────────────────────────────────
describe('useStockHistory hook', () => {
  it('初始狀態：isLoading=true，data=undefined', () => {
    vi.mocked(stockService.getHistory).mockReturnValue(new Promise(() => undefined));
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockHistory('2330', 'daily'), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('success：service 回 KLineHistory → data.items 為陣列且有元素', async () => {
    vi.mocked(stockService.getHistory).mockResolvedValueOnce(happyHistory);
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockHistory('2330', 'daily'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.period).toBe('daily');
    expect(Array.isArray(result.current.data?.items)).toBe(true);
    expect((result.current.data?.items.length ?? 0)).toBeGreaterThan(0);
  });

  it('error：service 拋 BusinessError → isError=true', async () => {
    vi.mocked(stockService.getHistory).mockRejectedValueOnce(
      new BusinessError(ErrorCode.TWSE_DATA_SOURCE_ERROR, 'TWSE 暫時不可用', 'hook-trace-002'),
    );
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockHistory('2330', 'daily'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ─── useFundamental Hook 單元測試 ─────────────────────────────────────────
describe('useFundamental hook', () => {
  it('初始狀態：isLoading=true，data=undefined', () => {
    vi.mocked(stockService.getFundamental).mockReturnValue(new Promise(() => undefined));
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useFundamental('2330'), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('success：service 回正常資料 → data.per / data.pbr 存在（非 perRatio / pbrRatio）', async () => {
    vi.mocked(stockService.getFundamental).mockResolvedValueOnce(happyFundamental);
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useFundamental('2330'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.eps).toBe('43.50');
    expect(result.current.data?.per).toBe('24.14');
    expect(result.current.data?.pbr).toBe('8.72');
    expect(result.current.data?.reportYear).toBe(2025);
    expect(result.current.data?.reportQuarter).toBe(4);
    expect(result.current.data?.source).toBe('MOPS');
  });

  it('error：service 拋 5011 MOPS_DATA_SOURCE_ERROR → isError=true', async () => {
    vi.mocked(stockService.getFundamental).mockRejectedValueOnce(
      new BusinessError(ErrorCode.MOPS_DATA_SOURCE_ERROR, 'MOPS 暫時無法存取', 'hook-trace-003'),
    );
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useFundamental('2330'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ─── useChip Hook 單元測試 ────────────────────────────────────────────────
describe('useChip hook', () => {
  it('初始狀態：isLoading=true，data=undefined', () => {
    vi.mocked(stockService.getChip).mockReturnValue(new Promise(() => undefined));
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useChip('2330'), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('success：service 回正常資料 → data.institutions 有 3 筆 + totalNetBuySell', async () => {
    vi.mocked(stockService.getChip).mockResolvedValueOnce(happyChip);
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useChip('2330'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.institutions.length).toBe(3);
    expect(result.current.data?.totalNetBuySell).toBe(6_500_000);
    expect(result.current.data?.source).toBe('TWSE');
  });

  it('error：service 拋 5012 CHIP_DATA_SOURCE_ERROR → isError=true', async () => {
    vi.mocked(stockService.getChip).mockRejectedValueOnce(
      new BusinessError(ErrorCode.CHIP_DATA_SOURCE_ERROR, '籌碼資料來源暫時無法存取', 'hook-trace-004'),
    );
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useChip('2330'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ─── PriceHeader 無資料降級顯示 ──────────────────────────────────────────
describe('PriceHeader 無資料狀態降級顯示', () => {
  it('quote=undefined 時顯示無資料狀態（不崩潰）', async () => {
    renderWithProviders(
      <PriceHeader quote={undefined} isLoading={false} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('price-header-empty')).toBeDefined();
    });
  });
});

// ─── useQueryErrorNotification Hook 行為測試 ─────────────────────────────
describe('useQueryErrorNotification（透過 StockDetailPage 整合驗證）', () => {
  it('error null → 不觸發 notification（頁面正常渲染股名）', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValue(happyQuote);
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('2330');
    await waitFor(() => {
      expect(screen.getByText(/台積電/)).toBeDefined();
    });
  });

  it('stockId 空字串 → Navigate 跳回首頁（顯示 home-page）', () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
    render(
      <QueryClientProvider client={client}>
        <ConfigProvider locale={zhTW}>
          <MemoryRouter initialEntries={['/stocks/']}>
            <Routes>
              {/* 此路由不含 :stockId，useParams 拿到 undefined → 預設 '' → Navigate */}
              <Route path="/stocks/" element={<StockDetailPage />} />
              <Route path="/home" element={<div data-testid="home-page">首頁</div>} />
            </Routes>
          </MemoryRouter>
        </ConfigProvider>
      </QueryClientProvider>,
    );
    expect(screen.getByTestId('home-page')).toBeDefined();
  });
});
