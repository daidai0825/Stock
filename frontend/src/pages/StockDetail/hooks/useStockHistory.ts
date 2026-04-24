import { useQuery } from '@tanstack/react-query';
import { stockService } from '@/services/stockService';
import type { KLineHistory, KLinePeriod } from '@/types/stock';

/**
 * K 線歷史資料 hook（Wave B Round 2 對齊 Schema Lock）
 *
 * 回傳 KLineHistory 包裝物件（含 stockId / period / items[]）。
 * 後端依 period 自動聚合（daily/weekly/monthly），前端不自行聚合。
 *
 * staleTime 5 分鐘（歷史資料非即時，不需頻繁 refetch）。
 */
export const useStockHistory = (stockId: string, period: KLinePeriod) =>
  useQuery<KLineHistory, Error>({
    queryKey: ['stockHistory', stockId, period],
    queryFn: () => stockService.getHistory(stockId, period),
    enabled: stockId.length > 0,
    staleTime: 5 * 60_000,
  });
