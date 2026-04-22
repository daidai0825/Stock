import {
  type FC,
  useEffect,
  useRef,
  useCallback,
} from 'react';
import {
  createChart,
  type IChartApi,
  type ISeriesApi,
  type CandlestickData,
  type HistogramData,
  type Time,
  ColorType,
  CrosshairMode,
} from 'lightweight-charts';
import { Skeleton, Typography, Alert } from 'antd';
import { useTranslation } from 'react-i18next';
import type { StockHistoryItem } from '@/types/stock';

const { Text } = Typography;

// m-FE-WaveB-01：用模組常數取代 useState（height 不需要 re-render 觸發）
const CHART_HEIGHT = 400;

interface KLineChartProps {
  history: StockHistoryItem[] | undefined;
  isLoading: boolean;
  isError: boolean;
}

/** 將後端 StockHistoryItem 轉為 lightweight-charts CandlestickData */
const toCandlestickData = (items: StockHistoryItem[]): CandlestickData<Time>[] =>
  items.map((item) => ({
    time: item.date as Time,
    open: parseFloat(item.open),
    high: parseFloat(item.high),
    low: parseFloat(item.low),
    close: parseFloat(item.close),
  }));

/** 將後端 StockHistoryItem 轉為 lightweight-charts HistogramData（成交量） */
const toVolumeData = (
  items: StockHistoryItem[],
  candleData: CandlestickData<Time>[],
): HistogramData<Time>[] =>
  items.map((item, idx) => {
    const candle = candleData[idx];
    const isUp = candle !== undefined ? candle.close >= candle.open : true;
    return {
      time: item.date as Time,
      value: item.volume,
      color: isUp ? 'rgba(207, 19, 34, 0.5)' : 'rgba(63, 134, 0, 0.5)',
    };
  });

/**
 * KLineChart — 使用 TradingView lightweight-charts 4.2
 *
 * 雙 panel 設計：上方 Candlestick + 下方 Volume Histogram。
 * 使用 lightweight-charts 內建 crosshair 顯示 OHLC tooltip。
 *
 * 記憶體釋放：unmount 時呼叫 chart.remove()（重要）。
 * 響應式：使用 ResizeObserver 監聽容器寬度變化並 applyOptions 更新。
 */
export const KLineChart: FC<KLineChartProps> = ({ history, isLoading, isError }) => {
  const { t } = useTranslation();
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candleSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const volumeSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null);

  /** 初始化 chart（只跑一次） */
  const initChart = useCallback(() => {
    const container = containerRef.current;
    if (container === null) return;

    const chart = createChart(container, {
      layout: {
        background: { type: ColorType.Solid, color: '#ffffff' },
        textColor: '#333333',
        fontSize: 12,
      },
      grid: {
        vertLines: { color: '#f0f0f0' },
        horzLines: { color: '#f0f0f0' },
      },
      crosshair: {
        mode: CrosshairMode.Normal,
      },
      rightPriceScale: {
        borderColor: '#d9d9d9',
        scaleMargins: { top: 0.1, bottom: 0.3 },
      },
      timeScale: {
        borderColor: '#d9d9d9',
        rightOffset: 5,
        barSpacing: 8,
        fixLeftEdge: false,
        fixRightEdge: false,
      },
      width: container.clientWidth,
      height: CHART_HEIGHT,
    });

    // Candlestick series（上方 panel）
    const candleSeries = chart.addCandlestickSeries({
      upColor: '#cf1322',
      downColor: '#3f8600',
      borderUpColor: '#cf1322',
      borderDownColor: '#3f8600',
      wickUpColor: '#cf1322',
      wickDownColor: '#3f8600',
      priceScaleId: 'right',
    });

    // Volume histogram series（下方 panel，獨立 price scale）
    const volumeSeries = chart.addHistogramSeries({
      priceFormat: { type: 'volume' },
      priceScaleId: 'volume',
    });

    chart.priceScale('volume').applyOptions({
      scaleMargins: { top: 0.8, bottom: 0 },
    });

    chartRef.current = chart;
    candleSeriesRef.current = candleSeries;
    volumeSeriesRef.current = volumeSeries;
  }, []);

  /** 初始化 chart */
  useEffect(() => {
    initChart();

    return () => {
      if (chartRef.current !== null) {
        chartRef.current.remove();
        chartRef.current = null;
        candleSeriesRef.current = null;
        volumeSeriesRef.current = null;
      }
    };
    // initChart 是穩定 callback，只執行一次
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** 響應式：容器寬度變化時更新 chart width */
  useEffect(() => {
    const container = containerRef.current;
    if (container === null) return;

    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const width = entry.contentRect.width;
        if (chartRef.current !== null && width > 0) {
          chartRef.current.applyOptions({ width });
        }
      }
    });

    observer.observe(container);
    return () => observer.disconnect();
  }, []);

  /** 資料更新時重繪 */
  useEffect(() => {
    if (history === undefined || history.length === 0) return;
    if (candleSeriesRef.current === null || volumeSeriesRef.current === null) return;

    const candleData = toCandlestickData(history);
    const volumeData = toVolumeData(history, candleData);

    candleSeriesRef.current.setData(candleData);
    volumeSeriesRef.current.setData(volumeData);

    // 自動縮放到最新資料
    chartRef.current?.timeScale().fitContent();
  }, [history]);

  if (isError) {
    return (
      <Alert
        type="warning"
        message={t('common.errorRetry')}
        showIcon
        style={{ marginBottom: 8 }}
      />
    );
  }

  return (
    <div data-testid="kline-chart-container">
      {isLoading && <Skeleton active style={{ height: CHART_HEIGHT }} />}

      {/* chart 容器：loading 時隱藏但不卸載，避免重複 initChart */}
      <div
        ref={containerRef}
        style={{
          width: '100%',
          height: CHART_HEIGHT,
          display: isLoading ? 'none' : 'block',
        }}
        role="img"
        aria-label={t('stock.klineTitle')}
      />

      <Text
        type="secondary"
        style={{
          display: 'block',
          marginTop: 8,
          fontSize: 12,
          textAlign: 'center',
        }}
        data-testid="kline-disclaimer"
      >
        {t('stock.klineDisclaimer')}
      </Text>
    </div>
  );
};
