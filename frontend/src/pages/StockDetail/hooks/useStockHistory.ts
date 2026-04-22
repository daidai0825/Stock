import { useQuery } from '@tanstack/react-query';
import { stockService } from '@/services/stockService';
import type { KLinePeriod, StockHistoryItem } from '@/types/stock';

/**
 * K 線歷史資料 hook
 *
 * staleTime 5 分鐘（歷史資料非即時，不需頻繁 refetch）。
 */
export const useStockHistory = (stockId: string, period: KLinePeriod) =>
  useQuery<StockHistoryItem[], Error>({
    queryKey: ['stockHistory', stockId, period],
    queryFn: () => stockService.getHistory(stockId, period),
    enabled: stockId.length > 0,
    staleTime: 5 * 60_000,
  });
