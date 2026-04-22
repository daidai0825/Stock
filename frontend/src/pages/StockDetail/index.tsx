import { type FC, useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { Col, notification, Result, Row, Space, Tag } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { KLinePeriod } from '@/types/stock';
import { BusinessError } from '@/types/api';
import { ErrorCode } from '@/constants/errorCodes';
import { RoutePath } from '@/constants/routes';
import { useStockQuote } from './hooks/useStockQuote';
import { useStockHistory } from './hooks/useStockHistory';
import { useFundamental } from './hooks/useFundamental';
import { useChip } from './hooks/useChip';
import { PriceHeader } from './components/PriceHeader';
import { KLineChart } from './components/KLineChart';
import { PeriodSelector } from './components/PeriodSelector';
import { FundamentalCard } from './components/FundamentalCard';
import { ChipCard } from './components/ChipCard';
import { logger } from '@/utils/logger';

/**
 * 個股詳情頁（/stocks/:stockId）
 *
 * 組合所有區塊：
 * - PriceHeader：即時行情標題列（含 isStale 警示 Tag）
 * - PeriodSelector + KLineChart：K 線圖
 * - FundamentalCard：基本面
 * - ChipCard：三大法人籌碼
 *
 * 錯誤處理：React Query error → useEffect → notification.error（含 traceId）。
 * useEffect 包裹確保只在 error 物件參考改變時觸發一次，
 * 避免每次 re-render 重複 fire（B-FE-WaveB-01 修正）。
 *
 * Round 2 新增：
 * - isStale=true 時在行情區域顯示「資料延遲」警示 Tag（schema-lock §1.3）
 * - 4001 STOCK_NOT_FOUND 使用 ErrorCode.STOCK_NOT_FOUND（取代舊版 RESOURCE_NOT_FOUND）
 */
const useQueryErrorNotification = (error: Error | null, context: string): void => {
  const { t } = useTranslation();
  useEffect(() => {
    if (error === null) return;
    const traceId = error instanceof BusinessError ? error.traceId : undefined;
    const message = `${context}：${error.message}`;
    const description = traceId !== undefined ? `traceId：${traceId}` : t('common.errorRetry');
    logger.warn(`[StockDetail] ${message}`, { traceId });
    notification.error({ message, description, duration: 5 });
  }, [error, context, t]);
};

/** 是否為「股票不存在」錯誤（STOCK_NOT_FOUND / 4001） */
const isStockNotFound = (error: Error | null): boolean =>
  error instanceof BusinessError && error.code === ErrorCode.STOCK_NOT_FOUND;

export const StockDetailPage: FC = () => {
  const { stockId = '' } = useParams<{ stockId: string }>();
  const { t } = useTranslation();
  const [period, setPeriod] = useState<KLinePeriod>('daily');

  // 所有 hook 必須無條件呼叫（Hook Rules），再依 stockId 的 enabled 控制 query 是否執行
  const quoteQuery = useStockQuote(stockId);
  const historyQuery = useStockHistory(stockId, period);
  const fundamentalQuery = useFundamental(stockId);
  const chipQuery = useChip(stockId);

  // 錯誤通知（各 query 獨立，useEffect 確保只在 error 改變時觸發一次）
  useQueryErrorNotification(quoteQuery.error, t('stock.loadingQuote'));
  useQueryErrorNotification(historyQuery.error, t('stock.loadingChart'));
  useQueryErrorNotification(fundamentalQuery.error, t('stock.fundamental.title'));
  useQueryErrorNotification(chipQuery.error, t('stock.chip.title'));

  // stockId 為空（非法路由）→ 跳回首頁（M-FE-WaveB-04）
  if (stockId.length === 0) {
    return <Navigate to={RoutePath.HOME} replace />;
  }

  // 任一 query 回傳 4001（股票不存在）→ 顯示 Result 空狀態（M-FE-WaveB-04）
  const stockNotFound =
    isStockNotFound(quoteQuery.error) ||
    isStockNotFound(fundamentalQuery.error) ||
    isStockNotFound(chipQuery.error);

  if (stockNotFound) {
    return (
      <div data-testid="stock-not-found-result">
        <Result
          status="404"
          title={t('stock.notFound')}
          extra={
            <Link to={RoutePath.HOME}>{t('common.cancel')}</Link>
          }
        />
      </div>
    );
  }

  // isStale=true 時顯示「資料延遲」警示 Tag（schema-lock §1.3 設計說明）
  const isStale = quoteQuery.data?.isStale === true;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {/* isStale 警示提示（schema-lock §1.3：isStale=true 時顯示） */}
      {isStale && (
        <Tag
          icon={<WarningOutlined />}
          color="warning"
          data-testid="stale-data-tag"
        >
          {t('stock.staleData', {
            date: quoteQuery.data?.quoteDate ?? '',
            defaultValue: '資料延遲，最後更新：{{date}}',
          })}
        </Tag>
      )}

      {/* 行情標題 */}
      <PriceHeader
        quote={quoteQuery.data}
        isLoading={quoteQuery.isLoading}
      />

      {/* K 線週期切換 */}
      <PeriodSelector value={period} onChange={setPeriod} />

      {/* K 線圖：消費 historyQuery.data.items 陣列 */}
      <KLineChart
        history={historyQuery.data?.items}
        isLoading={historyQuery.isLoading}
        isError={historyQuery.isError}
      />

      {/* 基本面 + 籌碼（並排） */}
      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <FundamentalCard
            fundamental={fundamentalQuery.data}
            isLoading={fundamentalQuery.isLoading}
          />
        </Col>
        <Col xs={24} md={12}>
          <ChipCard chip={chipQuery.data} isLoading={chipQuery.isLoading} />
        </Col>
      </Row>
    </Space>
  );
};

export default StockDetailPage;
