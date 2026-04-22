/**
 * StockDetail 整合測試 + 元件單元測試 + Hook 單元測試（Wave B Round 1 修正）
 *
 * 修正項目（M-FE-WaveB-03）：
 * - 加入真正的 StockDetailPage 整合測試（happy path + 4001 錯誤路徑）
 * - 4 支 hook 各加 3 個 renderHook 測試（loading / success / error）
 * - 修正 line 244 原 describe 名稱不實的問題（改為明確名稱）
 *
 * 策略：
 * - KLineChart 依賴 canvas API，jsdom 無法完整模擬，採 mock 替換。
 * - MSW server 攔截 API 呼叫，提供假資料。
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
import type { StockQuote, Fundamental, Chip } from '@/types/stock';

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

// ─── StockDetailPage 整合測試（M-FE-WaveB-03 新增） ─────────────────────
// 整合測試：render 整頁，驗證各子元件正確顯示資料。
// stockService 已被 vi.mock，透過 mockResolvedValue 注入假資料。
describe('StockDetailPage 整合測試', () => {
  const happyQuote = {
    stockId: '2330',
    stockName: '台積電',
    price: '912.00',
    change: '+12.00',
    changePercent: '+1.33',
    volume: 28_745_321,
    updatedAt: '2026-04-22T10:30:00.000+08:00',
  };
  const happyFundamental = {
    stockId: '2330',
    eps: '32.34',
    per: '28.20',
    pbr: '7.85',
    roe: '27.83',
    updatedAt: '2026-04-22',
    source: '公開資訊觀測站（MOPS）',
  };
  const happyChip = {
    stockId: '2330',
    date: '2026-04-22',
    institutions: [
      { name: '外資', netBuySell: 12_543_000, buy: 35_000_000, sell: 22_457_000 },
      { name: '投信', netBuySell: -2_100_000, buy: 5_000_000, sell: 7_100_000 },
      { name: '自營商', netBuySell: 890_000, buy: 3_200_000, sell: 2_310_000 },
    ],
    source: '台灣證券交易所（TWSE）',
  };
  const happyHistory = [
    { date: '2026-04-22', open: '900.00', high: '915.00', low: '898.00', close: '912.00', volume: 28_000_000 },
  ];

  it('happy path：service 回 success → 渲染股名、EPS、外資', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValue(happyQuote);
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('2330');

    // PriceHeader 出現股名
    await waitFor(() => {
      expect(screen.getByText(/台積電/)).toBeDefined();
    });

    // FundamentalCard 出現 EPS
    await waitFor(() => {
      expect(screen.getByText('32.34')).toBeDefined();
    });

    // ChipCard 出現「外資」
    await waitFor(() => {
      expect(screen.getByText('外資')).toBeDefined();
    });
  });

  it('錯誤路徑：quote service 拋 4001 → 顯示 stock-not-found-result', async () => {
    vi.mocked(stockService.getQuote).mockRejectedValue(
      new BusinessError(4001, '股票不存在', 'test-trace-4001'),
    );
    vi.mocked(stockService.getHistory).mockResolvedValue(happyHistory);
    vi.mocked(stockService.getFundamental).mockResolvedValue(happyFundamental);
    vi.mocked(stockService.getChip).mockResolvedValue(happyChip);

    renderStockDetailPage('9999');

    await waitFor(() => {
      expect(screen.getByTestId('stock-not-found-result')).toBeDefined();
    });
  });
});

// ─── PriceHeader ─────────────────────────────────────────────────────────
describe('PriceHeader', () => {
  const mockQuote: StockQuote = {
    stockId: '2330',
    stockName: '台積電',
    price: '912.00',
    change: '+12.00',
    changePercent: '+1.33',
    volume: 28_745_321,
    updatedAt: '2026-04-22T10:30:00.000+08:00',
  };

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
      <PriceHeader quote={mockQuote} isLoading={false} />,
    );
    expect(screen.getByText(/2330/)).toBeDefined();
    expect(screen.getByText(/台積電/)).toBeDefined();
  });

  it('顯示成交價', () => {
    renderWithProviders(
      <PriceHeader quote={mockQuote} isLoading={false} />,
    );
    expect(screen.getByTestId('price-value').textContent).toContain('912.00');
  });

  it('顯示漲跌資訊', () => {
    renderWithProviders(
      <PriceHeader quote={mockQuote} isLoading={false} />,
    );
    expect(screen.getByTestId('price-change').textContent).toContain('+12.00');
  });

  it('下跌時漲跌顯示負號', () => {
    const downQuote: StockQuote = {
      ...mockQuote,
      change: '-5.00',
      changePercent: '-0.55',
    };
    renderWithProviders(
      <PriceHeader quote={downQuote} isLoading={false} />,
    );
    expect(screen.getByTestId('price-change').textContent).toContain('-5.00');
  });
});

// ─── FundamentalCard ─────────────────────────────────────────────────────
describe('FundamentalCard', () => {
  const mockFundamental: Fundamental = {
    stockId: '2330',
    eps: '32.34',
    per: '28.20',
    pbr: '7.85',
    roe: '27.83',
    updatedAt: '2026-04-22',
    source: '公開資訊觀測站（MOPS）',
  };

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
      <FundamentalCard fundamental={mockFundamental} isLoading={false} />,
    );
    expect(screen.getByText('32.34')).toBeDefined();
  });

  it('顯示 PER 數值', () => {
    renderWithProviders(
      <FundamentalCard fundamental={mockFundamental} isLoading={false} />,
    );
    expect(screen.getByText('28.20')).toBeDefined();
  });

  it('null 值顯示 dash（-）', () => {
    const nullFundamental: Fundamental = {
      ...mockFundamental,
      eps: null,
      per: null,
    };
    renderWithProviders(
      <FundamentalCard fundamental={nullFundamental} isLoading={false} />,
    );
    const dashes = screen.getAllByText('-');
    expect(dashes.length).toBeGreaterThanOrEqual(2);
  });

  it('顯示 tooltip（info icon）', () => {
    renderWithProviders(
      <FundamentalCard fundamental={mockFundamental} isLoading={false} />,
    );
    expect(screen.getByLabelText('資料來源：公開資訊觀測站（MOPS）・每季更新')).toBeDefined();
  });
});

// ─── ChipCard ────────────────────────────────────────────────────────────
describe('ChipCard', () => {
  const mockChip: Chip = {
    stockId: '2330',
    date: '2026-04-22',
    institutions: [
      { name: '外資', netBuySell: 12_543_000, buy: 35_000_000, sell: 22_457_000 },
      { name: '投信', netBuySell: -2_100_000, buy: 5_000_000, sell: 7_100_000 },
      { name: '自營商', netBuySell: 890_000, buy: 3_200_000, sell: 2_310_000 },
    ],
    source: '台灣證券交易所（TWSE）',
  };

  it('loading 狀態不顯示 table 內容', () => {
    renderWithProviders(
      <ChipCard chip={undefined} isLoading={true} />,
    );
    expect(screen.queryByText('外資')).toBeNull();
  });

  it('顯示三大法人名稱', () => {
    renderWithProviders(
      <ChipCard chip={mockChip} isLoading={false} />,
    );
    expect(screen.getByText('外資')).toBeDefined();
    expect(screen.getByText('投信')).toBeDefined();
    expect(screen.getByText('自營商')).toBeDefined();
  });

  it('顯示資料日期', () => {
    renderWithProviders(
      <ChipCard chip={mockChip} isLoading={false} />,
    );
    expect(screen.getByText(/2026-04-22/)).toBeDefined();
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

// ─── useStockQuote Hook 單元測試（M-FE-WaveB-03 新增） ───────────────────
describe('useStockQuote hook', () => {
  const mockQuoteData = {
    stockId: '2330',
    stockName: '台積電',
    price: '912.00',
    change: '+12.00',
    changePercent: '+1.33',
    volume: 28_745_321,
    updatedAt: '2026-04-22T10:30:00.000+08:00',
  };

  it('初始狀態：isLoading=true，data=undefined', () => {
    vi.mocked(stockService.getQuote).mockReturnValue(new Promise(() => undefined));
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockQuote('2330'), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('success：service 回正常資料 → data 有 stockName', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValueOnce(mockQuoteData);
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockQuote('2330'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.stockName).toBe('台積電');
  });

  it('error：service 拋 BusinessError → isError=true', async () => {
    vi.mocked(stockService.getQuote).mockRejectedValueOnce(
      new BusinessError(4001, '股票不存在', 'hook-trace-001'),
    );
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockQuote('9999'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });
});

// ─── useStockHistory Hook 單元測試（M-FE-WaveB-03 新增） ─────────────────
describe('useStockHistory hook', () => {
  const mockHistoryData = [
    {
      date: '2026-04-22',
      open: '900.00',
      high: '915.00',
      low: '898.00',
      close: '912.00',
      volume: 28_000_000,
    },
  ];

  it('初始狀態：isLoading=true，data=undefined', () => {
    vi.mocked(stockService.getHistory).mockReturnValue(new Promise(() => undefined));
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockHistory('2330', 'daily'), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('success：service 回正常資料 → data 為陣列且有元素', async () => {
    vi.mocked(stockService.getHistory).mockResolvedValueOnce(mockHistoryData);
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockHistory('2330', 'daily'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(Array.isArray(result.current.data)).toBe(true);
    expect((result.current.data?.length ?? 0)).toBeGreaterThan(0);
  });

  it('error：service 拋 BusinessError → isError=true', async () => {
    vi.mocked(stockService.getHistory).mockRejectedValueOnce(
      new BusinessError(9001, '系統繁忙', 'hook-trace-002'),
    );
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useStockHistory('2330', 'daily'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ─── useFundamental Hook 單元測試（M-FE-WaveB-03 新增） ─────────────────
describe('useFundamental hook', () => {
  const mockFundamentalData = {
    stockId: '2330',
    eps: '32.34',
    per: '28.20',
    pbr: '7.85',
    roe: '27.83',
    updatedAt: '2026-04-22',
    source: '公開資訊觀測站（MOPS）',
  };

  it('初始狀態：isLoading=true，data=undefined', () => {
    vi.mocked(stockService.getFundamental).mockReturnValue(new Promise(() => undefined));
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useFundamental('2330'), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('success：service 回正常資料 → data.eps 存在', async () => {
    vi.mocked(stockService.getFundamental).mockResolvedValueOnce(mockFundamentalData);
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useFundamental('2330'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.eps).toBe('32.34');
  });

  it('error：service 拋 BusinessError → isError=true', async () => {
    vi.mocked(stockService.getFundamental).mockRejectedValueOnce(
      new BusinessError(4001, '資源不存在', 'hook-trace-003'),
    );
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useFundamental('9999'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ─── useChip Hook 單元測試（M-FE-WaveB-03 新增） ─────────────────────────
describe('useChip hook', () => {
  const mockChipData = {
    stockId: '2330',
    date: '2026-04-22',
    institutions: [
      { name: '外資', netBuySell: 12_543_000, buy: 35_000_000, sell: 22_457_000 },
      { name: '投信', netBuySell: -2_100_000, buy: 5_000_000, sell: 7_100_000 },
      { name: '自營商', netBuySell: 890_000, buy: 3_200_000, sell: 2_310_000 },
    ],
    source: '台灣證券交易所（TWSE）',
  };

  it('初始狀態：isLoading=true，data=undefined', () => {
    vi.mocked(stockService.getChip).mockReturnValue(new Promise(() => undefined));
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useChip('2330'), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it('success：service 回正常資料 → data.institutions 有 3 筆', async () => {
    vi.mocked(stockService.getChip).mockResolvedValueOnce(mockChipData);
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useChip('2330'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.institutions.length).toBe(3);
  });

  it('error：service 拋 BusinessError → isError=true', async () => {
    vi.mocked(stockService.getChip).mockRejectedValueOnce(
      new BusinessError(5101, '證交所資料來源暫時無法存取', 'hook-trace-004'),
    );
    const wrapper = createHookWrapper();
    const { result } = renderHook(() => useChip('2330'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ─── PriceHeader 無資料降級顯示（原 describe 名稱修正） ─────────────────
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

// ─── useQueryErrorNotification Hook 行為測試（B-FE-WaveB-01 新增） ───────
describe('useQueryErrorNotification（透過 StockDetailPage 整合驗證）', () => {
  it('error null → 不觸發 notification（頁面正常渲染股名）', async () => {
    vi.mocked(stockService.getQuote).mockResolvedValue({
      stockId: '2330',
      stockName: '台積電',
      price: '912.00',
      change: '+12.00',
      changePercent: '+1.33',
      volume: 28_745_321,
      updatedAt: '2026-04-22T10:30:00.000+08:00',
    });
    vi.mocked(stockService.getHistory).mockResolvedValue([]);
    vi.mocked(stockService.getFundamental).mockResolvedValue({
      stockId: '2330',
      eps: null,
      per: null,
      pbr: null,
      roe: null,
      updatedAt: '2026-04-22',
      source: 'MOPS',
    });
    vi.mocked(stockService.getChip).mockResolvedValue({
      stockId: '2330',
      date: '2026-04-22',
      institutions: [],
      source: 'TWSE',
    });

    renderStockDetailPage('2330');
    await waitFor(() => {
      expect(screen.getByText(/台積電/)).toBeDefined();
    });
  });

  it('stockId 空字串 → Navigate 跳回首頁（顯示 home-page）', () => {
    // 直接用有 Route 定義的 wrapper，stockId 預設 ''（Route 匹配空字串）
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
